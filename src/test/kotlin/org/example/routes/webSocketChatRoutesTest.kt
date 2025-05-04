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
import org.example.database.UserBlockTable
import org.example.database.UsuarioTable
import org.jetbrains.exposed.sql.insert

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebSocketChatRoutesTest {

    companion object {
        private lateinit var database: Database

        @BeforeAll
        @JvmStatic
        fun setupClass() {
            database = Database.connect(
                "jdbc:h2:mem:test_ws_${UUID.randomUUID()};DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                driver = "org.h2.Driver",
                user = "sa",
                password = ""
            )
            transaction(database) {
                SchemaUtils.create(MissatgesTable, UsuarioTable, UserBlockTable)
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
            SchemaUtils.drop(MissatgesTable, UsuarioTable, UserBlockTable)
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

        runBlocking {
            // Create both WebSocket sessions before sending any messages
            val session1Job = launch {
                client.webSocket("/ws/chat/user1/user2") {
                    // First receive and ignore the history message
                    val historyFrame = withTimeout(5000) { incoming.receive() }
                    assertTrue(historyFrame is Frame.Text)
                    println("Session 1: History received")

                    // Enviar mensaje
                    val testMessage = Missatge(
                        usernameSender = "user1",
                        usernameReceiver = "user2",
                        dataEnviament = LocalDateTime.parse("2024-05-01T12:00:00"),
                        missatge = "Hola desde test",
                        isEdited = false
                    )
                    send(Frame.Text(Json.encodeToString(testMessage)))
                    println("Session 1: Message sent")

                    // Keep connection open until explicitly cancelled
                    delay(10000)
                }
            }

            // Give some time for the first connection to establish
            delay(500)

            val session2Job = launch {
                client.webSocket("/ws/chat/user1/user2") {
                    // First receive the history message
                    val historyFrame = withTimeout(5000) { incoming.receive() }
                    assertTrue(historyFrame is Frame.Text)
                    println("Session 2: History received")

                    // Now receive the actual message
                    val receivedFrame = withTimeout(5000) { incoming.receive() }
                    assertTrue(receivedFrame is Frame.Text)
                    println("Session 2: Message received")

                    // Use ignoreUnknownKeys to handle any extra fields
                    val json = Json { ignoreUnknownKeys = true }
                    val receivedMessage = json.decodeFromString<Missatge>((receivedFrame as Frame.Text).readText())
                    assertEquals("Hola desde test", receivedMessage.missatge)
                }
            }

            // Wait for both sessions to complete their tasks
            try {
                withTimeout(15000) {
                    session2Job.join()  // Wait for session2 to complete first
                    session1Job.cancel() // Then cancel session1
                }
            } catch (e: Exception) {
                // Make sure to cancel the jobs in case of an exception
                session1Job.cancel()
                session2Job.cancel()
                throw e
            }
        }
    }

    @Test
    fun `test historial de mensajes al conectar`() = testApplication {
        // Create a separate function to insert test data
        insertTestMessage()

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

    // Add this helper method to insert test messages directly without using suspend functions
    private fun insertTestMessage() {
        transaction(database) {
            MissatgesTable.insert {
                it[usernameSender] = "user1"
                it[usernameReceiver] = "user2"
                it[dataEnviament] = LocalDateTime.parse("2024-05-01T10:00:00")
                it[missatge] = "Mensaje histórico"
                it[isEdited] = false
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
                assertFalse((response as Frame.Text).readText().contains("\"error\""))
            }
        }
    }
    @Test
    fun `test editar mensaje via WebSocket`() = testApplication {
        // Use current time for the test message to ensure it's within edit window
        val currentTime = java.time.LocalDateTime.now()
        val formattedTime = currentTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"))

        // Insert test message with current timestamp
        transaction(database) {
            MissatgesTable.insert {
                it[usernameSender] = "user1"
                it[usernameReceiver] = "user2"
                it[dataEnviament] = LocalDateTime.parse(formattedTime)
                it[missatge] = "Mensaje original"
                it[isEdited] = false
            }
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
                // First, receive history message
                val historyFrame = withTimeout(5000) { incoming.receive() }
                assertTrue(historyFrame is Frame.Text)

                // Send edit request using the working format
                val editRequest = """{"type":"EDIT",
                "usernameSender": "user1",
                "originalTimestamp": "$formattedTime",
                "newContent": "Mensaje editado"
            }""".trimIndent()

                send(Frame.Text(editRequest))

                // Wait for response
                val response = withTimeout(5000) { incoming.receive() }
                assertTrue(response is Frame.Text)
                val responseText = (response as Frame.Text).readText()

                // Debug output
                println("Response for successful edit: $responseText")

                // Check for success response
                assertTrue(responseText.contains("\"type\":\"EDIT\""), "Response should be an edit operation")
                assertTrue(responseText.contains("\"newContent\":\"Mensaje editado\""), "Response should have the new content")
                assertTrue(responseText.contains("\"isEdited\":true"), "Response should indicate message was edited")
            }
        }
    }
    @Test
    fun `test edicion de mensaje fallida - pasado tiempo limite`() = testApplication {
        // Insert an old message (beyond 20 minute limit)
        transaction(database) {
            MissatgesTable.insert {
                it[usernameSender] = "user1"
                it[usernameReceiver] = "user2"
                it[dataEnviament] = LocalDateTime.parse("2024-04-15T10:00:00")
                it[missatge] = "Mensaje antiguo"
                it[isEdited] = false
            }
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
                // First, receive history message
                val historyFrame = withTimeout(5000) { incoming.receive() }
                assertTrue(historyFrame is Frame.Text)

                // Send edit request in the exact same format as the working test
                val editRequest = """{"type":"EDIT", 
                "usernameSender": "user1",
                "originalTimestamp": "2024-04-15T10:00:00.000",
                "newContent": "Intento editar mensaje antiguo"
            }""".trimIndent()

                send(Frame.Text(editRequest))

                // Wait for response
                val errorResponse = withTimeout(5000) { incoming.receive() }
                assertTrue(errorResponse is Frame.Text)
                val responseText = (errorResponse as Frame.Text).readText()

                // Debug output
                println("Response for old message edit: $responseText")

                // Check both for error key and specific timeout message
                assertTrue(responseText.contains("\"error\""), "Response should contain an error")
                assertTrue(responseText.contains("No se puede editar mensajes después de 20 minutos"),
                    "Response should indicate message is too old to edit")
            }
        }
    }
}