package org.example.database

import org.jetbrains.exposed.sql.Table

object UsuarioTable : Table("usuaris") {
    val username = varchar("username", 100)
    val nom = varchar("nom", 100)
    val email = varchar("email", 100).uniqueIndex()
    val idioma = varchar("idioma", 20)
    val sesionIniciada = bool("sesion_iniciada").default(false)
    val isAdmin = bool("is_admin").default(false)

    override val primaryKey = PrimaryKey(username, name = "PK_username")
}