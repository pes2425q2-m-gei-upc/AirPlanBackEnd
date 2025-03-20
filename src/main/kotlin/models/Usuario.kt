package org.example.models

import kotlinx.serialization.Serializable
import org.example.enums.Idioma

@Serializable
open class Usuario(
    var username: String,
    var nom: String,
    var email: String,
    var contrasenya: String,
    var idioma: Idioma,
    var sesionIniciada: Boolean,
    var isAdmin: Boolean,
    val activitats: MutableList<Activitat> = mutableListOf()
) {

    fun modificarUsuario(
        nuevoNom: String? = null,    // Parámetros opcionales para actualizar
        nuevoEmail: String? = null,
        nuevaContrasenya: String? = null,
        nuevoIdioma: Idioma? = null,
    ): Boolean {
        // Modificar los campos si se proporcionan valores nuevos
        nuevoNom?.let { this.nom = it }
        nuevoEmail?.let { this.email = it }
        nuevaContrasenya?.let { this.contrasenya = it }
        nuevoIdioma?.let { this.idioma = it }

        println("El usuario ha sido modificado.")
        return true
    }

    fun eliminarUsuario() {
        // Eliminar de la base de dades
    }
    fun afegirActivitat(activitat: Activitat) {
        activitats.add(activitat)
    }
}
