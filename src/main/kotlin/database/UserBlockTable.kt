package org.example.database

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object UserBlockTable : Table("user_blocks") {
    val blockerUsername = varchar("blocker_username", 100).references(
        UsuarioTable.username, 
        onDelete = ReferenceOption.CASCADE
    )
    val blockedUsername = varchar("blocked_username", 100).references(
        UsuarioTable.username, 
        onDelete = ReferenceOption.CASCADE
    )

    override val primaryKey = PrimaryKey(blockerUsername, blockedUsername, name = "PK_user_block")
}