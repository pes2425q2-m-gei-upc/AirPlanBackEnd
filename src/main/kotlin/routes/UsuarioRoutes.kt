package org.example.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import org.example.controllers.ControladorUsuarios
import org.example.models.Usuario
import org.example.repositories.UsuarioRepository


fun Route.usuarioRoutes() {
    val usuarioController = ControladorUsuarios(UsuarioRepository())
    println("Ha arribat a UsuarioRoutes")  // Depuració
    route("/api/usuaris") {
        println("Ha arribat a /api/usuaris")  // Depuració
        post("/crear") {
            try {
                val receivedText = call.receiveText()
                println("Dades rebudes: $receivedText")

                // Deserialitzar manualment el JSON
                val usuario = kotlinx.serialization.json.Json.decodeFromString<Usuario>(receivedText)
                println("Usuari deserialitzat: $usuario")

                val resultado = usuarioController.crearUsuario(
                    username = usuario.username,
                    nom = usuario.nom,
                    email = usuario.email,
                    contrasenya = usuario.contrasenya,
                    idioma = usuario.idioma,
                    isAdmin = usuario.isAdmin
                )

                if (resultado != null) {
                    call.respond(HttpStatusCode.Created, "Usuari creat correctament")
                } else {
                    call.respond(HttpStatusCode.Conflict, "L'usuari ja existeix")
                }
            } catch (e: Exception) {
                println("Error: ${e.message}")
                call.respond(HttpStatusCode.BadRequest, "Error en processar la petició")
            }
        }
        delete("/api/eliminar-usuario/{email}") {
            val email = call.parameters["email"]

            if (email == null) {
                call.respond(HttpStatusCode.BadRequest, "El correo es requerido")
                return@delete
            }

            val eliminado = usuarioController.eliminarUsuario(email)

            if (eliminado) {
                call.respond(HttpStatusCode.OK, "Usuario eliminado correctamente")
            } else {
                call.respond(HttpStatusCode.NotFound, "No se encontró un usuario con ese correo")
            }
        }
    }
}