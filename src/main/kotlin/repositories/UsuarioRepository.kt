package org.example.repositories

import org.example.database.UsuarioTable
import org.example.database.ClienteTable
import org.example.enums.Idioma
import org.example.models.Usuario
import org.example.models.UserTypeInfo // Added import for UserTypeInfo
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
                it[esExtern] = usuario.esExtern
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
                        isAdmin = it[UsuarioTable.isAdmin],
                        esExtern = it[UsuarioTable.esExtern] // Añadido para obtener el campo esExtern
                    )
                }.singleOrNull() // Devuelve el único usuario o null si no se encuentra
        }
    }

    // Método para obtener un usuario por su username
    fun obtenerUsuarioPorUsername(username: String): Usuario? {
        return transaction {
            UsuarioTable
                .select { UsuarioTable.username eq username }
                .map {
                    Usuario(
                        username = it[UsuarioTable.username],
                        nom = it[UsuarioTable.nom],
                        email = it[UsuarioTable.email],
                        idioma = Idioma.valueOf(it[UsuarioTable.idioma]),
                        sesionIniciada = it[UsuarioTable.sesionIniciada],
                        isAdmin = it[UsuarioTable.isAdmin],
                        esExtern = it[UsuarioTable.esExtern]
                    )
                }.singleOrNull() // Devuelve el único usuario o null si no se encuentra
        }
    }

    // Actualizar usuario
    fun actualizarSesion(email: String, sesion: Boolean) {
        transaction {
            UsuarioTable
                .update({ UsuarioTable.email eq email }) {
                    it[sesionIniciada] = sesion
                }
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
        nuevoCorreo: String?,
        nuevaPhotoUrl: String? = null  // Añadir parámetro para la URL de la imagen
    ): Boolean {
        return transaction {
            val filasActualizadas = UsuarioTable.update({ UsuarioTable.email eq currentEmail }) {
                if (nuevoNom != null) it[UsuarioTable.nom] = nuevoNom
                if (nuevoUsername != null) it[UsuarioTable.username] = nuevoUsername
                if (nuevoIdioma != null) it[UsuarioTable.idioma] = nuevoIdioma
                if (nuevaPhotoUrl != null) it[UsuarioTable.photourl] = nuevaPhotoUrl  // Guardar la URL de la imagen

                // Ahora actualizamos directamente el correo si se proporciona uno nuevo
                if (nuevoCorreo != null) {
                    println("📧 Actualizando correo directamente: $currentEmail → $nuevoCorreo")
                    it[UsuarioTable.email] = nuevoCorreo
                }
            }
            filasActualizadas > 0
        }
    }

    // Actualizar directamente el correo electrónico
    fun actualizarCorreoDirecto(oldEmail: String, newEmail: String): Boolean {
        return transaction {
            val filasActualizadas = UsuarioTable.update({ UsuarioTable.email eq oldEmail }) {
                it[UsuarioTable.email] = newEmail
            }
            filasActualizadas > 0
        }
    }

    // Obtener el tipo de usuario y su nivel (si es cliente)
    fun obtenerTipoYNivelUsuario(username: String): UserTypeInfo {
        return transaction {
            // Primero verificamos si el usuario existe y si es admin
            val usuario = UsuarioTable
                .select { UsuarioTable.username eq username }
                .map {
                    Pair(
                        it[UsuarioTable.username],
                        it[UsuarioTable.isAdmin]
                    )
                }.singleOrNull()

            if (usuario == null) {
                UserTypeInfo(
                    tipo = "error",
                    username = username,
                    error = "Usuario no encontrado"
                )
            } else {
                val isAdmin = usuario.second

                if (isAdmin) {
                    // Si es admin, devolvemos tipo "admin" sin nivel
                    UserTypeInfo(
                        tipo = "admin",
                        username = usuario.first
                    )
                } else {
                    // Si no es admin, es cliente, buscamos su nivel
                    val clientInfo = ClienteTable
                        .select { ClienteTable.username eq username }
                        .map {
                            it[ClienteTable.nivell]
                        }.singleOrNull()

                    UserTypeInfo(
                        tipo = "cliente",
                        username = usuario.first,
                        nivell = clientInfo ?: 0
                    )
                }
            }
        }
    }

    fun existeUsuario(username: String): Boolean {
        return transaction {
            UsuarioTable
                .select { UsuarioTable.username eq username }
                .count() > 0
        }
    }

    //Lista todos los usuarios con su username
    fun listarUsuarios(): List<String> {
        return transaction {
            UsuarioTable
                .selectAll()
                .map { it[UsuarioTable.username] }
        }
    }

    // Obtener la URL de la foto de perfil de un usuario
    fun obtenerPhotoUrlPorEmail(email: String): String? {
        return transaction {
            UsuarioTable
                .slice(UsuarioTable.photourl)
                .select { UsuarioTable.email eq email }
                .map { it[UsuarioTable.photourl] }
                .singleOrNull()
        }
    }

    fun obtenirExterns(): List<Usuario> {
        return transaction {
            UsuarioTable
                .select { UsuarioTable.esExtern eq true }
                .map {
                    Usuario(
                        username = it[UsuarioTable.username],
                        nom = it[UsuarioTable.nom],
                        email = it[UsuarioTable.email],
                        idioma = Idioma.valueOf(it[UsuarioTable.idioma]),
                        sesionIniciada = it[UsuarioTable.sesionIniciada],
                        isAdmin = it[UsuarioTable.isAdmin],
                        esExtern = it[UsuarioTable.esExtern]
                    )
                }
        }
    }
}