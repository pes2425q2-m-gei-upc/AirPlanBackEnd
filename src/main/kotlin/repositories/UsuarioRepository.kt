package org.example.repositories

import org.example.database.UsuarioTable
import org.example.models.Usuario
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class UsuarioRepository {
    fun agregarUsuario(usuario: Usuario): Boolean {
        return transaction {
            UsuarioTable.insert {
                it[username] = usuario.username
                it[nom] = usuario.nom
                it[email] = usuario.email
                it[contrasenya] = usuario.contrasenya
                it[idioma] = usuario.idioma.toString()
                it[sesionIniciada] = usuario.sesionIniciada
                it[isAdmin] = usuario.isAdmin
            }.insertedCount > 0
        }
    }
    fun eliminarUsuario(email: String): Boolean {
        return transaction {
            val filasEliminadas = UsuarioTable.deleteWhere { UsuarioTable.email eq email }
            filasEliminadas > 0  // Retorna `true` si eliminó algún usuario
        }
    }
}