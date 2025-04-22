package org.example.database

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object InvitacioTable : Table("invitacions") {
    val id_activitat = integer("id_activitat").references(ActivitatTable.id_activitat, onDelete = ReferenceOption.CASCADE)
    val username_anfitrio = varchar("username_anfitrio", 100)
    val ussername_convidat = varchar("ussername_convidat", 100)

    override val primaryKey = PrimaryKey(id_activitat, username_anfitrio, ussername_convidat, name = "PK_invitacions")
}