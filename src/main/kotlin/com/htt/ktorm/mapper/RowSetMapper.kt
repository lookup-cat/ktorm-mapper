@file:Suppress("UNCHECKED_CAST")

package com.htt.ktorm.mapper

import org.ktorm.dsl.Query
import org.ktorm.dsl.QueryRowSet
import org.ktorm.entity.Entity
import org.ktorm.schema.Column
import org.ktorm.schema.Table
import javax.sql.*
import kotlin.reflect.KMutableProperty

open class RowSetMapper<E : Entity<E>>(
    val table: Table<E>
) {

    // 实体的唯一标识列集合
    open var entityIds: Array<Column<*>> = emptyArray()
    val oneToManyList = mutableListOf<OneToMany<*>>()
    val oneToOneList = mutableListOf<OneToOne<*>>()

    abstract class ChildMapper<E : Entity<E>>(
        table: Table<E>,
        private val parentMapper: RowSetMapper<*>
    ) : RowSetMapper<E>(table) {

        //包含所有父级Mapper的ids + 当前Mapper的ids
        var allEntityIds: Array<Column<*>> = computeAllIds()

        /**
         * 设置当前Mapper的唯一标识列集合
         */
        override fun entityIds(vararg entityIds: Column<*>): RowSetMapper<E> {
            super.entityIds(*entityIds)
            allEntityIds = computeAllIds()
            return this
        }

        private fun computeAllIds(): Array<Column<*>> {
            return if (parentMapper is ChildMapper<*>) {
                parentMapper.allEntityIds + this.entityIds
            } else {
                parentMapper.entityIds + this.entityIds
            }
        }
    }

    /**
     * 一对多映射
     */
    class OneToMany<T : Entity<T>>(
        targetTable: Table<T>,
        val property: KMutableProperty<*>,
        parentMapper: RowSetMapper<*>
    ) : ChildMapper<T>(targetTable, parentMapper)

    /**
     * 一对一映射`
     */
    class OneToOne<T : Entity<T>>(
        targetTable: Table<T>,
        val property: KMutableProperty<*>,
        parentMapper: RowSetMapper<*>,
    ) : ChildMapper<T>(targetTable, parentMapper)

    /**
     * 添加一对多映射
     */
    fun <T : Entity<T>> hasOne(
        targetTable: Table<T>,
        property: KMutableProperty<*>,
        vararg entityIds: Column<*>,
        block: OneToOne<T>.() -> Unit = {}
    ): OneToOne<T> {
        val oneToOne = OneToOne(targetTable, property, this)
        this.oneToOneList.add(oneToOne)
        oneToOne.entityIds(*entityIds)
        block(oneToOne)
        return oneToOne
    }

    /**
     * 添加一对多映射
     */
    inline fun <T : Entity<T>> hasMany(
        targetTable: Table<T>,
        property: KMutableProperty<*>,
        vararg entityIds: Column<*>,
        block: OneToMany<T>.() -> Unit = {}
    ): OneToMany<T> {
        val oneToMany = OneToMany(targetTable, property, this)
        this.oneToManyList.add(oneToMany)
        oneToMany.entityIds(*entityIds)
        block(oneToMany)
        return oneToMany
    }

    /**
     * 设置唯一标识列集合
     */
    open fun entityIds(vararg entityIds: Column<*>): RowSetMapper<E> {
        this.entityIds = entityIds as Array<Column<*>>
        return this
    }
}

/**
 * 创建实体映射 RowSetMapper
 */
inline fun <E : Entity<E>> Table<E>.rowSetMapper(
    vararg ids: Column<*>,
    block: RowSetMapper<E>.() -> Unit
): RowSetMapper<E> {
    return RowSetMapper(this).apply { entityIds(*ids) }.apply(block)
}

// { rowKey: (entity/collections) }
// rowKey: mapperId + entityColumnValues
typealias RowKeyCache = MutableMap<Any, Any>

@Suppress("FunctionName")
fun RowKeyCache() = mutableMapOf<Any,Any>()

/**
 * 从查询结果中映射到List集合
 */
fun <E : Entity<E>> Query.mapMany(mapper: RowSetMapper<E>): List<E> {
    val rowKeyMapper = RowKeyCache()
    val result = mutableListOf<Any>()
    for (queryRowSet in this) {
        queryRowSet.mapMany(mapper, rowKeyMapper, result)
    }
    return result as MutableList<E>
}

/**
 * 从查询结果中映射到单个实体 结果可能为null
 */
