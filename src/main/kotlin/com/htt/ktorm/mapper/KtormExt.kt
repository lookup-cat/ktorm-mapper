package com.htt.ktorm.mapper

import org.ktorm.dsl.*
import org.ktorm.entity.*
import org.ktorm.schema.*
import kotlin.reflect.*
import kotlin.reflect.full.*

fun <T : Entity<T>> QueryRowSet.mapToEntity(table: Table<T>): T {
    val rowSet = this
    return (table.entityClass!!.companionObjectInstance as Entity.Factory<T>) {
        table.columns.forEach {
            val value = rowSet[it] ?: return@forEach
            val binding = it.binding
            if (binding is NestedBinding) {
                for (property in binding.properties) {
                    if (property is KMutableProperty<*>) {
                        property.setter.call(this, value)
                    }
                }
            }
        }
    }
}



inline fun <reified T : Entity<T>> Query.mapToEntity(table: Table<T>): List<T> {
    return this.map { it.mapToEntity(table) }
}
