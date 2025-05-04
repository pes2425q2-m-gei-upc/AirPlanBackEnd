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
import java.util.Collections

// Definir la colección de sesiones WebSocket de los usuarios conectados
val connectedUsers = Collections.synchronizedSet(mutableSetOf<WebSocketSession>())

fun Route.websocketChatRoutes() {
    val repo = MissatgeRepository()
    val blockRepository = UserBlockRepository()
    val blockController = UserBlockController(blockRepository)

    webSocket("/ws/chat/{user1}/{user2}") {
        val user1 = call.parameters["user1"] ?: return@webSocket
        val user2 = call.parameters["user2"] ?: return@webSocket

        println("Nuevo cliente conectado: $user1 - $user2")
        connectedUsers += this

        // Enviar historial y estado de bloqueo
        sendChatHistoryAndBlockStatus(repo, blockRepository, user1, user2)

        try {
            println("Esperando mensajes del cliente...")
            handleIncomingMessages(repo, blockRepository, blockController, user1, user2, this)
        } catch (e: Exception) {
            println("Error al consumir WebSocket: ${e.message}")
        } finally {
            connectedUsers -= this
            println("Cliente desconectado.")
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
    user1: String,
    user2: String,
    session: WebSocketSession
) {
    session.incoming.consumeEach { frame ->
        if (frame is Frame.Text) {
            val text = frame.readText()
            println("Texto recibido: $text")

            processMessage(text, repo, blockRepository, blockController, user1, user2, session)
        }
    }
}

private suspend fun processMessage(
    text: String,
    repo: MissatgeRepository,
    blockRepository: UserBlockRepository,
    blockController: UserBlockController,
    user1: String,
    user2: String,
    session: WebSocketSession
) {
    try {
        when {
            text.contains("\"type\":\"PING\"") -> handlePingMessage(session)
            text.contains("\"type\":\"EDIT\"") -> handleEditMessage(text, repo, session)
            text.contains("\"type\":\"BLOCK\"") -> handleBlockMessage(text, blockRepository, session)
            text.contains("\"type\":\"UNBLOCK\"") -> handleUnblockMessage(text, blockRepository, session)
            else -> handleRegularMessage(text, repo, blockController, session)
        }
    } catch (e: Exception) {
        handleMessageError(e, session)
    }
}

private suspend fun handlePingMessage(session: WebSocketSession) {
    session.send(Frame.Text("{\"type\":\"PONG\"}"))
}

private suspend fun handleEditMessage(text: String, repo: MissatgeRepository, session: WebSocketSession) {
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

    val success = repo.editMessage(sender, originalTimestamp, newContent)
    if (success) {
        broadcastEditedMessage(sender, originalTimestamp, newContent)
    } else {
        session.send(Frame.Text("{\"error\": \"No se pudo editar el mensaje\"}"))
    }
}

private suspend fun handleBlockMessage(text: String, blockRepository: UserBlockRepository, session: WebSocketSession) {
    val jsonObject = Json.parseToJsonElement(text).jsonObject
    val blockerUsername = jsonObject["blockerUsername"]?.jsonPrimitive?.content ?: return
    val blockedUsername = jsonObject["blockedUsername"]?.jsonPrimitive?.content ?: return

    val success = blockRepository.blockUser(blockerUsername, blockedUsername)
    if (success) {
        val response = "{\"type\":\"BLOCK_RESPONSE\", \"success\":true, \"message\":\"Usuario bloqueado\"}"
        session.send(Frame.Text(response))

        // Notificar al otro usuario si está conectado
        val blockNotification = "{\"type\":\"BLOCK_NOTIFICATION\", \"blockerUsername\":\"$blockerUsername\"}"
        connectedUsers.forEach { userSession ->
            if (userSession != session) {
                try {
                    userSession.send(Frame.Text(blockNotification))
                } catch (e: Exception) {
                    println("Error al enviar notificación de bloqueo: ${e.message}")
                }
            }
        }
    }
}

private suspend fun handleUnblockMessage(text: String, blockRepository: UserBlockRepository, session: WebSocketSession) {
    val jsonObject = Json.parseToJsonElement(text).jsonObject
    val blockerUsername = jsonObject["blockerUsername"]?.jsonPrimitive?.content ?: return
    val blockedUsername = jsonObject["blockedUsername"]?.jsonPrimitive?.content ?: return

    val success = blockRepository.unblockUser(blockerUsername, blockedUsername)
    if (success) {
        val response = "{\"type\":\"UNBLOCK_RESPONSE\", \"success\":true, \"message\":\"Usuario desbloqueado\"}"
        session.send(Frame.Text(response))

        // Notificar al otro usuario si está conectado
        val unblockNotification = "{\"type\":\"UNBLOCK_NOTIFICATION\", \"blockerUsername\":\"$blockerUsername\"}"
        connectedUsers.forEach { userSession ->
            if (userSession != session) {
                try {
                    userSession.send(Frame.Text(unblockNotification))
                } catch (e: Exception) {
                    println("Error al enviar notificación de desbloqueo: ${e.message}")
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

private suspend fun broadcastEditedMessage(sender: String, originalTimestamp: String, newContent: String) {
    val editResponse = buildJsonObject {
        put("type", "EDIT")
        put("usernameSender", sender)
        put("originalTimestamp", originalTimestamp)
        put("newContent", newContent)
        put("isEdited", true)
    }

    val editJson = editResponse.toString()
    connectedUsers.forEach { session ->
        try {
            session.send(Frame.Text(editJson))
        } catch (e: Exception) {
            println("Error al enviar edición a sesión: ${e.message}")
        }
    }
}

private suspend fun handleRegularMessage(
    text: String,
    repo: MissatgeRepository,
    blockController: UserBlockController,
    currentSession: WebSocketSession
) {
    val missatge = Json.decodeFromString<Missatge>(text)

    // Verificar si algún usuario ha bloqueado al otro antes de enviar el mensaje
    val canSendMessage = !blockController.isEitherUserBlocked(missatge.usernameSender, missatge.usernameReceiver)

    if (!canSendMessage) {
        // Si hay un bloqueo, informar al remitente
        currentSession.send(Frame.Text("{\"type\":\"ERROR\", \"message\":\"No se puede enviar el mensaje debido a un bloqueo\"}"))
        return
    }

    // Guarda el mensaje en la base de datos
    repo.sendMessage(missatge)

    // Enviar el mensaje a los usuarios conectados en formato JSON
    val messageJson = Json.encodeToString(missatge)
    connectedUsers.forEach { session ->
        if (session != currentSession) {
            try {
                session.send(Frame.Text(messageJson))
            } catch (e: Exception) {
                println("Error al enviar mensaje a sesión: ${e.message}")
            }
        }
    }
}

private suspend fun handleMessageError(e: Exception, session: WebSocketSession) {
    println("Error al deserializar o guardar: ${e.message}")
    session.send(Frame.Text("{\"error\": \"Error al procesar el mensaje\"}"))
}