package org.example.routes

import io.ktor.server.routing.*
import io.ktor.websocket.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.decodeFromString
import org.example.models.Missatge
import org.example.repositories.MissatgeRepository
import org.example.repositories.UserBlockRepository
import org.example.controllers.UserBlockController
import org.example.services.PerspectiveService
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap // Added import

// Store sessions per chat room
val chatSessions = ConcurrentHashMap<String, MutableSet<WebSocketSession>>()

// Helper function to get a consistent chat room ID
private fun getChatRoomId(user1: String, user2: String): String =
    listOf(user1, user2).sorted().joinToString("_")

fun Route.websocketChatRoutes() {
    val repo = MissatgeRepository()
    val blockRepository = UserBlockRepository()
    val blockController = UserBlockController(blockRepository)
    val perspectiveService = PerspectiveService()

    webSocket("/ws/chat/{user1}/{user2}") {
        val user1 = call.parameters["user1"] ?: return@webSocket
        val user2 = call.parameters["user2"] ?: return@webSocket
        val chatRoomId = getChatRoomId(user1, user2)

        println("Nuevo cliente conectado al chat $chatRoomId: $user1 - $user2")
        val sessionsInRoom = chatSessions.computeIfAbsent(chatRoomId) {
            Collections.synchronizedSet(mutableSetOf<WebSocketSession>())
        }
        sessionsInRoom.add(this)

        sendChatHistoryAndBlockStatus(repo, blockRepository, user1, user2)

        try {
            println("Esperando mensajes del cliente en $chatRoomId...")
            handleIncomingMessages(repo, blockRepository, blockController, perspectiveService, user1, user2, chatRoomId, this) // Pass chatRoomId
        } catch (e: Exception) {
            println("Error al consumir WebSocket en $chatRoomId: ${e.message}")
        } finally {
            sessionsInRoom.remove(this)
            if (sessionsInRoom.isEmpty()) {
                chatSessions.remove(chatRoomId)
            }
            println("Cliente desconectado del chat $chatRoomId.")
        }
    }
}

private suspend fun WebSocketSession.sendChatHistoryAndBlockStatus(
    repo: MissatgeRepository,
    blockRepository: UserBlockRepository,
    user1: String,
    user2: String
) {
    val messages = repo.getMessagesBetweenUsers(user1, user2)
    val user1BlockedUser2 = blockRepository.isUserBlocked(user1, user2)
    val user2BlockedUser1 = blockRepository.isUserBlocked(user2, user1)

    val blockData = mapOf(
        "user1BlockedUser2" to user1BlockedUser2,
        "user2BlockedUser1" to user2BlockedUser1
    )

    val historyJson = Json.encodeToString(messages)
    val blockJson = Json.encodeToString(blockData)
    send(Frame.Text("{\"type\":\"history\", \"messages\":$historyJson, \"blockStatus\":$blockJson}"))
}

private suspend fun handleIncomingMessages(
    repo: MissatgeRepository,
    blockRepository: UserBlockRepository,
    blockController: UserBlockController,
    perspectiveService: PerspectiveService,
    user1: String, // Keep for context if needed, or remove if chatRoomId is enough
    user2: String, // Keep for context if needed, or remove if chatRoomId is enough
    chatRoomId: String, // Added parameter
    session: WebSocketSession
) {
    session.incoming.consumeEach { frame ->
        if (frame is Frame.Text) {
            val text = frame.readText()
            println("Texto recibido en $chatRoomId: $text")

            processMessage(text, repo, blockRepository, blockController, perspectiveService, user1, user2, chatRoomId, session) // Pass chatRoomId
        }
    }
}

private suspend fun processMessage(
    text: String,
    repo: MissatgeRepository,
    blockRepository: UserBlockRepository,
    blockController: UserBlockController,
    perspectiveService: PerspectiveService,
    user1: String, // Keep for context
    user2: String, // Keep for context
    chatRoomId: String, // Added parameter
    session: WebSocketSession
) {
    try {
        when {
            text.contains("\"type\":\"PING\"") -> handlePingMessage(session)
            text.contains("\"type\":\"EDIT\"") -> handleEditMessage(text, repo, perspectiveService, chatRoomId, session) // Pass chatRoomId
            text.contains("\"type\":\"BLOCK\"") -> handleBlockMessage(text, blockRepository, chatRoomId, session) // Pass chatRoomId
            text.contains("\"type\":\"UNBLOCK\"") -> handleUnblockMessage(text, blockRepository, chatRoomId, session) // Pass chatRoomId
            text.contains("\"type\":\"DELETE\"") -> handleDeleteMessage(text, repo, chatRoomId, session) // Pass chatRoomId
            else -> handleRegularMessage(text, repo, blockController, perspectiveService, chatRoomId, session) // Pass chatRoomId
        }
    } catch (e: Exception) {
        handleMessageError(e, session)
    }
}

