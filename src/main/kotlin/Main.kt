package org.example
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.controllers.ControladorUsuarios
import org.example.enums.Idioma
import org.example.database.DatabaseFactory
import org.example.repositories.UsuarioRepository
import org.example.routes.activitatRoutes
import org.example.routes.usuarioRoutes
import org.example.routes.invitacioRoutes

// Eliminada la importación de authRoutes
import org.example.services.FirebaseAdminService
import io.ktor.server.http.content.*
import org.example.routes.*
// Eliminada la importación de java.io.File que ya no se utiliza
import org.example.routes.valoracioRoutes
import org.example.routes.generalRoutes


fun main() {

    // 🔹 Creem l'entorn del servidor amb SSL
    val environment = applicationEngineEnvironment {

        connector {
            port = 8080 // 🔹 Escoltem en HTTP al port 8080
        }

        module {
            // Instal·lar CORS
            install(CORS) {
                anyHost() // Permet sol·licituds de qualsevol origen (restringeix-ho en producció)
                allowMethod(HttpMethod.Get)
                allowMethod(HttpMethod.Post)
                allowMethod(HttpMethod.Delete)
                allowMethod(HttpMethod.Put)
                allowHeader(HttpHeaders.ContentType)
                allowNonSimpleContentTypes = true  // Allow WebSocket connections
                allowCredentials = true
            }


            // Configuració de negociació de contingut
            install(ContentNegotiation) {
                json()
            }

            // Configurar WebSockets
            configureWebSockets()

            DatabaseFactory.init()
            val usuarioRepository = UsuarioRepository()
            val controladorUsuario = ControladorUsuarios(usuarioRepository)

            // Inicializar Firebase Admin SDK al inicio
            FirebaseAdminService.initialize()

            // Configuració de rutes
            routing {
                usuarioRoutes()
                activitatRoutes()
                solicitudRoutes()
                invitacioRoutes()
                missatgeRoutes()
                websocketChatRoutes()
                valoracioRoutes()
                userBlockRoutes() // Añadir rutas de bloqueo de usuarios
                // Eliminada la llamada a uploadImageRoute()
                webSocketRoutes() // Registrar rutas WebSocket
                generalRoutes()

                // Eliminada la llamada a authRoutes()
                // Eliminada la configuración de ruta estática para archivos de imagen

                get("/") {
                    call.respond(
                        """
                        <html>
                            <head>
                                <title>Backend Status</title>
                                <style>
                                    body {
                                        font-family: Arial, sans-serif;
                                        background-color: #f4f4f4;
                                        display: flex;
                                        justify-content: center;
                                        align-items: center;
                                        height: 100vh;
                                        margin: 0;
                                    }
                                    .message {
                                        background-color: #fff;
                                        padding: 20px;
                                        border-radius: 8px;
                                        box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
                                        text-align: center;
                                    }
                                    .message h1 {
                                        color: #4CAF50;
                                    }
                                </style>
                            </head>
                            <body>
                                <div class="message">
                                    <h1>¡Backend Funciona Correctament!</h1>
                                    <p>El servidor backend està en execució i llest per rebre sol·licituds.</p>
                                </div>
                            </body>
                        </html>
                        """.trimIndent()
                    )
                }

                get("/api/data") {
                    call.respond(mapOf("message" to "Hello from Kotlin Backend"))
                }
                get("/users") {

                    // Crear un usuario de muestra
                    controladorUsuario.crearUsuario(
                        username = "usuario123",
                        nom = "Carlos Gómez",
                        email = "carlos.gomez@example.com",
                        idioma = Idioma.Castellano.toString(),
                        isAdmin = false
                    )

                    val usuarios = controladorUsuario.listarUsuarios()
                    call.respond(usuarios)
                }
                get("/check-username/{username}") {
                    val username = call.parameters["username"]
                    if (username != null && controladorUsuario.comprobarNombreUsuario(username)) {
                        call.respond(HttpStatusCode.Conflict, "El nombre de usuario ya está en uso")
                    } else {
                        call.respond(HttpStatusCode.OK, "El nombre de usuario está disponible")
                    }
                }
                get("/isAdmin/{email}") {
                    val email = call.parameters["email"] // Obtener el email de la URL
                    if (email != null) {
                        // Consultar si el usuario es administrador
                        val isAdmin = controladorUsuario.isUserAdmin(email)
                        call.respond(mapOf("isAdmin" to isAdmin))
                    } else {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Email no proporcionado"))
                    }
                }
            }
        }
    }

    // 🔹 Arranquem el servidor amb l'entorn configurat
    embeddedServer(Netty, environment).start(wait = true)
}
