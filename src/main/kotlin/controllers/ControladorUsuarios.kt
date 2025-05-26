package org.example.controllers

import org.example.repositories.UsuarioRepository
import org.example.models.*
import org.example.models.UserTypeInfo  // Add import for UserTypeInfo
import org.example.enums.Idioma
import org.jetbrains.exposed.sql.transactions.transaction
import org.example.websocket.WebSocketManager
import kotlinx.coroutines.runBlocking
import org.example.services.PerspectiveService  // Add import for PerspectiveService
import org.example.exceptions.InappropriateContentException  // Import exception from new package

class ControladorUsuarios(
    private val usuarioRepository: UsuarioRepository,
    private val perspectiveService: PerspectiveService = PerspectiveService() // Inject PerspectiveService
) {

    // Crear nuevo usuario
    fun crearUsuario(username: String, nom: String, email: String, idioma: String, isAdmin: Boolean): Usuario? {
        // Block inappropriate content via Perspective API (batch)
        val inputs = listOf(nom, username, email)
        val results = runBlocking { perspectiveService.analyzeMessages(inputs) }
        if (results.any { it }) {
            val fields = listOf("nom", "username", "email")
            val flaggedIndex = results.indexOfFirst { it }
            // Throw exception indicating which field is inappropriate
            throw InappropriateContentException(fields[flaggedIndex])
        }

        val idiomaEnum = try {
            Idioma.valueOf(idioma)
        } catch (e: IllegalArgumentException) {
            Idioma.Castellano  // Valor por defecto si el idioma no es válido
        }

        val usuario = Usuario(
            username = username,
            nom = nom,
            email = email,
            idioma = idiomaEnum,
            sesionIniciada = false, // Por defecto, no tiene la sesión iniciada
            isAdmin = isAdmin
        )

        val success = usuarioRepository.agregarUsuario(usuario)
        return if (success) usuario else null
    }

    // Listar todos los usuarios
    fun listarUsuarios(): List<Usuario> {
        // Implementar lista de usuarios en el UsuarioRepository
        return listOf() // Temporal hasta implementar el método en UsuarioRepository
    }

    // Eliminar usuario por email
    fun eliminarUsuario(email: String, clientId: String? = null): Boolean {
        // Obtener el usuario antes de eliminarlo para tener su username
        val usuario = usuarioRepository.obtenerUsuarioPorEmail(email)

        // Si encontramos el usuario, notificamos antes de eliminarlo
        if (usuario != null) {
            // Notificar a otros dispositivos que la cuenta ha sido eliminada
            runBlocking {
                WebSocketManager.instance.notifyAccountDeleted(
                    username = usuario.username,
                    email = email,
                    clientId = clientId // Pasamos el clientId para evitar notificar al mismo dispositivo
                )
            }
        }

        // Procedemos con la eliminación
        return usuarioRepository.eliminarUsuario(email)
    }

    // Buscar usuario por nombre de usuario
    fun comprobarNombreUsuario(username: String): Boolean {
        return usuarioRepository.obtenerUsuarioPorUsername(username) != null
    }

    // Comprobar si existe un usuario con el email proporcionado
    fun comprobarEmailUsuario(email: String): Boolean {
        return usuarioRepository.obtenerUsuarioPorEmail(email) != null
    }

    // Actualizar usuario
    fun actualizarUsuario(
        currentEmail: String,
        nuevoNom: String?,
        nuevoUsername: String?,
        nuevoIdioma: String?,
        nuevoCorreo: String?
    ): Boolean {
        val success = usuarioRepository.actualizarUsuario(
            currentEmail, nuevoNom, nuevoUsername, nuevoIdioma, nuevoCorreo
        )

        // Si la actualización incluye cambio de correo, notificamos a todos los dispositivos
        if (success && nuevoCorreo != null && nuevoCorreo != currentEmail) {
            val usuario = usuarioRepository.obtenerUsuarioPorEmail(nuevoCorreo)
                ?: usuarioRepository.obtenerUsuarioPorEmail(currentEmail)

            if (usuario != null) {
                // Notificar cambio de correo a través de WebSockets
                runBlocking {
                    WebSocketManager.instance.notifyProfileUpdate(
                        username = usuario.username,
                        email = currentEmail, // Usamos el correo anterior para encontrar sesiones
                        updatedFields = listOf("email")
                    )

                    // También notificamos usando el nuevo correo por si ya hay sesiones registradas con él
                    if (nuevoCorreo != currentEmail) {
                        WebSocketManager.instance.notifyProfileUpdate(
                            username = usuario.username,
                            email = nuevoCorreo,
                            updatedFields = listOf("email")
                        )
                    }
                }
            }
        }

        return success
    }

    // Alias para actualizarUsuario para mantener compatibilidad con código existente
    fun modificarUsuario(
        currentEmail: String,
        nuevoNom: String?,
        nuevoUsername: String?,
        nuevoIdioma: String?,
        nuevoCorreo: String?,
        nuevaPhotoUrl: String? = null // Añadir parámetro para la URL de la foto
    ): Boolean {
        // Content filtering: check new name and new email if provided
        val toCheck = listOfNotNull(nuevoNom, nuevoCorreo)
        if (toCheck.isNotEmpty()) {
            val results = runBlocking { perspectiveService.analyzeMessages(toCheck) }
            if (results.any { it }) {
                val fields = listOf("nom", "email")
                val idx = results.indexOfFirst { it }
                throw InappropriateContentException(fields[idx])
            }
        }
        // Simplemente pasamos todos los parámetros al método actualizarUsuario del repositorio
        // y devolvemos el resultado, sin la notificación duplicada
        return usuarioRepository.actualizarUsuario(
            currentEmail, nuevoNom, nuevoUsername, nuevoIdioma, nuevoCorreo, nuevaPhotoUrl
        )
    }

    // Método para listar usuarios
    fun listarUsernames(): List<String> {
        return usuarioRepository.listarUsuarios()
    }

    // Obtener usuario por email
    fun obtenerUsuarioPorEmail(email: String): Usuario? {
        return usuarioRepository.obtenerUsuarioPorEmail(email)
    }

    // Obtener usuario por username
    fun obtenerUsuarioPorUsername(username: String): Usuario? {
        return usuarioRepository.obtenerUsuarioPorUsername(username)
    }

    // Método para actualizar el usuario en la base de datos (para cambiar sesionIniciada)
    fun actualizarSesion(email: String, sesionIniciada: Boolean) {
        usuarioRepository.actualizarSesion(email, sesionIniciada)
    }

    fun cerrarSesion(email: String): Boolean {
        println("Cerrando sesión para el usuario con email: $email")
        return usuarioRepository.cerrarSesion(email)
    }

    fun isUserAdmin(email: String): Boolean {
        val usuario = usuarioRepository.obtenerUsuarioPorEmail(email)
        return usuario?.isAdmin ?: false
    }

    // Método para actualizar directamente el correo electrónico
    fun actualizarCorreoDirecto(oldEmail: String, newEmail: String, clientId: String? = null): Boolean {
        // Content filtering: check new email
        val results = runBlocking { perspectiveService.analyzeMessages(listOf(newEmail)) }
        if (results.firstOrNull() == true) {
            throw InappropriateContentException("email")
        }
        val usuario = usuarioRepository.obtenerUsuarioPorEmail(oldEmail)
        val success = usuarioRepository.actualizarCorreoDirecto(oldEmail, newEmail)

        // Si la actualización fue exitosa y tenemos los datos del usuario, enviamos notificación
        if (success && usuario != null) {
            println("📢 Notificando cambio de correo: $oldEmail → $newEmail")

            // Notificar a todos los dispositivos conectados con el mismo usuario/email
            runBlocking {
                WebSocketManager.instance.notifyProfileUpdate(
                    username = usuario.username,
                    email = oldEmail, // Usamos el correo anterior para encontrar sesiones
                    updatedFields = listOf("email"),
                    clientId = clientId // Pasar el clientId para evitar notificaciones a este dispositivo
                )

                // También notificamos usando el nuevo correo por si ya hay sesiones registradas con él
                WebSocketManager.instance.notifyProfileUpdate(
                    username = usuario.username,
                    email = newEmail,
                    updatedFields = listOf("email"),
                    clientId = clientId // Pasar el clientId también para las notificaciones al nuevo correo
                )
            }
        }

        return success
    }

    // Obtener tipo de usuario (admin o cliente) y nivel del cliente
    fun obtenerTipoYNivelUsuario(username: String): UserTypeInfo {
        return usuarioRepository.obtenerTipoYNivelUsuario(username)
    }

    // Obtener la URL de la foto de perfil de un usuario
    fun obtenerPhotoUrlPorEmail(email: String): String? {
        return usuarioRepository.obtenerPhotoUrlPorEmail(email)
    }

}
