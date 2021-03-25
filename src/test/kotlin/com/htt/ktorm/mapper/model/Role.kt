package com.htt.ktorm.mapper.model

import org.ktorm.entity.*
import org.ktorm.schema.*

object Roles : Table<Role>("Role") {
    val id = int("id").primaryKey().bindTo { it.id }
    val name = varchar("name").bindTo { it.name }
}

interface Role : Entity<Role> {
    companion object : Entity.Factory<Role>()

    var id: Int
    var name: String

    var permissions: MutableList<Permission>
}
