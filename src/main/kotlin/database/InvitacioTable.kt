package org.example.database

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object InvitacioTable : Table("invitacions") {
    val id_activitat = integer("id_activitat").references(ActivitatTable.id_activitat, onDelete = ReferenceOption.CASCADE)
    val username_anfitrio = varchar("username_anfitrio", 100)
    val username_convidat = varchar("username_convidat", 100)

    override val primaryKey = PrimaryKey(id_activitat, username_anfitrio, username_convidat, name = "PK_invitacions")
}