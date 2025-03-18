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

    route("/api/usuaris") {
        post("/crear") {
            val usuario = call.receive<Usuario>()
            val resultado = usuarioController.crearUsuario(
                username = usuario.username,
                nom = usuario.nom,
                email = usuario.email,
                contrasenya = usuario.contrasenya,
                idioma = usuario.idioma,
                isAdmin = usuario.isAdmin
            )

            if (resultado != null) {
                call.respond(HttpStatusCode.Created, "Usuario creado exitosamente")
            } else {
                call.respond(HttpStatusCode.Conflict, "El usuario ya existe")
            }
        }
    }
}