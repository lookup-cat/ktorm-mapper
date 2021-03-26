package com.htt.ktorm.mapper

import com.fasterxml.jackson.databind.*
import com.htt.ktorm.mapper.model.*
import org.junit.*
import org.ktorm.database.*
import org.ktorm.dsl.*
import org.ktorm.entity.*
import org.ktorm.jackson.*
import org.ktorm.logging.*
import org.ktorm.support.mysql.*
import org.mariadb.jdbc.*
import org.mariadb.jdbc.Driver
import java.sql.*

class MapperTest {

    lateinit var database: Database
    lateinit var dataSource: MariaDbPoolDataSource
    lateinit var objectMapper: ObjectMapper

    @Before
    fun init() {
        Class.forName("org.mariadb.jdbc.Driver")
        dataSource = MariaDbPoolDataSource("jdbc:mariadb://localhost:3306/KtormMapper?user=root&password=123456")
        database = Database.connect(dataSource, logger = ConsoleLogger(LogLevel.DEBUG))
        objectMapper = ObjectMapper().apply {
            enable(SerializationFeature.INDENT_OUTPUT)
            registerModule(KtormModule())
        }
    }

    @After
    fun destroy() {
        dataSource.close()
    }


    @Test
    fun findTest() {
        val list = database.from(Users)
            .innerJoin(UserRoles, Users.id eq UserRoles.userId)
            .innerJoin(Roles, UserRoles.roleId eq Roles.id)
            .innerJoin(RolePermissions, Roles.id eq RolePermissions.roleId)
            .innerJoin(Permissions, RolePermissions.permissionId eq Permissions.id)
            .leftJoin(Assets, Assets.userId eq Users.id)
            .select(Users.columns + Roles.columns + Permissions.columns + Assets.columns)
            .mapMany(Users.rowSetMapper(Users.id) {
                hasMany(Roles, User::roles, Roles.id) {
                    hasMany(Permissions, Role::permissions, Permissions.id)
                }
                hasOne(Assets, User::asset, Assets.id)
            })
        val json = objectMapper.writeValueAsString(list)
        println(json)
    }

}
