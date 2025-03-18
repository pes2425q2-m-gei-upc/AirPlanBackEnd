package org.example.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils

object DatabaseFactory {
    fun init() {
        val url = "jdbc:postgresql://nattech.fib.upc.edu:40351/midb"
        val user = "airplan"
        val password = "airplan1234"

        Database.connect(url, driver = "org.postgresql.Driver", user = user, password = password)

        transaction {
            SchemaUtils.create(UsuarioTable) // Crea la tabla si no existe
        }
    }
}