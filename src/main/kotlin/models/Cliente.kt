package org.example.models
import org.example.models.Usuario
import org.example.enums.Idioma
import java.time.LocalDateTime

class Cliente(
    username: String,
    nom: String,
    email: String,
    contraseña: String,
    idioma: Idioma,
    sesionIniciada: Boolean,
    isAdmin: Boolean,
    var level: Int
) : Usuario(
    username = username,
    nom = nom,
    email = email,
    contraseña = contraseña,
    idioma = idioma,
    sesionIniciada = false,
    isAdmin = false // Siempre false para clientes
)
