package org.example.routes

import io.ktor.server.routing.*
import io.ktor.websocket.*
import io.ktor.server.application.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.decodeFromString
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
        connectedUsers += this  // Agregar la sesión WebSocket actual a la lista de usuarios conectados

        // Enviar el historial de mensajes cuando se conecta
        val messages = repo.getMessagesBetweenUsers(user1, user2)
        messages.forEach { message ->
            send("De ${message.usernameSender} a ${message.usernameReceiver}: ${message.missatge}")
        }

        try {
            // Escuchar los mensajes entrantes
            incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    println("Texto recibido: $text")

                    try {
                        val missatge = Json.decodeFromString<Missatge>(text)

                        // Guarda el mensaje en la base de datos
                        repo.sendMessage(missatge)

                        // Enviar el mensaje a los dos usuarios en este chat
                        connectedUsers.forEach { session ->
                            if (session != this) {
                                session.send("De ${missatge.usernameSender} a ${missatge.usernameReceiver}: ${missatge.missatge}")
                            }
                        }
                    } catch (e: Exception) {
                        println("Error al deserializar o guardar: ${e.message}")
                        send("Error al procesar el mensaje")
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
