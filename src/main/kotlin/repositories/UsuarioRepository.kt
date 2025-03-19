package org.example.repositories

import org.example.database.UsuarioTable
import org.example.enums.Idioma
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
            println("Eliminando usuario con email: $email")
            val filasEliminadas = UsuarioTable.deleteWhere { UsuarioTable.email eq email }
            println("Filas eliminadas" + filasEliminadas)
            filasEliminadas > 0  // Retorna `true` si eliminó algún usuario
        }
    }
    fun obtenerUsuarioPorEmail(email: String): Usuario? {
        return transaction {
            UsuarioTable
                .select { UsuarioTable.email eq email }
                .map {
                    Usuario(
                        username = it[UsuarioTable.username],
                        nom = it[UsuarioTable.nom],
                        email = it[UsuarioTable.email],
                        contrasenya = it[UsuarioTable.contrasenya],
                        idioma = Idioma.valueOf(it[UsuarioTable.idioma]),  // Asumiendo que se ha mapeado adecuadamente
                        sesionIniciada = it[UsuarioTable.sesionIniciada],
                        isAdmin = it[UsuarioTable.isAdmin]
                    )
                }.singleOrNull() // Devuelve el único usuario o null si no se encuentra
        }
    }

    // Actualizar usuario
    fun actualizar(usuario: Usuario) {
        UsuarioTable
            .update({ UsuarioTable.email eq usuario.email }) {
                it[sesionIniciada] = usuario.sesionIniciada
            }
    }

}