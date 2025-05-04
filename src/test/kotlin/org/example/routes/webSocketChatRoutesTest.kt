package org.example.routes

import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.datetime.LocalDateTime
import org.example.database.MissatgesTable
import org.example.models.Missatge
import org.example.repositories.MissatgeRepository
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import java.util.*
import java.util.Collections.synchronizedSet
import kotlin.test.*
import kotlin.test.Test
import io.ktor.server.routing.*
import io.ktor.server.application.install
import io.ktor.server.application.Application
import io.ktor.server.websocket.*
import io.ktor.server.websocket.WebSockets

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebSocketChatRoutesTest {

    companion object {
        private lateinit var database: Database

        @BeforeAll
        @JvmStatic
        fun setupClass() {
            database = Database.connect(
                "jdbc:h2:mem:test_ws_${UUID.randomUUID()};DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                driver = "org.h2.Driver"
            )
            transaction(database) {
                SchemaUtils.create(MissatgesTable)
            }
        }
    }

    @BeforeEach
    fun setup() {
        transaction(database) {
            exec("TRUNCATE TABLE missatges")
        }
    }

    @AfterAll
    fun tearDownAll() {
        transaction(database) {
            SchemaUtils.drop(MissatgesTable)
        }
    }

    @Test
    fun `test conexion WebSocket exitosa`() = testApplication {
        application {
            install(WebSockets) {
                pingPeriod = java.time.Duration.ofSeconds(15)
                timeout = java.time.Duration.ofSeconds(15)
            }
            routing {
                websocketChatRoutes()
            }
        }

        val client = createClient {
            install(io.ktor.client.plugins.websocket.WebSockets) {
                pingInterval = 15000
            }
        }

        client.webSocket("/ws/chat/user1/user2") {
            assertNotNull(this)
        }
    }

    @Test
    fun `test envio y recepcion de mensajes`() = testApplication {
        // Configuración del servidor
        application {
            install(WebSockets) {
                pingPeriod = java.time.Duration.ofSeconds(15)
                timeout = java.time.Duration.ofSeconds(15)
            }
            routing {
                websocketChatRoutes()
            }
        }

        // Configuración del cliente
        val client = createClient {
            install(io.ktor.client.plugins.websocket.WebSockets) {
                pingInterval = 15000
            }
        }

        runBlocking {
            // Cliente 1
            val session1 = client.webSocket(
                method = HttpMethod.Get,
                path = "/ws/chat/user1/user2"
            ) {
                // Enviar mensaje
                val testMessage = Missatge(
                    usernameSender = "user1",
                    usernameReceiver = "user2",
                    dataEnviament = LocalDateTime.parse("2024-05-01T12:00:00"),
                    missatge = "Hola desde test"
                )
                send(Frame.Text(Json.encodeToString(testMessage)))
            }

            // Cliente 2
            client.webSocket(
                method = HttpMethod.Get,
                path = "/ws/chat/user1/user2"
            ) {
                // Recibir mensaje
                val receivedFrame = withTimeout(5000) { incoming.receive() }
                assertTrue(receivedFrame is Frame.Text)
                val receivedMessage = Json.decodeFromString<Missatge>((receivedFrame as Frame.Text).readText())
                assertEquals("Hola desde test", receivedMessage.missatge)
            }

            // Cerrar sesión
            (session1 as? DefaultClientWebSocketSession)?.close()
        }
    }

    @Test
    fun `test historial de mensajes al conectar`() = testApplication {
        transaction(database) {
            val repo = MissatgeRepository()
            repo.sendMessage(Missatge(
                "user1", "user2",
                LocalDateTime.parse("2024-05-01T10:00:00"),
                "Mensaje histórico"
            ))
        }

        application {
            install(WebSockets) {
                pingPeriod = java.time.Duration.ofSeconds(15)
                timeout = java.time.Duration.ofSeconds(15)
            }
            routing {
                websocketChatRoutes()
            }
        }

        val client = createClient {
            install(io.ktor.client.plugins.websocket.WebSockets)
        }

        runBlocking {
            client.webSocket("/ws/chat/user1/user2") {
                val firstFrame = withTimeout(5000) { incoming.receive() }
                assertTrue(firstFrame is Frame.Text)
                val response = (firstFrame as Frame.Text).readText()
                assertTrue(response.contains("\"type\":\"history\""))
                assertTrue(response.contains("Mensaje histórico"))
            }
        }
    }

    @Test
    fun `test manejo de mensaje PING`() = testApplication {
        application {
            install(WebSockets) {
                pingPeriod = java.time.Duration.ofSeconds(15)
                timeout = java.time.Duration.ofSeconds(15)
            }
            routing {
                websocketChatRoutes()
            }
        }

        val client = createClient {
            install(io.ktor.client.plugins.websocket.WebSockets)
        }

        runBlocking {
            client.webSocket("/ws/chat/user1/user2") {
                send(Frame.Text("{\"type\":\"PING\"}"))

                val response = withTimeout(5000) { incoming.receive() }
                assertTrue(response is Frame.Text)
                assertEquals("{\"type\":\"PONG\"}", (response as Frame.Text).readText())
            }
        }
    }

    @Test
    fun `test mensaje invalido`() = testApplication {
        application {
            install(WebSockets) {
                pingPeriod = java.time.Duration.ofSeconds(15)
                timeout = java.time.Duration.ofSeconds(15)
            }
            routing {
                websocketChatRoutes()
            }
        }

        val client = createClient {
            install(io.ktor.client.plugins.websocket.WebSockets)
        }

        runBlocking {
            client.webSocket("/ws/chat/user1/user2") {
                send(Frame.Text("esto no es un JSON válido"))

                val response = withTimeout(5000) { incoming.receive() }
                assertTrue(response is Frame.Text)
                assertTrue((response as Frame.Text).readText().contains("\"error\""))
            }
        }
    }
}