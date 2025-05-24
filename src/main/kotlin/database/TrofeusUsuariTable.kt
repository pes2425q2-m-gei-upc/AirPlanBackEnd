package org.example.database

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object TrofeusUsuariTable : Table("trofeus_usuari") {
    val usuari = varchar("usuari", 100).references(UsuarioTable.username, onDelete = ReferenceOption.CASCADE)
    val trofeuId = integer("trofeu_id").references(TrofeuTable.id, onDelete = ReferenceOption.CASCADE)
    val dataObtencio = timestamp("data_obtencio").defaultExpression(CurrentTimestamp())

    override val primaryKey = PrimaryKey(usuari, trofeuId, name = "PK_Trofeus_Usuari")
}