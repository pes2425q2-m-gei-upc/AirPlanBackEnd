package org.example.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import org.example.controllers.ControladorInvitacions
import org.example.controllers.ControladorUsuarios
import org.example.repositories.InvitacioRepository
import org.example.repositories.ParticipantsActivitatsRepository
import org.example.repositories.UsuarioRepository

fun Route.invitacioRoutes() {
    val controladorInvitacions = ControladorInvitacions(ParticipantsActivitatsRepository(), InvitacioRepository(), UsuarioRepository())
    val controladorUsuarios = ControladorUsuarios(UsuarioRepository())

    route("/api/invitacions") {

        // Ruta para obtener las invitaciones de un usuario
        get("/{username}") {
            val username = call.parameters["username"]
            if (username == null) {
                call.respond(HttpStatusCode.BadRequest, "Username is required")
                return@get
            }

            try {
                val invitacions = controladorInvitacions.obtenirActivitatsAmbInvitacionsPerUsuari(username)
                call.respond(HttpStatusCode.OK, invitacions)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error al obtener las invitaciones")
            }
        }

        //Ruta para aceptar una invitación
        post("/acceptar") {
            try {
                // Recibir el cuerpo de la solicitud como un mapa
                val request = call.receive<Map<String, String>>()
                println("Datos recibidos: $request")

                // Extraer los valores del mapa
                val activityId = request["activityId"]?.toIntOrNull()
                val username = request["username"]

                if (activityId == null) {
                    println("Error: Falta activityId")
                    call.respond(HttpStatusCode.BadRequest, "Se requiere un id de actividad")
                    return@post
                }

                if (username == null) {
                    println("Error: Falta usDestinatari")
                    call.respond(HttpStatusCode.BadRequest, "Se requiere un usuario")
                    return@post
                }

                val invitacio = controladorInvitacions.listarInvitacions().find { (it.id_act == activityId) and (it.us_destinatari.equals(username)) }
                println("Invitación encontrada: $invitacio")
                if (invitacio == null) {
                    call.respond(HttpStatusCode.NotFound, "No se encontró la invitación")
                    return@post
                }
                val resultado = controladorInvitacions.acceptarInvitacio(invitacio)
                if (resultado) {
                    call.respond(HttpStatusCode.OK, "Invitación aceptada")
                } else {
                    call.respond(HttpStatusCode.NotFound, "No se encontró la invitación")
                }
            } catch (e: Exception) {
                println("Error al procesar la solicitud: ${e.message}")
                call.respond(HttpStatusCode.BadRequest, "Error al procesar la solicitud")
            }
        }

        // Ruta para rechazar una invitación
        post("/rebutjar") {
            try {
                // Recibir el cuerpo de la solicitud como un mapa
                val request = call.receive<Map<String, String>>()
                println("Datos recibidos: $request")

                // Extraer los valores del mapa
                val activityId = request["activityId"]?.toIntOrNull()
                val username = request["username"]

                if (activityId == null) {
                    println("Error: Falta activityId")
                    call.respond(HttpStatusCode.BadRequest, "Se requiere un id de actividad")
                    return@post
                }

                if (username == null) {
                    println("Error: Falta usDestinatari")
                    call.respond(HttpStatusCode.BadRequest, "Se requiere un usuario")
                    return@post
                }

                // Buscar la invitación en la lista
                val invitacio = controladorInvitacions.listarInvitacions().find { (it.id_act == activityId) and (it.us_destinatari.equals(username)) }
                println("Invitación encontrada: $invitacio")
                if (invitacio == null) {
                    call.respond(HttpStatusCode.NotFound, "No se encontró la invitación")
                    return@post
                }
                val resultado = controladorInvitacions.rebutjarInvitacio(invitacio)
                if (resultado) {
                    call.respond(HttpStatusCode.OK, "Invitación rechazada")
                } else {
                    call.respond(HttpStatusCode.NotFound, "No se encontró la invitación")
                }
            } catch (e: Exception) {
                println("Error al procesar la solicitud: ${e.message}")
                call.respond(HttpStatusCode.BadRequest, "Error al procesar la solicitud")
            }
        }

        // Ruta para invitar a un usuario a una actividad
        post("/invitar") {
            try {
                // Recibir los datos necesarios desde el frontend
                val request = call.receive<Map<String, String>>()
                val idAct = request["activityId"]?.toIntOrNull()
                val usAnfitrio = request["creator"]
                val usDestinatari = request["username"]

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

        //Ruta para comprobar si un usuario ha sido invitado a una actividad
        get("/check/{activityId}/{username}") {
            val activityId = call.parameters["activityId"]?.toIntOrNull()
            val username = call.parameters["username"]

            if (activityId == null || username == null) {
                call.respond(HttpStatusCode.BadRequest, "Faltan datos requeridos")
                return@get
            }

            // Comprobar si el usuario ha sido invitado a la actividad
            val invitacio = controladorInvitacions.listarInvitacions().find { (it.id_act == activityId) and (it.us_destinatari.equals(username)) }
            val response = mapOf("hasInvitation" to (invitacio != null))
            call.respond(HttpStatusCode.OK, response)
        }

        //Ruta para buscar usuarios
        get("/search") {
            val query = call.request.queryParameters["query"]

            if (query.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Query parameter is required")
                return@get
            }

            try {
                // Obtener todos los usuarios
                val allUsers = controladorUsuarios.listarUsuarios()

                // Filtrar usuarios que coincidan con la query
                val matchingUsers = allUsers.filter { username ->
                    username.startsWith(query, ignoreCase = true)
                }

                // Mapear los resultados
                val result = matchingUsers.map { username ->
                    mapOf("username" to username)
                }

                call.respond(HttpStatusCode.OK, result)
            } catch (e: Exception) {
                println("Error en /search: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, "Error fetching users")
            }
        }
    }
}