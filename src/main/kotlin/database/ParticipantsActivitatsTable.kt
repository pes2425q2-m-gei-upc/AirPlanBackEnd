package org.example.database

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object ParticipantsActivitatsTable : Table("participantsactivitats") {
    val id_activitat = integer("id_activitat").references(ActivitatTable.id_activitat, onDelete = ReferenceOption.CASCADE)
    val username_participant = varchar("username_participant", 100).references(UsuarioTable.username, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(id_activitat, username_participant, name = "PK_participantsactivitats")
}