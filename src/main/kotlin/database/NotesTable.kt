package org.example.database

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.time

/**
 * Defines the database table structure for storing user notes
 */
object NotesTable : Table("notas") {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 100).references(ClienteTable.username, onDelete = ReferenceOption.CASCADE)
    val fechaCreacion = date("fecha_creacion")
    val horaRecordatorio = time("hora_recordatorio")
    val comentario = text("comentario")

    override val primaryKey = PrimaryKey(id)
} //falta hacer la tabla en la base de datos