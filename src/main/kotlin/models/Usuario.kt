package org.example.models

import org.example.enums.Idioma

open class Usuario(
    var username: String,
    protected var nom: String,
    private var email: String,
    private var contraseña: String,
    protected var idioma: Idioma,
    private var sesionIniciada: Boolean,
    private var isAdmin: Boolean
) {

    companion object {
        fun crear(
            username: String,
            nom: String,
            email: String,
            contraseña: String,
            idioma: Idioma,
            isAdmin: Boolean = false
        ): Usuario {
            // Validaciones antes de la creación del usuario
            require(username.length >= 5) { "El nombre de usuario debe tener al menos 5 caracteres." }
            require(email.contains("@")) { "El correo electrónico debe ser válido." }
            require(contraseña.length >= 8) { "La contraseña debe tener al menos 8 caracteres." }

            return Usuario(
                username = username,
                nom = nom,
                email = email,
                contraseña = contraseña,
                idioma = idioma,
                sesionIniciada = false, // Inicialmente la sesión no está iniciada
                isAdmin = isAdmin // Determinado por un parámetro
            )
        }
    }
}
