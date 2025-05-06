package org.example.routes

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


fun Route.generalRoutes() {
    route("/api/locations") {
        get  {
            val format = call.request.queryParameters["format"] ?: "json"
            val lat = call.request.queryParameters["lat"]
            val lon = call.request.queryParameters["lon"]

            val endpoint = "https://nominatim.openstreetmap.org/reverse?format=${format}&lat=${lat}&lon=${lon}"
            val client = HttpClient(CIO)
            try {
                val response = client.get(endpoint)
                val responseBody = response.bodyAsText()
                call.respond(HttpStatusCode.OK,responseBody)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error al obtenir la informació de la localització: ${e.message}")
            }
            client.close()
        }
    }

    route ("/api/airquality") {
        get {
            print("Obtenint la qualitat de l'aire")
            val data = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString().substring(0,10)

            val endpoint = "https://analisi.transparenciacatalunya.cat/resource/tasf-thgu.json?data=${data}"
            val client = HttpClient(CIO)
            try {
                val response = client.get(endpoint)
                val responseBody = response.bodyAsText()
                call.respond(HttpStatusCode.OK,responseBody)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error al obtenir la informació de la qualitat de l'aire: ${e.message}")
            }
            client.close()
        }
    }
}
