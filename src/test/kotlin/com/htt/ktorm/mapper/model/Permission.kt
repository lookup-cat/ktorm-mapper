package com.htt.ktorm.mapper.model

import org.ktorm.entity.*
import org.ktorm.schema.*

object Permissions: Table<Permission>("Permission") {
    val id = int("id").primaryKey().bindTo { it.id }
    val name = varchar("name").bindTo { it.name }
}

interface Permission:Entity<Permission> {
    companion object: Entity.Factory<Permission>()

    var id:Int
    var name:String
}
