package org.example.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object ActivitatFavoritaTable : Table("activitatsfavoritas") {
    val id_activitat = integer ("id_activitat")
    val username = varchar("username", 100)
    val dataAfegida = datetime("dataafegida")

    override val primaryKey = PrimaryKey(id_activitat, username, name = "PK_id_username")
}