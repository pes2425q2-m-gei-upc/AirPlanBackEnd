package org.example.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp


object NotificacionsTable : Table("notificacions") {
    val id_notificacion = integer ("id_notificacion").autoIncrement()
    val username = varchar("username", 100)
    val type = varchar("type", 100)
    val message = text("message")
    val isRead = bool("is_read")
    val created_at = datetime("created_at").defaultExpression(CurrentTimestamp())

    override val primaryKey = PrimaryKey(id_notificacion)
}


