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
                
                // IMPORTANTE: No actualizar directamente el email, sino usar pendingEmail
                // para esperar confirmación de Firebase
                if (nuevoCorreo != null) {
                    println("📧 Guardando nuevo correo como pendiente: $currentEmail → $nuevoCorreo")
                    it[UsuarioTable.pendingEmail] = nuevoCorreo
                }
                // Eliminamos la línea que actualizaba directamente UsuarioTable.email
            }
            filasActualizadas > 0
        }
    }

    // Guardar un correo pendiente de verificación
    fun guardarCorreoPendiente(currentEmail: String, pendingEmail: String): Boolean {
        return transaction {
            val filasActualizadas = UsuarioTable.update({ UsuarioTable.email eq currentEmail }) {
                it[UsuarioTable.pendingEmail] = pendingEmail
            }
            filasActualizadas > 0
        }
    }

    // Confirmar el cambio de correo (una vez verificado en Firebase)
    fun confirmarCambioCorreo(currentEmail: String, oldEmail: String? = null): Boolean {
        return transaction {
            // Caso 0: Si se proporciona un correo antiguo, intentamos buscar directamente ese usuario
            if (oldEmail != null) {
                println("🔍 Buscando usuario con correo antiguo: $oldEmail")
                val usuarioConOldEmail = UsuarioTable
                    .select { UsuarioTable.email eq oldEmail }
                    .singleOrNull()
                
                if (usuarioConOldEmail != null) {
                    println("🔄 Actualizando directamente de $oldEmail a $currentEmail")
                    val filasActualizadas = UsuarioTable.update({ UsuarioTable.email eq oldEmail }) {
                        it[UsuarioTable.email] = currentEmail
                        it[UsuarioTable.pendingEmail] = null
                    }
                    return@transaction filasActualizadas > 0
                }
            }
            
            // Caso 1: El correo actual es el pendingEmail de otro usuario
            val usuarioConPendingEmail = UsuarioTable
                .select { UsuarioTable.pendingEmail eq currentEmail }
                .singleOrNull()
            
            if (usuarioConPendingEmail != null) {
                val oldEmailFromDB = usuarioConPendingEmail[UsuarioTable.email]
                println("🔄 Detectada actualización de correo. Antiguo: $oldEmailFromDB, Nuevo: $currentEmail")
                
                val filasActualizadas = UsuarioTable.update({ UsuarioTable.email eq oldEmailFromDB }) {
                    it[UsuarioTable.email] = currentEmail
                    it[UsuarioTable.pendingEmail] = null
                }
                return@transaction filasActualizadas > 0
            } 
            
            // Caso 2: El usuario actual tiene un correo pendiente
            val usuario = UsuarioTable
                .select { UsuarioTable.email eq currentEmail }
                .singleOrNull()
            
            if (usuario != null && usuario[UsuarioTable.pendingEmail] != null) {
                val pendingEmail = usuario[UsuarioTable.pendingEmail]
                println("📧 Actualizando correo: $currentEmail → $pendingEmail")
                
                val filasActualizadas = UsuarioTable.update({ UsuarioTable.email eq currentEmail }) {
                    it[UsuarioTable.email] = pendingEmail!!
                    it[UsuarioTable.pendingEmail] = null
                }
                
                return@transaction filasActualizadas > 0
            } 
            
            println("❌ No se encontró correo pendiente para: $currentEmail")
            false
        }
    }

    // Cancelar el cambio de correo pendiente
    fun cancelarCambioCorreo(currentEmail: String): Boolean {
        return transaction {
            val filasActualizadas = UsuarioTable.update({ UsuarioTable.email eq currentEmail }) {
                it[UsuarioTable.pendingEmail] = null
            }
            filasActualizadas > 0
        }
    }

}