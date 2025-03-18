package org.example.controllers

import org.example.enums.Idioma
import org.example.models.Usuario
import org.example.repositories.UsuarioRepository

class ControladorUsuarios (private val usuarioRepository: UsuarioRepository) {
    private val usuarios = mutableListOf<Usuario>()

    // Función para crear un nuevo usuario
    fun crearUsuario(
        username: String,
        nom: String,
        email: String,
        contrasenya: String,
        idioma: Idioma,
        isAdmin: Boolean = false
    ): Usuario {
        // Aquí se instancia un objeto Usuario llamando a su método `crear`
        val nuevoUsuario = Usuario(username, nom, email, contrasenya, idioma, false, isAdmin)

        usuarioRepository.agregarUsuario(nuevoUsuario)

        // Retorna el usuario creado
        return nuevoUsuario
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
        nuevaContrasenya: String? = null,
        nuevoIdioma: Idioma? = null,
    ): Boolean? {
        val usuario = usuarios.find { it.username == username }
        return usuario?.modificarUsuario(nuevoNom, nuevoEmail, nuevaContrasenya, nuevoIdioma)

    }


    // Método para listar usuarios
    fun listarUsuarios(): List<Usuario> {
        return usuarios
    }

    fun comprobarNombreUsuario(username: String): Boolean {
        return usuarios.any { it.username == username }
    }
}
