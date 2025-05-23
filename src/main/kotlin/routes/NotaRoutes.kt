package org.example.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.controllers.ControladorNotes
import org.example.models.Nota
import org.example.repositories.NotaRepository

fun Route.notaRoutes() {
    val controladorNotes = ControladorNotes(NotaRepository())

    route("/notas") {
        // Get all notes for a user
        get("/{username}") {
            val username = call.parameters["username"]

            if (username != null) {
                val notes = controladorNotes.obtenirNotesPerUsuari(username).toList()
                call.respond(HttpStatusCode.OK, notes)
            } else {
                call.respond(HttpStatusCode.BadRequest, "Username is required")
            }
        }

        // Add a new note
        post {
            try {
                println("Rebent nota JSON")
                val nota = call.receive<Nota>()
                println("Nota rebuda: $nota")
                val result = controladorNotes.afegirNota(nota)
                println("Resultat afegir nota: $result")

                if (result) {
                    call.respond(HttpStatusCode.Created, "Nota afegida correctament")
                } else {
                    call.respond(HttpStatusCode.InternalServerError, "Error al afegir la nota")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Error en la solicitud: ${e.message}")
            }
        }

        // Update an existing note
        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()

            if (id != null) {
                try {
                    val novaNota = call.receive<Nota>()
                    val result = controladorNotes.editarNota(id, novaNota)

                    if (result) {
                        call.respond(HttpStatusCode.OK, "Nota modificada correctament")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "La nota no existeix")
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Error en la solicitud: ${e.message}")
                }
            } else {
                call.respond(HttpStatusCode.BadRequest, "Cal proporcionar un ID")
            }
        }

        // Delete a note
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()

            if (id != null) {
                val result = controladorNotes.eliminarNota(id)

                if (result) {
                    call.respond(HttpStatusCode.OK, "Nota eliminada correctament")
                } else {
                    call.respond(HttpStatusCode.NotFound, "La nota no existeix")
                }
            } else {
                call.respond(HttpStatusCode.BadRequest, "Cal proporcionar un ID")
            }
        }
    }
}