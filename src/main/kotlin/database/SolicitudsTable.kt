package org.example.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.ReferenceOption

object SolicitudsTable : Table("Solicituds") {
    val usernameAnfitrio = varchar("username_anfitrio", 100)
    val usernameSolicitant = varchar("username_solicitant", 100)
    val idActivitat = integer("id_activitat").references(ActivitatTable.id_activitat, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(usernameSolicitant, idActivitat, usernameAnfitrio, name = "PK_solicituds")
}