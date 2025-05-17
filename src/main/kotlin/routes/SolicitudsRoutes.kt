package org.example.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import org.example.controllers.ControladorSolicitudsUnio
import repositories.SolicitudRepository

const val PARAMETROS_INVALIDOS = "Parámetros inválidos"

fun Route.solicitudRoutes() {
    val solicitudController = ControladorSolicitudsUnio(SolicitudRepository())

    route("/api/solicituds") {
        post("/{usernameAnfitrio}/{usernameSolicitant}/{idActivitat}") {
            call.enviarSolicitud(solicitudController)
        }
        delete("/{usernameSolicitant}/{idActivitat}") {
            call.eliminarSolicitud(solicitudController)
        }
        get("/{username}") {
            call.obtenirSolicitudsUsuari(solicitudController)
        }
        get("/{usernameAnfitrio}/{usernameSolicitant}/{idActivitat}") {
            call.activitatJaSolicitada(solicitudController)
        }
        get("/{idActivitat}/solicituds") {
            call.obtenirSolicitudsActivitat(solicitudController)
        }
    }
}

suspend fun ApplicationCall.enviarSolicitud(controller: ControladorSolicitudsUnio) {
    val usernameAnfitrio = parameters["usernameAnfitrio"]
    val usernameSolicitant = parameters["usernameSolicitant"]
    val idActivitat = parameters["idActivitat"]?.toIntOrNull()

    if (usernameAnfitrio != null && usernameSolicitant != null && idActivitat != null) {
        val result = controller.enviarSolicitud(usernameAnfitrio, usernameSolicitant, idActivitat)
        if (result) {
            respond(HttpStatusCode.Created, "Solicitud enviada correctamente")
        } else {
            respond(HttpStatusCode.Conflict, "Error al enviar la solicitud")
        }
    } else {
        respond(HttpStatusCode.BadRequest, PARAMETROS_INVALIDOS)
    }
}

suspend fun ApplicationCall.eliminarSolicitud(controller: ControladorSolicitudsUnio) {
    val usernameSolicitant = parameters["usernameSolicitant"]
    val idActivitat = parameters["idActivitat"]?.toIntOrNull()

    if (usernameSolicitant != null && idActivitat != null) {
        val result = controller.eliminarSolicitud(usernameSolicitant, idActivitat)
        if (result) {
            respond(HttpStatusCode.OK, "Solicitud eliminada correctamente")
        } else {
            respond(HttpStatusCode.NotFound, "Solicitud no encontrada")
        }
    } else {
        respond(HttpStatusCode.BadRequest, PARAMETROS_INVALIDOS)
    }
}

suspend fun ApplicationCall.obtenirSolicitudsUsuari(controller: ControladorSolicitudsUnio) {
    val username = parameters["username"]

    if (username != null) {
        val solicitudes = controller.obtenirSolicitudesPerUsuari(username)
        respond(HttpStatusCode.OK, solicitudes)
    } else {
        respond(HttpStatusCode.BadRequest, "Username no proporcionado")
    }
}

suspend fun ApplicationCall.activitatJaSolicitada(controller: ControladorSolicitudsUnio) {
    val usernameAnfitrio = parameters["usernameAnfitrio"]
    val usernameSolicitant = parameters["usernameSolicitant"]
    val idActivitat = parameters["idActivitat"]?.toIntOrNull()

    if (usernameAnfitrio != null && usernameSolicitant != null && idActivitat != null) {
        val result = controller.activitatJaSolicitada(usernameAnfitrio, usernameSolicitant, idActivitat)
        respond(HttpStatusCode.OK, mapOf("result" to result))
    } else {
        respond(HttpStatusCode.BadRequest, PARAMETROS_INVALIDOS)
    }
}

suspend fun ApplicationCall.obtenirSolicitudsActivitat(controller: ControladorSolicitudsUnio) {

    println("Obteniendo solicitudes para la actividad")
    val idActivitat = parameters["idActivitat"]?.toIntOrNull()

    if (idActivitat != null) {
        val solicitudes = controller.obtenirSolicitudesPerActivitat(idActivitat)
        respond(HttpStatusCode.OK, solicitudes)
    } else {
        respond(HttpStatusCode.BadRequest, "ID de actividad no proporcionado")
    }
}