package org.example.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.client.*
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.example.controllers.ControladorRuta
import org.example.repositories.RutaRepository
import java.util.Properties
import java.io.File
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.*

@Serializable
data class RouteRequest(
    val coordinates: List<List<Double>>,
    val instructions: Boolean,
    val language: String,
)

val properties = Properties().apply {
    load(File("secrets.properties").inputStream())
}

val hereApiKey = properties.getProperty("HERE_API_KEY")
val orsApiKey = properties.getProperty("ORS_API_KEY")

fun Route.rutaRoutes(rutaController: ControladorRuta) {

    route("/api/rutas") {
        post {
            try {
                val jsonString = call.receiveText() // Get raw JSON string
                val rutaJson = Json.parseToJsonElement(jsonString).jsonObject
                val createdRuta = rutaController.crearRuta(rutaJson) // Pass it to the controller
                call.respond(HttpStatusCode.Created, createdRuta.id) // Respond with the created Ruta
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Error al crear la ruta: ${e.message}")
            }
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id != null) {
                val result = rutaController.eliminarRuta(id)
                if (result) {
                    call.respond(HttpStatusCode.OK, "Ruta eliminada correctament")
                } else {
                    call.respond(HttpStatusCode.NotFound, "Ruta no trobada")
                }
            } else {
                call.respond(HttpStatusCode.BadRequest, "ID invàlid")
            }
        }

        get {
            try {
                val clientUsername = call.request.queryParameters["username"]
                val rutas = rutaController.obtenirTotesRutesClient(clientUsername.toString())
                call.respond(HttpStatusCode.OK, rutas)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error al obtenir les rutes: ${e.message}")
            }

        }

        get ("/calculate/publictransport") {
            val origen = call.request.queryParameters["origin"]
            val desti = call.request.queryParameters["destination"]
            val departureTime = call.request.queryParameters["departureTime"] ?: "no_departure"
            val arrivalTime = call.request.queryParameters["arrivalTime"] ?: "no_arrival"
            val returnValue = call.request.queryParameters["return"] ?: "no_return_value"
            val lang = call.request.queryParameters["lang"] ?: "ca"

            if (origen != null && desti != null) {
                val client = HttpClient(CIO)
                try {
                    val response = client.get("https://transit.router.hereapi.com/v8/routes") {
                        parameter("origin", origen)
                        parameter("destination", desti)
                        if (departureTime != "no_departure") parameter("departureTime", departureTime)
                        if (arrivalTime != "no_arrival") parameter("arrivalTime", arrivalTime)
                        if (returnValue != "no_return_value") parameter("return", returnValue)
                        parameter("lang", lang)
                        parameter("apikey", hereApiKey)
                    }
                    val responseBody = response.bodyAsText()
                    call.respond(HttpStatusCode.OK, responseBody)
                } finally {
                    client.close()
                }
            } else {
                call.respond(HttpStatusCode.BadRequest, "Paràmetres invàlids")
            }
        }

        get("calculate/simple") {
            val profile = call.request.queryParameters["profile"] ?: "foot-walking"
            val origen = call.request.queryParameters["origin"]
            val desti = call.request.queryParameters["destination"]
            val language = call.request.queryParameters["language"] ?: "es-es"

            if (origen != null && desti != null) {
                val client = HttpClient(CIO) {
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true })
                    }
                }
                try {

                    val apiKey = orsApiKey
                    val endpoint = "https://api.openrouteservice.org/v2/directions/$profile"

                    val requestBody = RouteRequest(
                        coordinates = listOf(
                            listOf(origen.split(",")[1].toDouble(), origen.split(",")[0].toDouble()),
                            listOf(desti.split(",")[1].toDouble(), desti.split(",")[0].toDouble())
                        ),
                        instructions = true,
                        language = language
                    )

                    val response: HttpResponse = client.post(endpoint) {
                        headers {
                            append(HttpHeaders.Authorization, apiKey)
                            append(HttpHeaders.ContentType, ContentType.Application.Json)
                        }
                        setBody(requestBody)
                    }
                    val responseBody = response.bodyAsText()
                    call.respond(HttpStatusCode.OK, responseBody)
                } finally {
                    client.close()
                }
            } else {
                call.respond(HttpStatusCode.BadRequest, "Paràmetres invàlids")
            }
        }
    }
}
