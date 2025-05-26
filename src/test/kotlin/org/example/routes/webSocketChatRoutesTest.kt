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
import io.mockk.*
import org.example.services.PerspectiveService
import org.example.controllers.UserBlockController
import org.example.repositories.UserBlockRepository
import kotlinx.serialization.json.*

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
    }    @Test
    fun `test envio de mensaje inapropiado bloqueado`() = testApplication {
        // Mockeamos PerspectiveService para simular detección de contenido inapropiado
        val mockPerspectiveService = mockk<PerspectiveService>()
        // El mensaje "contenido inapropiado" será bloqueado
        coEvery { mockPerspectiveService.analyzeMessage("contenido inapropiado") } returns true
        // Cualquier otro mensaje pasará la verificación
        coEvery { mockPerspectiveService.analyzeMessage(not(eq("contenido inapropiado"))) } returns false

        application {
            install(WebSockets) {
                pingPeriod = java.time.Duration.ofSeconds(15)
                timeout = java.time.Duration.ofSeconds(15)
            }
            routing {
                // Redefinimos la ruta websocket para usar nuestro mock de PerspectiveService
                webSocket("/ws/chat/{user1}/{user2}") {
                    val user1 = call.parameters["user1"] ?: return@webSocket
                    val user2 = call.parameters["user2"] ?: return@webSocket
                    val chatRoomId = listOf(user1, user2).sorted().joinToString("_")
                    
                    val repo = MissatgeRepository()
                    val blockRepository = UserBlockRepository()
                    val blockController = UserBlockController(blockRepository)
                    
                    // Agregamos esta sesión al chat
                    val sessionsInRoom = chatSessions.computeIfAbsent(chatRoomId) {
                        synchronizedSet(mutableSetOf<WebSocketSession>())
                    }
                    sessionsInRoom.add(this)
                    
                    // Enviar historial inicialmente
                    val messages = repo.getMessagesBetweenUsers(user1, user2)
                    val blockData = mapOf(
                        "user1BlockedUser2" to false,
                        "user2BlockedUser1" to false
                    )
                    val historyJson = Json.encodeToString(messages)
                    val blockJson = Json.encodeToString(blockData)
                    send(Frame.Text("{\"type\":\"history\", \"messages\":$historyJson, \"blockStatus\":$blockJson}"))
                    
                    try {
                        // Procesar mensajes entrantes en suspending context
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                
                                try {
                                    if (text.contains("\"type\":\"EDIT\"")) {
                                        // Manejar edición de mensajes
                                        val editData = Json.decodeFromString<Map<String, String>>(text)
                                        val sender = editData["usernameSender"]
                                        val originalTimestamp = editData["originalTimestamp"]
                                        val newContent = editData["newContent"]
                                        
                                        if (sender == null || originalTimestamp == null || newContent == null) {
                                            send(Frame.Text("{\"error\": \"Datos de edición incompletos\"}"))
                                            continue
                                        }
                                        
                                        if (mockPerspectiveService.analyzeMessage(newContent)) {
                                            send(Frame.Text("{\"type\":\"ERROR\", \"message\":\"El contenido editado fue bloqueado por ser inapropiado.\"}"))
                                            continue
                                        }
                                        
                                        // Procesar la edición como normal
                                        val success = repo.editMessage(sender, originalTimestamp, newContent)
                                        if (success) {
                                            val editResponse = buildJsonObject {
                                                put("type", "EDIT")
                                                put("usernameSender", sender)
                                                put("originalTimestamp", originalTimestamp)
                                                put("newContent", newContent)
                                                put("isEdited", true)
                                            }
                                            
                                            sessionsInRoom.forEach { session ->
                                                session.send(Frame.Text(editResponse.toString()))
                                            }
                                        }
                                    } else if (!text.contains("\"type\":")) {
                                        // Mensaje regular
                                        val missatgeObj = Json.decodeFromString<Missatge>(text)
                                        
                                        // Verificar contenido inapropiado con el mock
                                        if (mockPerspectiveService.analyzeMessage(missatgeObj.missatge)) {
                                            send(Frame.Text("{\"type\":\"ERROR\", \"message\":\"Tu mensaje fue bloqueado por contenido inapropiado.\"}"))
                                            continue
                                        }
                                        
                                        // Si llega aquí, no es inapropiado
                                        repo.sendMessage(
                                            missatgeObj,
                                            notify = { message ->
                                                val messageJson = Json.encodeToString(message)
                                                sessionsInRoom.forEach { session ->
                                                    session.send(Frame.Text(messageJson))
                                                }
                                            }
                                        )
                                        val messageJson = Json.encodeToString(missatgeObj)
                                        sessionsInRoom.forEach { session ->
                                            session.send(Frame.Text(messageJson))
                                        }
                                    }
                                } catch (e: Exception) {
                                    send(Frame.Text("{\"error\": \"Error al procesar el mensaje\"}"))
                                }
                            }
                        }
                    } finally {
                        sessionsInRoom.remove(this)
                    }
                }
            }
        }

        val client = createClient {
            install(io.ktor.client.plugins.websocket.WebSockets) {
                pingInterval = 15000
            }
        }

        runBlocking {
            client.webSocket("/ws/chat/user1/user2") {
                // Primero recibir el mensaje de historial
                val historyFrame = withTimeout(5000) { incoming.receive() }
                assertTrue(historyFrame is Frame.Text)
                
                // Enviar un mensaje inapropiado
                val inappropriateMessage = Missatge(
                    usernameSender = "user1",
                    usernameReceiver = "user2",
                    dataEnviament = LocalDateTime.parse("2024-05-01T12:00:00"),
                    missatge = "contenido inapropiado",
                    isEdited = false
                )
                send(Frame.Text(Json.encodeToString(inappropriateMessage)))
                
                // Esperar la respuesta de error
                val response = withTimeout(5000) { incoming.receive() }
                assertTrue(response is Frame.Text)
                val responseText = (response as Frame.Text).readText()
                
                // Verificar que se recibió el mensaje de error por contenido inapropiado
                assertTrue(responseText.contains("\"type\":\"ERROR\""))
                assertTrue(responseText.contains("contenido inapropiado"))
                
                // Ahora enviar un mensaje apropiado
                val appropriateMessage = Missatge(
                    usernameSender = "user1",
                    usernameReceiver = "user2",
                    dataEnviament = LocalDateTime.parse("2024-05-01T12:01:00"),
                    missatge = "mensaje normal",
                    isEdited = false
                )
                send(Frame.Text(Json.encodeToString(appropriateMessage)))
                
                // Esperar la confirmación (el mensaje se reenvía al emisor también)
                val confirmationResponse = withTimeout(5000) { incoming.receive() }
                assertTrue(confirmationResponse is Frame.Text)
                val confirmationText = (confirmationResponse as Frame.Text).readText()
                
                // Verificar que el mensaje apropiado pasó la verificación
                assertTrue(confirmationText.contains("\"missatge\":\"mensaje normal\""))
            }
        }
        
        // Verificar que se llamó al servicio de Perspective para ambos mensajes
        coVerify(exactly = 1) { mockPerspectiveService.analyzeMessage("contenido inapropiado") }
        coVerify(exactly = 1) { mockPerspectiveService.analyzeMessage("mensaje normal") }
    }    @Test
    fun `test edicion de mensaje con contenido inapropiado bloqueado`() = testApplication {
        // Insertar un mensaje reciente para poder editarlo
        val currentTime = java.time.LocalDateTime.now()
        val formattedTime = currentTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"))
        
        transaction(database) {
            MissatgesTable.insert {
                it[usernameSender] = "user1"
                it[usernameReceiver] = "user2"
                it[dataEnviament] = LocalDateTime.parse(formattedTime)
                it[missatge] = "Mensaje original apropiado"
                it[isEdited] = false
            }
        }
        
        // Mockeamos PerspectiveService para simular detección de contenido inapropiado
        val mockPerspectiveService = mockk<PerspectiveService>()
        // El mensaje con "inapropiado" será bloqueado durante la edición
        coEvery { mockPerspectiveService.analyzeMessage(match { it.contains("inapropiado") }) } returns true
        // Cualquier otro contenido pasará la verificación
        coEvery { mockPerspectiveService.analyzeMessage(not(match { it.contains("inapropiado") })) } returns false

        application {
            install(WebSockets) {
                pingPeriod = java.time.Duration.ofSeconds(15)
                timeout = java.time.Duration.ofSeconds(15)
            }
            routing {
                // Redefinimos la ruta websocket para usar nuestro mock de PerspectiveService
                webSocket("/ws/chat/{user1}/{user2}") {
                    val user1 = call.parameters["user1"] ?: return@webSocket
                    val user2 = call.parameters["user2"] ?: return@webSocket
                    val chatRoomId = listOf(user1, user2).sorted().joinToString("_")
                    
                    val repo = MissatgeRepository()
                    val blockRepository = UserBlockRepository()
                    val blockController = UserBlockController(blockRepository)
                    
                    // Agregamos esta sesión al chat
                    val sessionsInRoom = chatSessions.computeIfAbsent(chatRoomId) {
                        synchronizedSet(mutableSetOf<WebSocketSession>())
                    }
                    sessionsInRoom.add(this)
                    
                    // Enviar historial inicialmente
                    val messages = repo.getMessagesBetweenUsers(user1, user2)
                    val blockData = mapOf(
                        "user1BlockedUser2" to false,
                        "user2BlockedUser1" to false
                    )
                    val historyJson = Json.encodeToString(messages)
                    val blockJson = Json.encodeToString(blockData)
                    send(Frame.Text("{\"type\":\"history\", \"messages\":$historyJson, \"blockStatus\":$blockJson}"))
                    
                    try {
                        // Procesar mensajes entrantes en suspending context
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                
                                try {                                    if (text.contains("\"type\":\"EDIT\"")) {
                                        try {
                                            // Manejar edición de mensajes
                                            val editData = Json.decodeFromString<Map<String, String>>(text)
                                            val sender = editData["usernameSender"]
                                            val originalTimestamp = editData["originalTimestamp"]
                                            val newContent = editData["newContent"]
                                            
                                            if (sender == null || originalTimestamp == null || newContent == null) {
                                                send(Frame.Text("{\"error\": \"Datos de edición incompletos\"}"))
                                                continue
                                            }
                                            
                                            // Verificar si el mensaje es editable por tiempo
                                            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                                            val originalDateTime = java.time.LocalDateTime.parse(originalTimestamp, formatter)
                                                .atZone(java.time.ZoneId.systemDefault()).toInstant()
                                            val currentDateTime = java.time.Instant.now()
                                            val diffMinutes = java.time.temporal.ChronoUnit.MINUTES.between(originalDateTime, currentDateTime)
                                            
                                            if (diffMinutes > 20) {
                                                send(Frame.Text("{\"error\": \"No se puede editar mensajes después de 20 minutos\"}"))
                                                continue
                                            }
                                            
                                            // Verificar contenido inapropiado con el mock
                                            if (mockPerspectiveService.analyzeMessage(newContent)) {
                                                send(Frame.Text("{\"type\":\"ERROR\", \"message\":\"El contenido editado fue bloqueado por ser inapropiado.\"}"))
                                                continue
                                            }
                                            
                                            // Si llega aquí, la edición es válida
                                            val success = repo.editMessage(sender, originalTimestamp, newContent)
                                            if (success) {
                                                // Create edit response JSON
                                                val editResponse = buildJsonObject {
                                                    put("type", "EDIT")
                                                    put("usernameSender", sender)
                                                    put("originalTimestamp", originalTimestamp)
                                                    put("newContent", newContent)
                                                    put("isEdited", true)
                                                }
                                                
                                                val editJson = editResponse.toString()
                                                println("Enviando respuesta de edición exitosa: $editJson")
                                                
                                                // Send to all sessions including the current one first
                                                send(Frame.Text(editJson))
                                                
                                                // Then send to other sessions
                                                sessionsInRoom.forEach { otherSession ->
                                                    if (otherSession != this) {
                                                        try {
                                                            otherSession.send(Frame.Text(editJson))
                                                        } catch (e: Exception) {
                                                            println("Error al enviar edición a otra sesión: ${e.message}")
                                                        }
                                                    }
                                                }
                                            } else {
                                                send(Frame.Text("{\"error\": \"No se pudo editar el mensaje\"}"))
                                            }
                                        } catch (e: Exception) {
                                            println("Error procesando edición: ${e.message}")
                                            println("Texto recibido: $text")
                                            send(Frame.Text("{\"error\": \"Error al procesar el mensaje: ${e.message}\"}"))
                                        }
                                    }
                                } catch (e: Exception) {
                                    send(Frame.Text("{\"error\": \"Error al procesar el mensaje\"}"))
                                }
                            }
                        }
                    } finally {
                        sessionsInRoom.remove(this)
                    }
                }
            }
        }

        val client = createClient {
            install(io.ktor.client.plugins.websocket.WebSockets)
        }

        runBlocking {
            client.webSocket("/ws/chat/user1/user2") {
                // Primero recibir el mensaje de historial
                val historyFrame = withTimeout(5000) { incoming.receive() }
                assertTrue(historyFrame is Frame.Text)
                  // Intentar editar el mensaje con contenido inapropiado
                val editRequestInappropriate = """{"type":"EDIT",
                "usernameSender": "user1",
                "originalTimestamp": "$formattedTime",
                "newContent": "Contenido inapropiado editado"
                }""".trimIndent()
                
                println("Enviando edición con contenido inapropiado")
                send(Frame.Text(editRequestInappropriate))
                
                // Esperar la respuesta de error por contenido inapropiado
                val errorResponse = withTimeout(10000) { incoming.receive() }
                assertTrue(errorResponse is Frame.Text, "La respuesta no es de tipo Frame.Text")
                val errorText = (errorResponse as Frame.Text).readText()
                
                // Para depuración
                println("Respuesta para edición inapropiada: $errorText")
                
                // Verificar que se recibió el mensaje de error por contenido inapropiado
                assertTrue(errorText.contains("\"type\":\"ERROR\""), "La respuesta no contiene tipo ERROR")
                assertTrue(errorText.contains("inapropiado"), "La respuesta no menciona contenido inapropiado")
                
                // Ahora enviar una edición apropiada
                val editRequestAppropriate = """{"type":"EDIT",
                "usernameSender": "user1",
                "originalTimestamp": "$formattedTime",
                "newContent": "Contenido editado apropiado"
                }""".trimIndent()
                
                println("Enviando edición con contenido apropiado")
                send(Frame.Text(editRequestAppropriate))// Esperar confirmación de edición exitosa
                val successResponse = withTimeout(10000) { incoming.receive() }
                assertTrue(successResponse is Frame.Text, "La respuesta no es de tipo Frame.Text")
                val successText = (successResponse as Frame.Text).readText()
                
                // Para depuración, mostrar el contenido real
                println("Respuesta recibida para edición apropiada: $successText")
                println("Verificando que contiene type:EDIT - " + successText.contains("\"type\":\"EDIT\""))
                println("Verificando que contiene newContent:Contenido editado apropiado - " + successText.contains("\"newContent\":\"Contenido editado apropiado\""))
                println("Verificando que contiene isEdited:true - " + successText.contains("\"isEdited\":true"))
                
                // Verificar que la edición apropiada pasó la verificación
                // Usando una estructura try-catch para capturar cualquier fallo de aserción y ver más detalles
                try {
                    assertTrue(successText.contains("\"type\":\"EDIT\""), "El mensaje de respuesta no contiene 'type':'EDIT'")
                    assertTrue(successText.contains("\"newContent\":\"Contenido editado apropiado\""), "El mensaje no contiene el nuevo contenido esperado")
                    assertTrue(successText.contains("\"isEdited\":true"), "El mensaje no indica que fue editado")
                } catch (e: AssertionError) {
                    println("FALLO DE ASERCIÓN: ${e.message}")
                    println("Contenido completo de la respuesta: $successText")
                    throw e
                }
            }
        }
        
        // Verificar que se llamó al servicio de Perspective para ambos intentos de edición
        coVerify(exactly = 1) { mockPerspectiveService.analyzeMessage("Contenido inapropiado editado") }
        coVerify(exactly = 1) { mockPerspectiveService.analyzeMessage("Contenido editado apropiado") }
    }

    // Función auxiliar para manejar edición de mensajes (simplificada para tests)
    private suspend fun handleEditMessage(text: String, repo: MissatgeRepository, perspectiveService: PerspectiveService, chatRoomId: String, session: WebSocketSession) {
        val editData = Json.decodeFromString<Map<String, String>>(text)
        val sender = editData["usernameSender"]
        val originalTimestamp = editData["originalTimestamp"]
        val newContent = editData["newContent"]

        if (sender == null || originalTimestamp == null || newContent == null) {
            session.send(Frame.Text("{\"error\": \"Datos de edición incompletos\"}"))
            return
        }

        // Verificar si el mensaje es editable por tiempo
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
        val originalDateTime = java.time.LocalDateTime.parse(originalTimestamp, formatter)
            .atZone(java.time.ZoneId.systemDefault()).toInstant()
        val currentDateTime = java.time.Instant.now()
        val diffMinutes = java.time.temporal.ChronoUnit.MINUTES.between(originalDateTime, currentDateTime)
        
        if (diffMinutes > 20) {
            session.send(Frame.Text("{\"error\": \"No se puede editar mensajes después de 20 minutos\"}"))
            return
        }

        if (perspectiveService.analyzeMessage(newContent)) {
            session.send(Frame.Text("{\"type\":\"ERROR\", \"message\":\"El contenido editado fue bloqueado por ser inapropiado.\"}"))
            return
        }

        val success = repo.editMessage(sender, originalTimestamp, newContent)
        if (success) {
            val editResponse = buildJsonObject {
                put("type", "EDIT")
                put("usernameSender", sender)
                put("originalTimestamp", originalTimestamp)
                put("newContent", newContent)
                put("isEdited", true)
            }

            val editJson = editResponse.toString()
            org.example.routes.chatSessions[chatRoomId]?.forEach { s ->
                try {
                    s.send(Frame.Text(editJson))
                } catch (e: Exception) {
                    println("Error al enviar edición: ${e.message}")
                }
            }
        } else {
            session.send(Frame.Text("{\"error\": \"No se pudo editar el mensaje\"}"))
        }
    }
}