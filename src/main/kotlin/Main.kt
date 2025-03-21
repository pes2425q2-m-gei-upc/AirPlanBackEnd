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
import org.example.database.DatabaseFactory
import org.example.routes.activitatRoutes
import repositories.ActivitatRepository
import java.io.File
import java.security.KeyStore

fun main() {
    val keyStoreFile = File("keystore.jks")
    val keyStorePassword = "airplan"
    val privateKeyPassword = "airplan"

    val keyStore = KeyStore.getInstance("JKS").apply {
        load(keyStoreFile.inputStream(), keyStorePassword.toCharArray())
    }

    // Create the server environment with SSL
    val environment = applicationEngineEnvironment {
        /*sslConnector(
            keyStore = keyStore,
            keyAlias = "AirPlan",
            keyStorePassword = { keyStorePassword.toCharArray() },
            privateKeyPassword = { privateKeyPassword.toCharArray() }
        ) {
            port = 8080 // Listen on HTTPS port 8080
            keyStorePath = keyStoreFile
        }*/

        connector {
            port = 8080 // Listen on HTTP port 8080
        }

        module {
            // Install CORS
            install(CORS) {
                anyHost() // Allow requests from any origin (restrict in production)
                allowMethod(HttpMethod.Get)
                allowMethod(HttpMethod.Post)
                allowMethod(HttpMethod.Delete)
                allowMethod(HttpMethod.Put)
                allowHeader(HttpHeaders.ContentType)
                allowCredentials = true
            }

            // Content negotiation configuration
            install(ContentNegotiation) {
                json()
            }

            DatabaseFactory.init()

            // Route configuration
            routing {
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
                get("/activitats") {
                    val activitatRepository = ActivitatRepository()
                    val activitats = activitatRepository.obtenirActivitats()
                    call.respond(activitats)
                }
            }
        }
    }

    // Start the server with the configured environment
    embeddedServer(Netty, environment).start(wait = true)
}