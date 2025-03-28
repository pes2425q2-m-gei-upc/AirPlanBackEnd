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
        idioma: Idioma,
        isAdmin: Boolean = false
    ): Usuario {
        // Aquí se instancia un objeto Usuario llamando a su método `crear`
        val nuevoUsuario = Usuario(username, nom, email, idioma, true, isAdmin)

        usuarioRepository.agregarUsuario(nuevoUsuario)

        // Retorna el usuario creado
        return nuevoUsuario
    }

    // Método para borrar un usuario por nombre de usuario
    fun eliminarUsuario(email: String): Boolean {
        return usuarioRepository.eliminarUsuario(email)
    }


    // Método para modificar un usuario existente
    fun modificarUsuario(
        username: String,            // Identificador del usuario a buscar
        nuevoNom: String? = null,    // Parámetros opcionales para actualizar
        nuevaContrasenya: String? = null,
        nuevoIdioma: Idioma? = null,
    ): Boolean? {
        val usuario = usuarios.find { it.username == username }
        return usuario?.modificarUsuario(nuevoNom, nuevoIdioma)

    }


    // Método para listar usuarios
    fun listarUsuarios(): List<Usuario> {
        return usuarios
    }

    fun obtenerUsuarioPorEmail(email: String): Usuario? {
        return usuarioRepository.obtenerUsuarioPorEmail(email)
    }

    fun comprobarNombreUsuario(username: String): Boolean {
        return usuarios.any { it.username == username }
    }

    fun login(email: String?, contrasenya: String?): Usuario? {
        // Buscar el usuario por email y verificar la contraseña
        val usuario = usuarioRepository.obtenerUsuarioPorEmail(email ?: "")

        return usuario
    }

    // Método para actualizar el usuario en la base de datos (para cambiar sesionIniciada)
    fun actualizarSesion(email: String, sesionIniciada: Boolean) {
        usuarioRepository.actualizarSesion(email, sesionIniciada)
    }

    fun cerrarSesion(email: String): Boolean {
        println("Cerrando sesión para el usuario con email: $email")
        return usuarioRepository.cerrarSesion(email)
    }

    fun isUserAdmin(email: String): Boolean {
        val usuario = usuarioRepository.obtenerUsuarioPorEmail(email)
        return usuario?.isAdmin ?: false
    }

}
