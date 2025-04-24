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

val connectedUsers = Collections.synchronizedSet(mutableSetOf<DefaultWebSocketServerSession>())

fun Route.websocketChatRoutes() {
    val repo = MissatgeRepository()

    webSocket("/ws/chat") {
        println("Nou client connectat.")
        connectedUsers += this

        try {
            incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    println("Text rebut: $text")

                    try {
                        val missatge = Json.decodeFromString<Missatge>(text)

                        // Guarda a la base de dades
                        repo.sendMessage(missatge)

                        // Envia a tots els altres usuaris connectats
                        connectedUsers.forEach { session ->
                            if (session != this) {
                                session.send("De ${missatge.usernameSender} a ${missatge.usernameReceiver}: ${missatge.missatge}")
                            }
                        }
                    } catch (e: Exception) {
                        println("Error en deserialitzar o guardar: ${e.message}")
                        send("Error al processar el missatge")
                    }
                }
            }
        } catch (e: Exception) {
            println("Error al consumir WebSocket: ${e.message}")
        } finally {
            connectedUsers -= this
            println("Client desconnectat.")
        }
    }
}
