package org.example.models
import org.example.enums.Idioma

class Administrador(
    username: String,
    nom: String,
    email: String,
    contrasena: String,
    idioma: Idioma,
    sesionIniciada: Boolean,
    isAdmin: Boolean
) : Usuario(
    username = username,
    nom = nom,
    email = email,
    contrasena = contrasena,
    idioma = idioma,
    sesionIniciada = false,
    isAdmin = true // Siempre true para administradores
)
