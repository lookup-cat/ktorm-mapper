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
    open var ids: List<Column<*>> = emptyList()
    val oneToManyList = mutableListOf<OneToMany<*>>()
    val oneToOneList = mutableListOf<OneToOne<*>>()

    abstract class ChildMapper<E : Entity<E>>(
        table: Table<E>,
        private val parentMapper: RowSetMapper<*>
    ) : RowSetMapper<E>(table) {

        //包含所有父级Mapper的ids + 当前Mapper的ids
        var allIds: List<Column<*>> = computeAllIds()

        /**
         * 设置当前Mapper的唯一标识列集合
         */
        override fun ids(vararg ids: Column<*>): RowSetMapper<E> {
            super.ids(*ids)
            allIds = computeAllIds()
            return this
        }

        private fun computeAllIds(): List<Column<*>> {
            val ids = mutableListOf<Column<*>>()
            val parent = parentMapper
            ids += this.ids
            ids += if (parent is ChildMapper<*>) {
                parent.allIds
            } else {
                parent.ids
            }
            return ids
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
     * 一对一映射
     */
    class OneToOne<T : Entity<T>>(
        targetTable: Table<T>,
        val property: KMutableProperty<*>,
        parentMapper: RowSetMapper<*>
    ) : ChildMapper<T>(targetTable, parentMapper)

    /**
     * 添加一对多映射
     */
    fun <T : Entity<T>> hasOne(targetTable: Table<T>, property: KMutableProperty<*>): OneToOne<T> {
        val oneToOne = OneToOne(targetTable, property, this)
        this.oneToOneList.add(oneToOne)
        return oneToOne
    }

    /**
     * 添加一对多映射
     */
    inline fun <T : Entity<T>> hasMany(
        targetTable: Table<T>,
        property: KMutableProperty<*>,
        block: OneToMany<T>.() -> Unit = {}
    ): OneToMany<T> {
        val oneToMany = OneToMany(targetTable, property, this)
        this.oneToManyList.add(oneToMany)
        block(oneToMany)
        return oneToMany
    }

    /**
     * 设置唯一标识列集合
     */
    open fun ids(vararg ids: Column<*>): RowSetMapper<E> {
        this.ids = ids.toList()
        return this
    }
}

/**
 * 创建实体映射 RowSetMapper
 */
inline fun <E : Entity<E>> Table<E>.rowSetMapper(block: RowSetMapper<E>.() -> Unit): RowSetMapper<E> {
    return RowSetMapper(this).apply(block)
}

// { mapper: { rowKey :partialObject(entity/collections) } }
typealias RowKeyCache = MutableMap<RowSetMapper<*>, MutableMap<Any, Any>>

@Suppress("FunctionName")
fun RowKeyMapper() = mutableMapOf<RowSetMapper<*>, MutableMap<Any, Any>>()

/**
 * 从查询结果中映射到List集合
 */
fun <E : Entity<E>> Query.mapMany(mapper: RowSetMapper<E>): List<E> {
    val rowKeyMapper = RowKeyMapper()
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
    val rowKeyMapper = RowKeyMapper()
    var entity:E? = null
    for (queryRowSet in this) {
        if (entity == null) {
            entity = queryRowSet.mapToEntity(mapper.table)
        }
        queryRowSet.mapChild(mapper,rowKeyMapper,entity)
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
 * User的唯一表示列为 'User_id'
 * Role的唯一表示列为 'Role_id'
 *
 * 那么根据User的id可以得出 表中第二和第三行 是同一个User对象
 * 这两行通过User的Mapper获取到的RowKey都是 (int)2
 *
 * 而这两行数据通过Role的Mapper获取到的RowKey分别是 [2,2] [2,3]
 * 列表第一个元素为User的id 第二个元素为Role的id
 * 因此可以区分出这是两个Role对象
 *
 */
fun QueryRowSet.getRowKey(mapper: RowSetMapper<*>): Any {
    if (mapper is RowSetMapper.ChildMapper) {
        //需要获取所有父级的id
        val childIds = mapper.ids
        return if (childIds.isEmpty()) {
            mapper.allIds
                .map { this[it] }
                .toMutableList()
                .apply { add(this@getRowKey.row) }  // 如果id列表为空 那么取行号为id
        } else {
            mapper.allIds
                .map { this[it] }
        }
    } else {
        val ids = mapper.ids
        if (ids.isEmpty()) return this.row // 如果id列表为空 那么取行号为id
        if (ids.size == 1) return this[ids.first()]!!
        return ids.map { this[it] }
    }
}

fun <E : Entity<E>> QueryRowSet.mapMany(
    mapper: RowSetMapper<E>,
    rowKeyCache: RowKeyCache,
    result: MutableList<Any>
) {
    val rowKey = this.getRowKey(mapper)
    val rowKeyMap = rowKeyCache.computeIfAbsent(mapper) { mutableMapOf() }
    val entity = rowKeyMap.computeIfAbsent(rowKey) {
        val entity = this.mapToEntity(mapper.table)
        result.add(entity)
        entity
    } as E
    this.mapChild(mapper, rowKeyCache, entity)
}

fun <P : Entity<P>, E : Entity<E>> QueryRowSet.mapOne(
    mapper: RowSetMapper.OneToOne<E>,
    parent: Entity<P>,
    rowKeyCache: RowKeyCache
) {
    val rowKey = this.getRowKey(mapper)
    val rowKeyMap = rowKeyCache.computeIfAbsent(mapper) { mutableMapOf() }
    val entity = rowKeyMap.computeIfAbsent(rowKey) {
        val entity = this.mapToEntity(mapper.table)
        mapper.property.call(parent, entity)
        entity
    } as E
    this.mapChild(mapper, rowKeyCache, entity)
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
