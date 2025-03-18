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
import java.io.File
import java.security.KeyStore


fun main() {
    val keyStoreFile = File("keystore.jks")
    val keyStorePassword = "airplan"
    val privateKeyPassword = "airplan"

    val keyStore = KeyStore.getInstance("JKS").apply {
        load(keyStoreFile.inputStream(), keyStorePassword.toCharArray())
    }

    // 🔹 Creem l'entorn del servidor amb SSL
    val environment = applicationEngineEnvironment {
        /*sslConnector(
            keyStore = keyStore,
            keyAlias = "AirPlan",
            keyStorePassword = { keyStorePassword.toCharArray() },
            privateKeyPassword = { privateKeyPassword.toCharArray() }
        ) {
            port = 8080 // 🔹 Només escoltem en HTTPS al port 8080
            keyStorePath = keyStoreFile
        }*/

        connector {
            port = 8080 // 🔹 Escoltem en HTTP al port 8080
        }

        module {
            // Instal·lar CORS
            install(CORS) {
                anyHost() // Permet sol·licituds de qualsevol origen (restringeix-ho en producció)
                allowMethod(HttpMethod.Get)
                allowMethod(HttpMethod.Post)
                allowHeader(HttpHeaders.ContentType)
                allowCredentials = true
            }

            // Configuració de negociació de contingut
            install(ContentNegotiation) {
                json()
            }

            // Configuració de rutes
            routing {
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
                get("/api/users") {
                    val controladorUsuario = ControladorUsuarios()

                    // Crear un usuario de muestra
                    controladorUsuario.crearUsuario(
                        username = "usuario123",
                        nom = "Carlos Gómez",
                        email = "carlos.gomez@example.com",
                        contrasena = "contraseñaSegura123",
                        idioma = Idioma.Castellano,
                        isAdmin = false
                    )

                    val usuarios = controladorUsuario.listarUsuarios()
                    call.respond(usuarios)
                }
            }
        }
    }

    // 🔹 Arranquem el servidor amb l'entorn configurat
    embeddedServer(Netty, environment).start(wait = true)
}
