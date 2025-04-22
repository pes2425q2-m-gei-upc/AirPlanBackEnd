package org.example.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import org.example.controllers.ControladorInvitacions
import org.example.models.Invitacio
import org.example.repositories.InvitacioRepository
import org.example.repositories.ParticipantsActivitatsRepository

fun Route.invitacioRoutes() {
    val controladorInvitacions = ControladorInvitacions(ParticipantsActivitatsRepository(), InvitacioRepository())

    route("/api/invitacions") {

        // Ruta para obtener las invitaciones de un usuario
        get("/{username}") {
            val username = call.parameters["username"]
            if (username == null) {
                call.respond(HttpStatusCode.BadRequest, "Username is required")
                return@get
            }

            try {
                val invitacions = controladorInvitacions.obtenirNomsActivitatsAmbInvitacionsPerUsuari(username)
                call.respond(HttpStatusCode.OK, invitacions)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error al obtener las invitaciones")
            }
        }

        // Ruta para invitar a un usuario a una actividad
        post("/invitar") {
            try {
                // Recibir los datos necesarios desde el frontend
                val request = call.receive<Map<String, String>>()
                val idAct = request["idAct"]?.toIntOrNull()
                val usAnfitrio = request["usAnfitrio"]
                val usDestinatari = request["usDestinatari"]

                // Validar que todos los datos estén presentes
                if (idAct == null || usAnfitrio.isNullOrBlank() || usDestinatari.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Faltan datos requeridos")
                    return@post
                }

                // Llamar al controlador para crear la invitación
                val resultado = controladorInvitacions.crearInvitacio(idAct, usAnfitrio, usDestinatari)
                if (resultado) {
                    call.respond(HttpStatusCode.Created, "Invitación creada correctamente")
                } else {
                    call.respond(HttpStatusCode.Conflict, "No se pudo crear la invitación")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Error al procesar la solicitud")
            }
        }
    }
}