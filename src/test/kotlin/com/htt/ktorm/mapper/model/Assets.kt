package com.htt.ktorm.mapper.model

import org.ktorm.entity.*
import org.ktorm.schema.*

object Assets: Table<Asset>("Asset") {
    val id = int("id").primaryKey().bindTo { it.id }
    val name = varchar("name").bindTo { it.name }
    val userId = int("userId").bindTo { it.userId }
}
interface Asset: Entity<Asset> {
    companion object: Entity.Factory<Asset>()

    var id:Int
    var name:String
    var userId:Int
}
