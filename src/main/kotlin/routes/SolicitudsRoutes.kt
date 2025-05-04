package org.example.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import org.example.controllers.ControladorSolicitudsUnio
import repositories.SolicitudRepository

fun Route.solicitudRoutes() {
    val solicitudController = ControladorSolicitudsUnio(SolicitudRepository())

    route("/api/solicituds") {

        post("/{usernameAnfitrio}/{usernameSolicitant}/{idActivitat}") {
            val usernameAnfitrio = call.parameters["usernameAnfitrio"]
            val usernameSolicitant = call.parameters["usernameSolicitant"]
            val idActivitat = call.parameters["idActivitat"]?.toIntOrNull()

            if (usernameAnfitrio != null && usernameSolicitant != null && idActivitat != null) {
                val result = solicitudController.enviarSolicitud(usernameAnfitrio, usernameSolicitant, idActivitat)
                if (result) {
                    call.respond(HttpStatusCode.Created, "Solicitud enviada correctamente")
                } else {
                    call.respond(HttpStatusCode.Conflict, "Error al enviar la solicitud")
                }
            } else {
                call.respond(HttpStatusCode.BadRequest, "Parámetros inválidos")
            }
        }

        delete("/{usernameSolicitant}/{idActivitat}") {
            val usernameSolicitant = call.parameters["usernameSolicitant"]
            val idActivitat = call.parameters["idActivitat"]?.toIntOrNull()

            if (usernameSolicitant != null && idActivitat != null) {
                val result = solicitudController.eliminarSolicitud(usernameSolicitant, idActivitat)
                if (result) {
                    call.respond(HttpStatusCode.OK, "Solicitud eliminada correctamente")
                } else {
                    call.respond(HttpStatusCode.NotFound, "Solicitud no encontrada")
                }
            } else {
                call.respond(HttpStatusCode.BadRequest, "Parámetros inválidos")
            }
        }

        get("/{username}") {
            val username = call.parameters["username"]

            if (username != null) {
                val solicitudes = solicitudController.obtenirSolicitudesPerUsuari(username)
                call.respond(HttpStatusCode.OK, solicitudes)
            } else {
                call.respond(HttpStatusCode.BadRequest, "Username no proporcionado")
            }
        }
        get ("/{usernameAnfitrio}/{usernameSolicitant}/{idActivitat}") {
            val usernameAnfitrio = call.parameters["usernameAnfitrio"]
            val usernameSolicitant = call.parameters["usernameSolicitant"]
            val idActivitat = call.parameters["idActivitat"]?.toIntOrNull()

            if (usernameAnfitrio != null && usernameSolicitant != null && idActivitat != null) {
                val result = solicitudController.activitatJaSolicitada(usernameAnfitrio, usernameSolicitant, idActivitat)
                call.respond(HttpStatusCode.OK, mapOf("result" to result))
            } else {
                call.respond(HttpStatusCode.BadRequest, "Parámetros inválidos")
            }
        }
    }
}