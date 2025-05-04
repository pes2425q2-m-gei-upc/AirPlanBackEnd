package org.example.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp

object ValoracioTable : Table("valoraciones") {
    val username = varchar("username", 100).references(UsuarioTable.username)
    val id_activitat = integer("id_activitat").references(ActivitatTable.id_activitat)
    val valoracion = integer("valoracion").check { it.between(1, 5) }
    val comentario = text("comentario").nullable()
    val fecha_valoracion = datetime("fecha_valoracion").defaultExpression(CurrentTimestamp())

    override val primaryKey = PrimaryKey(username, id_activitat, name = "PK_valoracio")
}