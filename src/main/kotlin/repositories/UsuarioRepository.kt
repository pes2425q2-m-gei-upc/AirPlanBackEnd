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
    fun obtenerUsuarioPorEmail(email: String): Usuario? {
        return transaction {
            UsuarioTable
                .select { UsuarioTable.email eq email }
                .map {
                    Usuario(
                        username = it[UsuarioTable.username],
                        nom = it[UsuarioTable.nom],
                        email = it[UsuarioTable.email],
                        idioma = Idioma.valueOf(it[UsuarioTable.idioma]),  // Asumiendo que se ha mapeado adecuadamente
                        sesionIniciada = it[UsuarioTable.sesionIniciada],
                        isAdmin = it[UsuarioTable.isAdmin]
                    )
                }.singleOrNull() // Devuelve el único usuario o null si no se encuentra
        }
    }

    // Actualizar usuario
    fun actualizarSesion(email: String, sesion: Boolean) {
        UsuarioTable
            .update({ UsuarioTable.email eq email }) {
                it[sesionIniciada] = sesion
            }
    }

    fun cerrarSesion(email: String): Boolean {
        return transaction {
            val filasActualizadas = UsuarioTable.update({ UsuarioTable.email eq email }) {
                it[sesionIniciada] = false
            }
            println("Filas actualizadas: $filasActualizadas")
            filasActualizadas > 0  // Retorna `true` si se modificó alguna fila
        }
    }

    fun actualizarUsuario(
        currentEmail: String,
        nuevoNom: String?,
        nuevoUsername: String?,
        nuevoIdioma: String?,
        nuevoCorreo: String?
    ): Boolean {
        return transaction {
            val filasActualizadas = UsuarioTable.update({ UsuarioTable.email eq currentEmail }) {
                if (nuevoNom != null) it[UsuarioTable.nom] = nuevoNom
                if (nuevoUsername != null) it[UsuarioTable.username] = nuevoUsername
                if (nuevoIdioma != null) it[UsuarioTable.idioma] = nuevoIdioma
                if (nuevoCorreo != null) it[UsuarioTable.email] = nuevoCorreo
            }
            filasActualizadas > 0
        }
    }

}