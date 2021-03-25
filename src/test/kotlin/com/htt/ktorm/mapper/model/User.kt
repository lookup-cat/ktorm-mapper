package com.htt.ktorm.mapper.model

import org.ktorm.entity.*
import org.ktorm.schema.*
import java.security.*

object Users : Table<User>("User") {
    val id = int("id").primaryKey().bindTo { it.id }
    var username = varchar("username").bindTo { it.username }
}

interface User : Entity<User> {
    companion object : Entity.Factory<User>()

    var id: Int
    var username: String

    var roles: MutableList<Role>
    var permission: MutableList<Permission>
    var asset: Asset?
}
