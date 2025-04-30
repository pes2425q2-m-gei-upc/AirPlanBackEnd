package org.example.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.example.controllers.ControladorActivitat
import org.example.models.Activitat
import repositories.ActivitatFavoritaRepository
import repositories.ActivitatRepository
import org.example.repositories.UserBlockRepository
import java.sql.Timestamp

fun Route.activitatRoutes() {
    val activitatRepository = ActivitatRepository()
    val activitatFavoritaRepository = ActivitatFavoritaRepository() // Create an instance of ActivitatFavoritaRepository
    val activitatController = ControladorActivitat(activitatRepository, activitatFavoritaRepository) // Pass both repositories
    val userBlockRepository = UserBlockRepository() // Añadir repositorio de bloqueos de usuarios

    println("Ha arribat a ActivitatRoutes")  // Depuració
    route("/api/activitats") {
        println("Ha arribat a /api/activitats")  // Depuració

        // Endpoint para filtrar actividades según usuarios bloqueados
        get("/filter/{username}") {
            try {
                val username = call.parameters["username"]
                
                if (username != null) {
                    // Obtener actividades excluyendo las de usuarios bloqueados en una única consulta SQL
                    val filteredActivities = activitatController.obtenirActivitatsPerUsuariSenseBloquejos(username)
                    
                    call.respond(HttpStatusCode.OK, filteredActivities)
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Username is required")
                }
            } catch (e: Exception) {
                println("Error filtrando actividades: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, "Error filtering activities: ${e.message}")
            }
        }

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
        put("/editar/{id}") {
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
        // Check if an activity is a favorite
        get("/favorita/{id}/{username}") {
            val id = call.parameters["id"]?.toInt()
            val username = call.parameters["username"]

            if (id != null && !username.isNullOrBlank()) {
                val esFavorita = activitatController.comprovarActivitatFavorita(id, username)
                call.respond(HttpStatusCode.OK, mapOf("esFavorita" to esFavorita))
            } else {
                call.respond(HttpStatusCode.BadRequest, "ID or username is invalid")
            }
        }

        // Add an activity as a favorite
        post("/favorita/anadir/{id}/{username}") {
            try {
                val id = call.parameters["id"]?.toInt()
                val username = call.parameters["username"]
                val dataAfegida = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())

                if (id != null && !username.isNullOrBlank()) {
                    val resultado = activitatController.afegirActivitatFavorita(id, username, dataAfegida)
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
        delete("/favorita/eliminar/{id}/{username}") {
            try {
                val id = call.parameters["id"]?.toInt()
                val username = call.parameters["username"]

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

        // Get all favorite activities for a user
        get("/favoritas/{username}") {
            val username = call.parameters["username"]

            if (!username.isNullOrBlank()) {
                val activitatsFavorites = activitatController.obtenirActivitatsFavoritesPerUsuari(username)
                call.respond(HttpStatusCode.OK, activitatsFavorites)
            } else {
                call.respond(HttpStatusCode.BadRequest, "Username is invalid")
            }
        }
    }
}