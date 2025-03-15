package org.example.models

import kotlinx.serialization.Serializable
import org.example.enums.Idioma

@Serializable
open class Usuario(
    var username: String,
    var nom: String,
    var email: String,
    var contrasena: String,
    var idioma: Idioma,
    var sesionIniciada: Boolean,
    var isAdmin: Boolean,
    val activitats: MutableList<Activitat> = mutableListOf()
) {

    companion object {
        fun crear(
            username: String,
            nom: String,
            email: String,
            contrasena: String,
            idioma: Idioma,
            isAdmin: Boolean = false
        ): Usuario {
            // Validaciones antes de la creación del usuario
            require(username.length >= 5) { "El nombre de usuario debe tener al menos 5 caracteres." }
            require(email.contains("@")) { "El correo electrónico debe ser válido." }
            require(contrasena.length >= 8) { "La contraseña debe tener al menos 8 caracteres." }

            return Usuario(
                username = username,
                nom = nom,
                email = email,
                contrasena = contrasena,
                idioma = idioma,
                sesionIniciada = false, // Inicialmente la sesión no está iniciada
                isAdmin = isAdmin // Determinado por un parámetro
            )
        }
    }

    fun modificarUsuario(
        nuevoNom: String? = null,    // Parámetros opcionales para actualizar
        nuevoEmail: String? = null,
        nuevaContrasena: String? = null,
        nuevoIdioma: Idioma? = null,
    ): Boolean {
        // Modificar los campos si se proporcionan valores nuevos
        nuevoNom?.let { this.nom = it }
        nuevoEmail?.let { this.email = it }
        nuevaContrasena?.let { this.contrasena = it }
        nuevoIdioma?.let { this.idioma = it }

        println("El usuario ha sido modificado.")
        return true
    }

    fun eliminarUsuario() {
        // Eliminar de la base de dades
    }
    fun afegirActivitat(activitat: Activitat) {
        activitats.add(activitat)
    }
}
