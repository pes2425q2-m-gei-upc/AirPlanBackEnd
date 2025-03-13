import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.get

fun main() {
    try {
        embeddedServer(Netty, port = 8080) {
            // Instalar CORS
            install(CORS) {
                anyHost() // Permite solicitudes de cualquier origen (puedes restringirlo en producción)
                allowMethod(HttpMethod.Get)
                allowMethod(HttpMethod.Post)
                allowHeader(HttpHeaders.ContentType) // Permitir peticiones con tipo de contenido
                allowCredentials = true // Permitir el envío de cookies y credenciales
            }

            // Configuración de negociación de contenido
            install(ContentNegotiation) {
                json() // Usar JSON como formato de respuesta
            }

            // Configuración de rutas
            routing {
                get("/") {
                    call.respond("Hello, Ktor!")
                }

                // Ejemplo de endpoint API
                get("/api/data") {
                    call.respond(mapOf("message" to "Hello from Kotlin Backend"))
                }
            }
        }.start(wait = true)
    } catch (e: Exception) {
        println("Error al iniciar el servidor: ${e.message}")
    }
}