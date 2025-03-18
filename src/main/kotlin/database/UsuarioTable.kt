package org.example.database

import org.jetbrains.exposed.sql.Table

object UsuarioTable : Table("usuaritemp") {
    val username = varchar("username", 100)
    val nom = varchar("nom", 100)
    val email = varchar("email", 100).uniqueIndex()
    val contrasenya = varchar("contrasenya", 255)
    val idioma = varchar("idioma", 20)
    val sesionIniciada = bool("sesioniniciada").default(false)
    val isAdmin = bool("isadmin").default(false)

    override val primaryKey = PrimaryKey(username, name = "PK_username")
}