fun <E : Entity<E>> Query.mapOne(mapper: RowSetMapper<E>): E? {
    val rowKeyMapper = RowKeyCache()
    var entity: E? = null
    for (queryRowSet in this) {
        if (entity == null) {
            entity = queryRowSet.mapToEntity(mapper.table)
        }
        if (entity != null) {
            queryRowSet.mapChild(mapper, rowKeyMapper, entity)
        }
    }
    return entity
}

/**
 * 根据Mapper的唯一标识列集合 获取该Mapper在当前行中的Key
 * 这个Key的作用在于 可以区分多行数据中 是同一个对象还是不同对象
 * 例如下表
 *      +-------+-------------+-------+---------+
 *      |User_id|User_username|Role_id|Role_name|
 *      +-------+-------------+-------+---------+
 *      |1      |张三           |1      |管理员      |
 *      |2      |李四           |2      |用户       |
 *      |2      |李四           |3      |运维       |
 *      |3      |王五           |3      |运维       |
 *      +-------+-------------+-------+---------+
 * 此表中一个User有多个Role
 * User的entityIds为 'User_id'
 * Role的entityIds为 'Role_id'
 *
 * 那么根据User的entityIds可以得出 表中第二和第三行 是同一个User对象
 * 这两行通过User的Mapper获取到的RowKey都是 [mapper,(int)2]
 *
 * 而这两行数据通过Role的Mapper获取到的RowKey分别是 [mapper,2,2] [mapper,2,3]
 * 列表第一个元素为User的id 第二个元素为Role的id
 * 因此可以区分出这是两个Role对象
 *
 */
fun QueryRowSet.getRowKey(mapper: RowSetMapper<*>): Any {
    if (mapper is RowSetMapper.ChildMapper) {
        //需要获取所有父级的id
        val childIds = mapper.entityIds
        return if (childIds.isEmpty()) {
            mutableListOf<Any?>(mapper)
                .apply {
                    addAll(mapper.allEntityIds.map { this@getRowKey[it] })
                    add(this@getRowKey.row) // 如果id列表为空 那么取行号为id
                }
        } else {
            listOf(mapper) + mapper.allEntityIds.map { this@getRowKey[it] }
        }
    } else {
        val entityIds = mapper.entityIds
        if (entityIds.isEmpty()) return listOf(mapper, this.row)  // 如果id列表为空 那么取行号为id
        if (entityIds.size == 1) return listOf(mapper, this[entityIds.first()])
        return listOf(mapper) + mapper.entityIds.map { this@getRowKey[it] }
    }
}

fun <E : Entity<E>> QueryRowSet.mapMany(
    mapper: RowSetMapper<E>,
    rowKeyCache: RowKeyCache,
    result: MutableList<Any>
) {
    val rowKey = this.getRowKey(mapper)
    var entity: E? = null
    if (!rowKeyCache.containsKey(rowKey)) {
        entity = this.mapToEntity(mapper.table)
        if (entity != null) {
            result.add(entity)
            rowKeyCache[rowKey] = entity
        }
    }
    if (entity != null) {
        this.mapChild(mapper, rowKeyCache, entity)
    }
}

fun <P : Entity<P>, E : Entity<E>> QueryRowSet.mapOne(
    mapper: RowSetMapper.OneToOne<E>,
    parent: Entity<P>,
    rowKeyCache: RowKeyCache
) {
    val rowKey = this.getRowKey(mapper)
    var entity: E? = null
    if (!rowKeyCache.containsKey(rowKey)) {
        entity = this.mapToEntity(mapper.table)
        if (entity != null) {
            mapper.property.setter.call(parent, entity)
            rowKeyCache[rowKey] = entity
        }
    }
    if (entity != null) {
        this.mapChild(mapper, rowKeyCache, entity)
    }
}

private fun <E : Entity<E>> QueryRowSet.mapChild(
    mapper: RowSetMapper<E>,
    rowKeyCache: RowKeyCache,
    entity: E
) {
    for (oneToMany: RowSetMapper.OneToMany<*> in mapper.oneToManyList) {
        var collections = oneToMany.property.getter.call(entity) as? MutableList<Any>
        if (collections == null) {
            collections = mutableListOf()
            oneToMany.property.setter.call(entity, collections)
        }
        this.mapMany(oneToMany, rowKeyCache, collections)
    }
    for (oneToOne in mapper.oneToOneList) {
        this.mapOne(oneToOne, entity, rowKeyCache)
    }
}
