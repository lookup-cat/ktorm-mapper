package com.htt.ktorm.mapper.model

import org.ktorm.schema.*

object UserRoles : Table<Nothing>("UserRole") {
    val userId = int("userId").primaryKey()
    val roleId = int("roleId").primaryKey()
}
