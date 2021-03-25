package com.htt.ktorm.mapper.model

import org.ktorm.schema.*

object RolePermissions: Table<Nothing>("RolePermission") {
    val roleId = int("roleId").primaryKey()
    val permissionId = int("permissionId").primaryKey()
}
