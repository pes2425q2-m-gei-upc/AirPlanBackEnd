package org.example.database

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object ClienteTable : Table("clients") {
    val username = varchar("username", 100).references(UsuarioTable.username, onDelete = ReferenceOption.CASCADE)
    val nivell = integer("nivell").default(0) // Added the "nivell" column as an int4 not null with a default value
    
    override val primaryKey = PrimaryKey(username, name = "PK_clients_username")
}