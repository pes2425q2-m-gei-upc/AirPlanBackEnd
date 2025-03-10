package org.example.models
import org.example.enums.Idioma

class Administrador(
    idUsuario: Int,
    nom: String,
    email: String,
    contraseña: String,
    idioma: Idioma,
    sesionIniciada: Boolean,
    isAdmin: Boolean
) : Usuario(idUsuario, nom, email, contraseña, idioma, sesionIniciada, isAdmin) {
}