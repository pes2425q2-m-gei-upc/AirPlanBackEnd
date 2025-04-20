package org.example.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.datetime.toKotlinLocalDateTime
import org.example.controllers.ControladorActivitat
import org.example.models.Activitat
import repositories.ActivitatFavoritaRepository
import repositories.ActivitatRepository
import java.sql.Timestamp

fun Route.activitatRoutes() {
    val activitatRepository = ActivitatRepository()
    val activitatFavoritaRepository = ActivitatFavoritaRepository() // Create an instance of ActivitatFavoritaRepository
    val activitatController = ControladorActivitat(activitatRepository, activitatFavoritaRepository) // Pass both repositories

    println("Ha arribat a ActivitatRoutes")  // Depuració
    route("/api/activitats") {
        println("Ha arribat a /api/activitats")  // Depuració

        post("/crear") {
            try {
                val receivedText = call.receiveText()
                println("Dades rebudes: $receivedText")

                // Deserialitzar manualment el JSON
                val activitat = kotlinx.serialization.json.Json.decodeFromString<Activitat>(receivedText)
                println("Activitat deserialitzada: $activitat")

                val resultado = activitatController.afegirActivitat(
                    nom = activitat.nom,
                    descripcio = activitat.descripcio,
                    ubicacio = activitat.ubicacio,
                    dataInici = activitat.dataInici,
                    dataFi = activitat.dataFi,
                    creador = activitat.creador
                )

                if (resultado != null) {
                    call.respond(HttpStatusCode.Created, "Activitat creada correctament")
                } else {
                    call.respond(HttpStatusCode.Conflict, "L'activitat ja existeix")
                }
            } catch (e: Exception) {
                println("Error: ${e.message}")
                call.respond(HttpStatusCode.BadRequest, "Error en processar la petició")
            }
        }

        get {
            try {
                val activitats = activitatController.obtenirTotesActivitats()
                call.respond(HttpStatusCode.OK, activitats)
            } catch (e: Exception) {
                println("Error: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, "Error en processar la petició")
            }
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()

            if (id != null) {
                val activitat = activitatController.obtenirActivitatPerId(id)
                if (activitat != null) {
                    call.respond(HttpStatusCode.OK, activitat)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Activitat no trobada")
                }
            } else {
                call.respond(HttpStatusCode.BadRequest, "Cal proporcionar un ID")
            }
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()

            if (id != null) {
                val resultado = activitatController.eliminarActividad(id)

                if (resultado) {
                    call.respond(HttpStatusCode.OK, "Actividad eliminada correctamente")
                } else {
                    call.respond(HttpStatusCode.NotFound, "Actividad no encontrada")
                }
            } else {
                call.respond(HttpStatusCode.BadRequest, "ID inválido")
            }
        }
        put ("/editar/{id}"){
            val id = call.parameters["id"]?.toIntOrNull()
            if (id != null) {
                val receivedText = call.receiveText()
                println("Dades rebudes: $receivedText")

                // Deserialitzar manualment el JSON
                val activitat = kotlinx.serialization.json.Json.decodeFromString<Activitat>(receivedText)
                println("Activitat deserialitzada: $activitat")

                val resultado = activitatController.modificarActivitat(
                    id = id,
                    nom = activitat.nom,
                    descripcio = activitat.descripcio,
                    ubicacio = activitat.ubicacio,
                    dataInici = activitat.dataInici,
                    dataFi = activitat.dataFi
                )

                if (resultado) {
                    call.respond(HttpStatusCode.OK, "Activitat modificada correctament")
                } else {
                    call.respond(HttpStatusCode.NotFound, "L'activitat no existeix")
                }
            } else {
                call.respond(HttpStatusCode.BadRequest, "Cal proporcionar un ID")
            }
        }
    }
    route("/favoritas") {
        // Check if an activity is a favorite
        get("/{id}/{username}") {
            val id = call.parameters["id"]?.toIntOrNull()
            val username = call.parameters["username"]

            if (id != null && !username.isNullOrBlank()) {
                val esFavorita = activitatController.comprovarActivitatFavorita(id, username)
                call.respond(HttpStatusCode.OK, mapOf("esFavorita" to esFavorita))
            } else {
                call.respond(HttpStatusCode.BadRequest, "ID or username is invalid")
            }
        }

        // Add an activity as a favorite
        post("/añadir") {
            try {
                val params = call.receive<Map<String, String>>()
                val id = params["id"]?.toIntOrNull()
                val username = params["username"]
                val dataAfegida = params["dataAfegida"]?.let { Timestamp.valueOf(it).toLocalDateTime() }

                if (id != null && !username.isNullOrBlank() && dataAfegida != null) {
                    val resultado = activitatController.afegirActivitatFavorita(id, username, dataAfegida.toKotlinLocalDateTime())
                    if (resultado) {
                        call.respond(HttpStatusCode.Created, "Activity added to favorites")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Activity not found")
                    }
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Invalid parameters")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Error processing the request")
            }
        }

        // Remove an activity from favorites
        delete("/eliminar") {
            try {
                val params = call.receive<Map<String, String>>()
                val id = params["id"]?.toIntOrNull()
                val username = params["username"]

                if (id != null && !username.isNullOrBlank()) {
                    val resultado = activitatController.eliminarActivitatFavorita(id, username)
                    if (resultado) {
                        call.respond(HttpStatusCode.OK, "Activity removed from favorites")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Activity not found or not a favorite")
                    }
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Invalid parameters")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Error processing the request")
            }
        }
    }
}