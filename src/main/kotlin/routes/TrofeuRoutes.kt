package org.example.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.controllers.ControladorTrofeus
import org.example.repositories.TrofeuRepository

fun Route.trofeuRoutes() {
    val controladorTrofeus = ControladorTrofeus(TrofeuRepository())

    route("/api/trofeus/{usuari}") {
        get {
            val usuari = call.parameters["usuari"] ?: return@get call.respondText(
                "Usuari no proporcionat", status = io.ktor.http.HttpStatusCode.BadRequest
            )
            val trofeusPerUsuari = controladorTrofeus.obtenirTrofeusPerUsuari(usuari)

            println("Datos enviados al frontend: $trofeusPerUsuari")

            call.respond(trofeusPerUsuari)
        }
    }
}