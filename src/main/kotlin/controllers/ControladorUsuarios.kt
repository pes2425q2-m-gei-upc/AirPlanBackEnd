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
        contrasena: String,
        idioma: Idioma,
        isAdmin: Boolean = false
    ): Usuario {
        val usuario = Usuario.crear(
            username = username,
            nom = nom,
            email = email,
            contrasena = contrasena,
            idioma = idioma,
            isAdmin = isAdmin
        )
        usuarios.add(usuario)
        return usuario
    }

    // Método para borrar un usuario por nombre de usuario
    fun borrarUsuario(username: String): Boolean {
        val usuario = usuarios.find { it.username == username } // Busca al usuario
        return if (usuario != null) {
            usuario.eliminarUsuario()
            usuarios.remove(usuario) // Elimina al usuario de la lista
            println("El usuario con username '$username' ha sido eliminado.")
            true
        } else {
            println("El usuario con username '$username' no existe.")
            false
        }
    }



    // Método para modificar un usuario existente
    fun modificarUsuario(
        username: String,            // Identificador del usuario a buscar
        nuevoNom: String? = null,    // Parámetros opcionales para actualizar
        nuevoEmail: String? = null,
        nuevaContrasena: String? = null,
        nuevoIdioma: Idioma? = null,
    ): Boolean? {
        val usuario = usuarios.find { it.username == username }
        return usuario?.modificarUsuario(nuevoNom, nuevoEmail, nuevaContrasena, nuevoIdioma)

    }


    // Método para listar usuarios
    fun listarUsuarios(): List<Usuario> {
        return usuarios
    }
}
