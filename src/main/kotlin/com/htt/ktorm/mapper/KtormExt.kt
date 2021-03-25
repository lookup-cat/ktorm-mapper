package com.htt.ktorm.mapper

import org.ktorm.dsl.*
import org.ktorm.entity.*
import org.ktorm.schema.*
import kotlin.reflect.*
import kotlin.reflect.full.*

fun <T : Entity<T>> QueryRowSet.mapToEntity(table: Table<T>): T? {
    val rowSet = this
    var entity: T? = null
    table.columns.forEach {
        val value = rowSet[it] ?: return@forEach
        val binding = it.binding
        if (binding is NestedBinding) {
            for (property in binding.properties) {
                if (property is KMutableProperty<*>) {
                    if (entity == null) {
                        entity = table.createEntity()
                    }
                    property.setter.call(entity, value)
                }
            }
        }
    }
    return entity
}

fun <T : Entity<T>> Table<T>.createEntity(): T {
    return (entityClass!!.companionObjectInstance as Entity.Factory<T>)()
}


inline fun <reified T : Entity<T>> Query.mapToEntity(table: Table<T>): List<T> {
    return this.map { it.mapToEntity(table) ?: table.createEntity() }
}
