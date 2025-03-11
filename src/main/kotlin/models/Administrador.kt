package org.example.models
import org.example.enums.Idioma

class Administrador(
    username: String,
    nom: String,
    email: String,
    contraseña: String,
    idioma: Idioma,
    sesionIniciada: Boolean,
    isAdmin: Boolean
) : Usuario(username, nom, email, contraseña, idioma, sesionIniciada, isAdmin) {
}