package org.example.controllers

import org.example.enums.Idioma
import org.example.models.Usuario

class ControladorUsuarios {
    private val usuarios = mutableListOf<Usuario>()

    // Método para crear un usuario y agregarlo a la lista
    fun crearUsuario(
        username: String,
        nom: String,
        email: String,
        contraseña: String,
        idioma: Idioma,
        isAdmin: Boolean = false
    ): Usuario {
        val usuario = Usuario.crear(
            username = username,
            nom = nom,
            email = email,
            contraseña = contraseña,
            idioma = idioma,
            isAdmin = isAdmin
        )
        usuarios.add(usuario)
        return usuario
    }

    // Método para borrar un usuario por nombre de usuario
    fun borrarUsuario(username: String): Boolean {
        val usuario = usuarios.find { it.username == username }
        return if (usuario != null) {
            usuarios.remove(usuario)
            println("El usuario con username '$username' ha sido eliminado.")
            true
        } else {
            println("El usuario con username '$username' no existe.")
            false
        }
    }

    // Método para listar usuarios
    fun listarUsuarios(): List<Usuario> {
        return usuarios
    }
}
