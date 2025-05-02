package org.example.routes

import io.ktor.server.routing.*
import io.ktor.websocket.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.models.Missatge
import org.example.repositories.MissatgeRepository
import java.util.Collections

// Definir la colección de sesiones WebSocket de los usuarios conectados
val connectedUsers = Collections.synchronizedSet(mutableSetOf<WebSocketSession>())

fun Route.websocketChatRoutes() {
    val repo = MissatgeRepository()

    webSocket("/ws/chat/{user1}/{user2}") {
        val user1 = call.parameters["user1"] ?: return@webSocket
        val user2 = call.parameters["user2"] ?: return@webSocket

        println("Nuevo cliente conectado: $user1 - $user2")
        connectedUsers += this

        sendChatHistory(repo, user1, user2)

        try {
            println("Esperando mensajes del cliente...")
            handleIncomingMessages(repo, this)
        } catch (e: Exception) {
            println("Error al consumir WebSocket: ${e.message}")
        } finally {
            connectedUsers -= this
            println("Cliente desconectado.")
        }
    }
}

private suspend fun WebSocketSession.sendChatHistory(repo: MissatgeRepository, user1: String, user2: String) {
    val messages = repo.getMessagesBetweenUsers(user1, user2)
    if (messages.isNotEmpty()) {
        val historyJson = Json.encodeToString(messages)
        send(Frame.Text("{\"type\":\"history\", \"messages\":$historyJson}"))
    }
}

private suspend fun handleIncomingMessages(repo: MissatgeRepository, session: WebSocketSession) {
    session.incoming.consumeEach { frame ->
        if (frame is Frame.Text) {
            val text = frame.readText()
            println("Texto recibido: $text")

            processMessage(text, repo, session)
        }
    }
}

private suspend fun processMessage(text: String, repo: MissatgeRepository, session: WebSocketSession) {
    try {
        when {
            text.contains("\"type\":\"PING\"") -> handlePingMessage(session)
            text.contains("\"type\":\"EDIT\"") -> handleEditMessage(text, repo, session)
            text.contains("\"type\":\"DELETE\"") -> handleDeleteMessage(text, repo, session)
            else -> handleRegularMessage(text, repo, session)
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

private suspend fun handleDeleteMessage(text: String, repo: MissatgeRepository, session: WebSocketSession) {
    val deleteData = Json.decodeFromString<Map<String, String>>(text)
    val sender = deleteData["usernameSender"]
    val originalTimestamp = deleteData["timestamp"]

    if (sender == null || originalTimestamp == null) {
        session.send(Frame.Text("{\"error\": \"Datos de eliminación incompletos\"}"))
        return
    }

    val success = repo.deleteMessage(sender, originalTimestamp)
    if (success) {
        broadcastDeletedMessage(sender, originalTimestamp)
    } else {
        session.send(Frame.Text("{\"error\": \"No se pudo eliminar el mensaje\"}"))
    }
}

private suspend fun broadcastDeletedMessage(sender: String, originalTimestamp: String) {
    val deleteResponse = buildJsonObject {
        put("type", "DELETE")
        put("usernameSender", sender)
        put("originalTimestamp", originalTimestamp)
    }

    val deleteJson = deleteResponse.toString()
    connectedUsers.forEach { session ->
        try {
            session.send(Frame.Text(deleteJson))
        } catch (e: Exception) {
            println("Error al enviar eliminación a sesión: ${e.message}")
        }
    }
}

private suspend fun handleRegularMessage(text: String, repo: MissatgeRepository, currentSession: WebSocketSession) {
    val missatge = Json.decodeFromString<Missatge>(text)
    repo.sendMessage(missatge)

    val messageJson = Json.encodeToString(missatge)
    //arreglar session para que solo sean 2 personas quien los reciban
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