private suspend fun handlePingMessage(session: WebSocketSession) {
    session.send(Frame.Text("{\"type\":\"PONG\"}"))
}

private suspend fun handleEditMessage(text: String, repo: MissatgeRepository, perspectiveService: PerspectiveService, chatRoomId: String, session: WebSocketSession) { // Added chatRoomId
    val editData = Json.decodeFromString<Map<String, String>>(text)
    val sender = editData["usernameSender"]
    val originalTimestamp = editData["originalTimestamp"]
    val newContent = editData["newContent"]

    if (sender == null || originalTimestamp == null || newContent == null) {
        session.send(Frame.Text("{\"error\": \"Datos de edición incompletos\"}"))
        return
    }

    if (!isMessageEditableByTime(originalTimestamp)) {
        session.send(Frame.Text("{\"error\": \"No se puede editar mensajes después de 20 minutos\"}"))
        return
    }

    if (perspectiveService.analyzeMessage(newContent)) {
        session.send(Frame.Text("{\"type\":\"ERROR\", \"message\":\"El contenido editado fue bloqueado por ser inapropiado.\"}"))
        return
    }

    val success = repo.editMessage(sender, originalTimestamp, newContent)
    if (success) {
        broadcastEditedMessage(sender, originalTimestamp, newContent, chatRoomId) // Pass chatRoomId
    } else {
        session.send(Frame.Text("{\"error\": \"No se pudo editar el mensaje\"}"))
    }
}

private suspend fun handleBlockMessage(text: String, blockRepository: UserBlockRepository, chatRoomId: String, session: WebSocketSession) { // Added chatRoomId
    val jsonObject = Json.parseToJsonElement(text).jsonObject
    val blockerUsername = jsonObject["blockerUsername"]?.jsonPrimitive?.content ?: return
    val blockedUsername = jsonObject["blockedUsername"]?.jsonPrimitive?.content ?: return

    val success = blockRepository.blockUser(blockerUsername, blockedUsername)
    if (success) {
        val response = "{\"type\":\"BLOCK_RESPONSE\", \"success\":true, \"message\":\"Usuario bloqueado\"}"
        session.send(Frame.Text(response))

        val blockNotification = "{\"type\":\"BLOCK_NOTIFICATION\", \"blockerUsername\":\"$blockerUsername\", \"blockedUsername\":\"$blockedUsername\"}"
        chatSessions[chatRoomId]?.forEach { userSession -> // Target users in the same chat room
            if (userSession != session) { // Notify the other user in the chat
                try {
                    userSession.send(Frame.Text(blockNotification))
                } catch (e: Exception) {
                    println("Error al enviar notificación de bloqueo a $chatRoomId: ${e.message}")
                }
            }
        }
    }
}

private suspend fun handleUnblockMessage(text: String, blockRepository: UserBlockRepository, chatRoomId: String, session: WebSocketSession) { // Added chatRoomId
    val jsonObject = Json.parseToJsonElement(text).jsonObject
    val blockerUsername = jsonObject["blockerUsername"]?.jsonPrimitive?.content ?: return
    val blockedUsername = jsonObject["blockedUsername"]?.jsonPrimitive?.content ?: return

    val success = blockRepository.unblockUser(blockerUsername, blockedUsername)
    if (success) {
        val response = "{\"type\":\"UNBLOCK_RESPONSE\", \"success\":true, \"message\":\"Usuario desbloqueado\"}"
        session.send(Frame.Text(response))

        val unblockNotification = "{\"type\":\"UNBLOCK_NOTIFICATION\", \"blockerUsername\":\"$blockerUsername\", \"unblockedUsername\":\"$blockedUsername\"}"
        chatSessions[chatRoomId]?.forEach { userSession -> // Target users in the same chat room
            if (userSession != session) { // Notify the other user in the chat
                try {
                    userSession.send(Frame.Text(unblockNotification))
                } catch (e: Exception) {
                    println("Error al enviar notificación de desbloqueo a $chatRoomId: ${e.message}")
                }
            }
        }
    }
}

