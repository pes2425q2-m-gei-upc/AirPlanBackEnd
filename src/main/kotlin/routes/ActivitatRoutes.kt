package org.example.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import org.example.controllers.ControladorActivitat
import org.example.models.Activitat
import repositories.ActivitatRepository
import java.sql.Timestamp

fun Route.activitatRoutes() {
    val activitatController = ControladorActivitat(ActivitatRepository())
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
    }
}