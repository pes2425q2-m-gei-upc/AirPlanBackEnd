package org.example.models
import org.example.enums.Idioma

open class Usuario(
    protected var idUsuario: Int,
    protected var nom: String,
    private var email: String,
    private var contraseña: String,
    protected var idioma: Idioma,
    private var sesionIniciada: Boolean,
    private var isAdmin: Boolean
) {
}