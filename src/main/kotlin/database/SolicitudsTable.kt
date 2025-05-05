package org.example.database

import org.jetbrains.exposed.sql.Table

object SolicitudsTable : Table("Solicituds") {
    val usernameAnfitrio = varchar("username_anfitrio", 100)
    val usernameSolicitant = varchar("username_solicitant", 100)
    val idActivitat = integer("id_activitat").references(ActivitatTable.id_activitat)

    override val primaryKey = PrimaryKey(usernameSolicitant, idActivitat, usernameAnfitrio, name = "PK_solicituds")
}