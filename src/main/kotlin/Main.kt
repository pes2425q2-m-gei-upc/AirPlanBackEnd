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
                allowCredentials = true
            }

            // Configuració de negociació de contingut
            install(ContentNegotiation) {
                json()
            }

            DatabaseFactory.init()
            val usuarioRepository = UsuarioRepository()
            val controladorUsuario = ControladorUsuarios(usuarioRepository)

            // Configuració de rutes
            routing {
                usuarioRoutes()
                activitatRoutes()
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
                        idioma = Idioma.Castellano,
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
                    }                }
            }
        }
    }

    // 🔹 Arranquem el servidor amb l'entorn configurat
    embeddedServer(Netty, environment).start(wait = true)
}
