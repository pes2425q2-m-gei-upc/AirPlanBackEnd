package org.example.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.example.controllers.ControladorActivitat
import org.example.repositories.ParticipantsActivitatsRepository
import org.example.models.Activitat
import repositories.ActivitatFavoritaRepository
import repositories.ActivitatRepository
import org.example.repositories.UserBlockRepository
import java.sql.Timestamp

import ControladorValoracio
import ValoracioRepository
import kotlinx.serialization.json.*
import org.example.services.AirQualityService

fun Route.activitatRoutes() {
    val activitatRepository = ActivitatRepository()
    val activitatFavoritaRepository = ActivitatFavoritaRepository() // Create an instance of ActivitatFavoritaRepository
    val participantsActivitatsRepository = ParticipantsActivitatsRepository()
    val activitatController = ControladorActivitat(activitatRepository, participantsActivitatsRepository, activitatFavoritaRepository) // Pass both repositories
    val userBlockRepository = UserBlockRepository() // Añadir repositorio de bloqueos de usuarios
    val valoracioRepository = ValoracioRepository() // Añadir repositorio de valoraciones

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

        //Obtenir participants de una activitat
        get("/{activityId}/participants") {
            val activityId = call.parameters["activityId"]?.toIntOrNull()
            if (activityId == null) {
                call.respond(HttpStatusCode.BadRequest, "ID d'activitat invàlid")
                return@get
            }

            val participants = activitatController.obtenirParticipantsDeActivitat(activityId)
            call.respond(participants)
        }

        //Borra usuario de actividad
        delete("{id}/participants/{username}") {
            val idActivitat = call.parameters["id"]?.toIntOrNull()
            val usernameAEliminar = call.parameters["username"]

            if (idActivitat == null || usernameAEliminar.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Paràmetres invàlids.")
                return@delete
            }

            val eliminat = activitatController.eliminarParticipant(idActivitat, usernameAEliminar)
            if (eliminat) {
                call.respond(HttpStatusCode.OK, "Participant eliminat correctament.")
            } else {
                call.respond(HttpStatusCode.NotFound, "No s'ha trobat el participant.")
            }
        }
        get ("/participant/{username}") {
            val username = call.parameters["username"]
            if (username != null) {
                val activitats = activitatController.obtenirActivitatsPerParticipant(username)
                for (activitat in activitats) {
                    println("${activitat.nom} - ${activitat.descripcio} - ${activitat.ubicacio.latitud} -${activitat.ubicacio.longitud}  - ${activitat.dataInici} - ${activitat.dataFi} - ${activitat.creador}")
                }
                call.respond(HttpStatusCode.OK, activitats)
            } else {
                call.respond(HttpStatusCode.BadRequest, "Username is required")
            }
        }

        get("/hoy") {
            try {
                // Get activities that start today
                val activitiesToday = activitatController.obtenirActivitatsStartingToday()

                // Enhanced response list to hold complete activity data
                val enhancedActivities = mutableListOf<JsonObject>()

                // Fetch air quality data once for all activities and cache it - now using the service
                val airQualityData = AirQualityService.fetchAirQualityData()

                // Process each activity
                for (activitat in activitiesToday) {
                    // Get participants for this activity
                    val participants = activitatController.obtenirParticipantsDeActivitat(activitat.id)

                    // Get ratings for this activity
                    val ratings = valoracioRepository.obtenirValoracionsPerActivitat(activitat.id)

                    // Find the closest air quality station data for this activity location - now using the service
                    val airQuality = AirQualityService.findClosestAirQualityData(activitat.ubicacio.latitud, activitat.ubicacio.longitud, airQualityData)

                    // Build enhanced activity object with all information
                    val enhancedActivity = buildJsonObject {
                        put("id", activitat.id)
                        put("nom", activitat.nom)
                        put("descripcio", activitat.descripcio)
                        put("ubicacio", buildJsonObject {
                            put("latitud", activitat.ubicacio.latitud)
                            put("longitud", activitat.ubicacio.longitud)
                        })
                        put("dataInici", activitat.dataInici.toString())
                        put("dataFi", activitat.dataFi.toString())
                        put("creador", activitat.creador)
                        put("participants", JsonArray(participants.map { JsonPrimitive(it) }))

                        // Add ratings as array
                        put("valoracions", JsonArray(ratings.map { rating ->
                            buildJsonObject {
                                put("username", rating.username)
                                put("valoracion", rating.valoracion)
                                put("comentario", rating.comentario?.let { JsonPrimitive(it) } ?: JsonNull)
                                put("fechaValoracion", rating.fechaValoracion.toString())
                            }
                        }))

                        // Add air quality information
                        put("qualityAire", airQuality)
                    }

                    enhancedActivities.add(enhancedActivity)
                }

                // Return the enhanced activity list
                call.respond(HttpStatusCode.OK, enhancedActivities)
            } catch (e: Exception) {
                println("Error getting activities for today: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, "Error getting activities for today: ${e.message}")
            }
        }
    }
}