private fun isMessageEditableByTime(originalTimestamp: String): Boolean {
    val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
    val originalDateTime = java.time.LocalDateTime.parse(originalTimestamp, formatter)
        .atZone(java.time.ZoneId.systemDefault()).toInstant()
    val currentDateTime = java.time.Instant.now()

    val diffMinutes = java.time.temporal.ChronoUnit.MINUTES.between(originalDateTime, currentDateTime)
    return diffMinutes <= 20
}

private suspend fun broadcastEditedMessage(sender: String, originalTimestamp: String, newContent: String, chatRoomId: String) { // Added chatRoomId
    val editResponse = buildJsonObject {
        put("type", "EDIT")
        put("usernameSender", sender)
        put("originalTimestamp", originalTimestamp)
        put("newContent", newContent)
        put("isEdited", true)
    }

    val editJson = editResponse.toString()
    chatSessions[chatRoomId]?.forEach { session -> // Target users in the same chat room
        try {
            session.send(Frame.Text(editJson))
        } catch (e: Exception) {
            println("Error al enviar edición a sesión en $chatRoomId: ${e.message}")
        }
    }
}

private suspend fun handleRegularMessage(
    text: String,
    repo: MissatgeRepository,
    blockController: UserBlockController,
    perspectiveService: PerspectiveService,
    chatRoomId: String, // Added parameter
    currentSession: WebSocketSession
) {
    val missatgeObj = Json.decodeFromString<Missatge>(text)
    val messageContentForAnalysis = missatgeObj.missatge

    if (messageContentForAnalysis.isBlank()) {
        println("WARN: Message content is blank in $chatRoomId. Message will be allowed without Perspective check. Original JSON: $text")
    }

    if (messageContentForAnalysis.isNotBlank() && perspectiveService.analyzeMessage(messageContentForAnalysis)) {
        currentSession.send(Frame.Text("{\"type\":\"ERROR\", \"message\":\"Tu mensaje fue bloqueado por contenido inapropiado.\"}"))
        return
    }

    val canSendMessage = !blockController.isEitherUserBlocked(missatgeObj.usernameSender, missatgeObj.usernameReceiver)

    if (!canSendMessage) {
        currentSession.send(Frame.Text("{\"type\":\"ERROR\", \"message\":\"No se puede enviar el mensaje debido a un bloqueo\"}"))
        return
    }

    repo.sendMessage(missatgeObj)
    val messageJson = Json.encodeToString(missatgeObj)
    
    // Send to all users in the specific chat room (including sender for confirmation, or exclude sender if frontend handles it)
    // Current logic sends to everyone in the room. If sender should not receive their own message back, add `if (session != currentSession)`
    chatSessions[chatRoomId]?.forEach { session ->
        // if (session != currentSession) { // Uncomment if sender should not receive their own message
            try {
                session.send(Frame.Text(messageJson))
            } catch (e: Exception) {
                println("Error al enviar mensaje a sesión en $chatRoomId: ${e.message}")
            }
        // }
    }
}

private suspend fun handleDeleteMessage(text: String, repo: MissatgeRepository, chatRoomId: String, session: WebSocketSession) { // Added chatRoomId
    val deleteData = Json.decodeFromString<Map<String, String>>(text)
    val sender = deleteData["usernameSender"]
    val originalTimestamp = deleteData["timestamp"]

    if (sender == null || originalTimestamp == null) {
        session.send(Frame.Text("{\"error\": \"Datos de eliminación incompletos\"}"))
        return
    }

    val success = repo.deleteMessage(sender, originalTimestamp)
    if (success) {
        broadcastDeletedMessage(sender, originalTimestamp, chatRoomId) // Pass chatRoomId
    } else {
        session.send(Frame.Text("{\"error\": \"No se pudo eliminar el mensaje\"}"))
    }
}

private suspend fun broadcastDeletedMessage(sender: String, originalTimestamp: String, chatRoomId: String) { // Added chatRoomId
    val deleteResponse = buildJsonObject {
        put("type", "DELETE")
        put("usernameSender", sender)
        put("originalTimestamp", originalTimestamp)
    }

    val deleteJson = deleteResponse.toString()
    chatSessions[chatRoomId]?.forEach { session -> // Target users in the same chat room
        try {
            session.send(Frame.Text(deleteJson))
        } catch (e: Exception) {
            println("Error al enviar mensaje de eliminación a $chatRoomId: ${e.message}")
        }
    }
}

private suspend fun handleMessageError(e: Exception, session: WebSocketSession) {
    println("Error al deserializar o guardar: ${e.message}")
    session.send(Frame.Text("{\"error\": \"Error al procesar el mensaje\"}"))
}