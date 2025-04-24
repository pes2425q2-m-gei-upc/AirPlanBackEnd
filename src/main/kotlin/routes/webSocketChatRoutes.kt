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
import java.util.*

val connectedUsers = Collections.synchronizedMap(mutableMapOf<String, DefaultWebSocketServerSession>()) // Mapa de usuario -> sesión

fun Route.websocketChatRoutes() {
    val repo = MissatgeRepository()

    webSocket("/ws/chat") {
        println("Nuevo cliente conectado.")
        var username: String? = null

        try {
            // Esperamos a recibir el nombre de usuario del cliente cuando se conecta
            incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    if (username == null) {
                        // Asumimos que el primer mensaje es el nombre de usuario
                        username = text
                        connectedUsers[username!!] = this // Guardamos la sesión con el nombre de usuario
                        println("Usuario $username conectado.")
                    } else {
                        // Procesamos los mensajes después de haber recibido el nombre de usuario
                        try {
                            val missatge = Json.decodeFromString<Missatge>(text)

                            // Guarda el mensaje en la base de datos
                            repo.sendMessage(missatge)

                            // Enviar el mensaje solo al receptor
                            val receiverSession = connectedUsers[missatge.usernameReceiver]
                            if (receiverSession != null) {
                                receiverSession.send("De ${missatge.usernameSender}: ${missatge.missatge}")
                            } else {
                                println("Usuario ${missatge.usernameReceiver} no está conectado.")
                                send("El usuario ${missatge.usernameReceiver} no está conectado.")
                            }
                        } catch (e: Exception) {
                            println("Error al deserializar o guardar: ${e.message}")
                            send("Error al procesar el mensaje.")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Error al consumir WebSocket: ${e.message}")
        } finally {
            if (username != null) {
                connectedUsers -= username // Eliminamos la sesión del mapa al desconectarse
                println("Cliente $username desconectado.")
            }
        }
    }
}
