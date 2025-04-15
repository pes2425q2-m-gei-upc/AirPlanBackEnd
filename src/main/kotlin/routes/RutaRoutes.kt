package org.example.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import org.example.controllers.ControladorRuta
import org.example.models.Ruta

fun Route.rutaRoutes() {
    val rutaController = ControladorRuta()

    route("/api/rutas") {
        post("/crear") {
            try {
                val ruta = call.receive<Ruta>()
                val createdRuta = rutaController.crearRuta(ruta)
                call.respond(HttpStatusCode.Created, createdRuta)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Error en processar la petició")
            }
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id != null) {
                val result = rutaController.eliminarRuta(id)
                if (result) {
                    call.respond(HttpStatusCode.OK, "Ruta eliminada correctament")
                } else {
                    call.respond(HttpStatusCode.NotFound, "Ruta no trobada")
                }
            } else {
                call.respond(HttpStatusCode.BadRequest, "ID invàlid")
            }
        }

        get {
            val rutas = rutaController.obtenirTotesRutes()
            call.respond(HttpStatusCode.OK, rutas)
        }
    }
}