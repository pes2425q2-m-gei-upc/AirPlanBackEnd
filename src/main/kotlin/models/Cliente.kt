package org.example.models
import org.example.models.Usuario
import org.example.enums.Idioma

class Cliente(
    idUsuario: Int,
    nom: String,
    email: String,
    contraseña: String,
    idioma: Idioma,
    sesionIniciada: Boolean,
    isAdmin: Boolean,
    var level: Int
) : Usuario(idUsuario, nom, email, contraseña, idioma, sesionIniciada, isAdmin) {
}