package org.example.models
import org.example.models.Usuario
import org.example.enums.Idioma

class Cliente(
    username: String,
    nom: String,
    email: String,
    contraseña: String,
    idioma: Idioma,
    sesionIniciada: Boolean,
    isAdmin: Boolean,
    var level: Int
) : Usuario(username, nom, email, contraseña, idioma, sesionIniciada, isAdmin) {

}