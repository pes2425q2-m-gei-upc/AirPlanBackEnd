package org.example.routes

import io.ktor.server.routing.*
import io.ktor.websocket.*
import io.ktor.server.application.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
        connectedUsers += this  // Agregar la sesión WebSocket actual a la lista de usuarios conectados

        // Verificar el estado de bloqueo entre los usuarios
        val isBlocked = blockController.isEitherUserBlocked(user1, user2)
        val user1BlockedUser2 = blockRepository.isUserBlocked(user1, user2)
        val user2BlockedUser1 = blockRepository.isUserBlocked(user2, user1)
        
        // Enviar información sobre el estado de bloqueo junto con el historial
        val messages = repo.getMessagesBetweenUsers(user1, user2)
        val blockData = mapOf(
            "user1BlockedUser2" to user1BlockedUser2,
            "user2BlockedUser1" to user2BlockedUser1
        )
        
        // Enviar todo como un solo mensaje JSON (historial y estado de bloqueo)
        val historyJson = Json.encodeToString(messages)
        val blockJson = Json.encodeToString(blockData)
        send(Frame.Text("{\"type\":\"history\", \"messages\":$historyJson, \"blockStatus\":$blockJson}"))
        
        try {
            // Escuchar los mensajes entrantes
            incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    println("Texto recibido: $text")

                    try {
                        // Ignorar mensajes de ping
                        if (text.contains("\"type\":\"PING\"")) {
                            send(Frame.Text("{\"type\":\"PONG\"}"))
                            return@consumeEach
                        }

                        // Mensajes para gestionar bloqueos
                        if (text.contains("\"type\":\"BLOCK\"")) {
                            val jsonObject = Json.parseToJsonElement(text).jsonObject
                            val blockerUsername = jsonObject["blockerUsername"]?.jsonPrimitive?.content ?: return@consumeEach
                            val blockedUsername = jsonObject["blockedUsername"]?.jsonPrimitive?.content ?: return@consumeEach
                            
                            val success = blockRepository.blockUser(blockerUsername, blockedUsername)
                            if (success) {
                                val response = "{\"type\":\"BLOCK_RESPONSE\", \"success\":true, \"message\":\"Usuario bloqueado\"}"
                                send(Frame.Text(response))
                                
                                // Notificar al otro usuario si está conectado
                                val blockNotification = "{\"type\":\"BLOCK_NOTIFICATION\", \"blockerUsername\":\"$blockerUsername\"}"
                                connectedUsers.forEach { session ->
                                    if (session != this) {
                                        try {
                                            session.send(Frame.Text(blockNotification))
                                        } catch (e: Exception) {
                                            println("Error al enviar notificación de bloqueo: ${e.message}")
                                        }
                                    }
                                }
                            }
                            return@consumeEach
                        }
                        
                        // Mensajes para gestionar desbloqueos
                        if (text.contains("\"type\":\"UNBLOCK\"")) {
                            val jsonObject = Json.parseToJsonElement(text).jsonObject
                            val blockerUsername = jsonObject["blockerUsername"]?.jsonPrimitive?.content ?: return@consumeEach
                            val blockedUsername = jsonObject["blockedUsername"]?.jsonPrimitive?.content ?: return@consumeEach
                            
                            val success = blockRepository.unblockUser(blockerUsername, blockedUsername)
                            if (success) {
                                val response = "{\"type\":\"UNBLOCK_RESPONSE\", \"success\":true, \"message\":\"Usuario desbloqueado\"}"
                                send(Frame.Text(response))
                                
                                // Notificar al otro usuario si está conectado
                                val unblockNotification = "{\"type\":\"UNBLOCK_NOTIFICATION\", \"blockerUsername\":\"$blockerUsername\"}"
                                connectedUsers.forEach { session ->
                                    if (session != this) {
                                        try {
                                            session.send(Frame.Text(unblockNotification))
                                        } catch (e: Exception) {
                                            println("Error al enviar notificación de desbloqueo: ${e.message}")
                                        }
                                    }
                                }
                            }
                            return@consumeEach
                        }

                        val missatge = Json.decodeFromString<Missatge>(text)
                        
                        // Verificar si algún usuario ha bloqueado al otro antes de enviar el mensaje
                        val canSendMessage = !blockController.isEitherUserBlocked(missatge.usernameSender, missatge.usernameReceiver)
                        
                        if (!canSendMessage) {
                            // Si hay un bloqueo, informar al remitente
                            send(Frame.Text("{\"type\":\"ERROR\", \"message\":\"No se puede enviar el mensaje debido a un bloqueo\"}"))
                            return@consumeEach
                        }

                        // Guarda el mensaje en la base de datos
                        repo.sendMessage(missatge)

                        // Enviar el mensaje a los usuarios conectados en formato JSON
                        val messageJson = Json.encodeToString(missatge)
                        connectedUsers.forEach { session ->
                            // Solo enviar a otros usuarios conectados a este chat específico
                            // Verificamos si la sesión corresponde a la conversación entre user1 y user2
                            if (session != this) {
                                try {
                                    session.send(Frame.Text(messageJson))
                                } catch (e: Exception) {
                                    println("Error al enviar mensaje a sesión: ${e.message}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("Error al deserializar o guardar: ${e.message}")
                        send(Frame.Text("{\"error\": \"Error al procesar el mensaje\"}"))
                    }
                }
            }
        } catch (e: Exception) {
            println("Error al consumir WebSocket: ${e.message}")
        } finally {
            connectedUsers -= this  // Eliminar la sesión WebSocket cuando se desconecta
            println("Cliente desconectado.")
        }
    }
}
