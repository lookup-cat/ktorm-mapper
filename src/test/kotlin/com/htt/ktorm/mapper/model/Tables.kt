package com.htt.ktorm.mapper.model

import org.ktorm.database.*
import org.ktorm.entity.*

val Database.permissions get() = sequenceOf(Permissions)
val Database.roles get() = sequenceOf(Roles)
val Database.users get() = sequenceOf(Users)
