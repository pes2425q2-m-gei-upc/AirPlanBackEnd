package org.example.routes

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import io.ktor.server.request.*
import io.ktor.http.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.websocket.WebSocketManager
import org.example.websocket.ProfileUpdateNotification
import java.time.Duration

// Request data para la API REST
@Serializable
data class ProfileUpdateRequest(
    val username: String,
    val email: String,
    val updatedFields: List<String>,
    val clientId: String? = null, // Añadido para identificar al cliente que envía la notificación
    val newUsername: String? = null // Campo para cuando cambia el username
)

@Serializable
data class ApiResponse(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null,
    val connections: Int? = null
)

fun Application.configureWebSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(60)
        timeout = Duration.ofSeconds(120)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
}

fun Route.webSocketRoutes() {
    val webSocketManager = WebSocketManager.instance
    
    // WebSocket endpoint for real-time communications
    webSocket("/ws") {
        val parameters = call.request.queryParameters
        val username = parameters["username"]
        val email = parameters["email"]
        val clientId = parameters["clientId"] // Capturar clientId de los parámetros de conexión
        
        try {
            // Register this session with the WebSocketManager
            webSocketManager.registerSession(this, username, email, clientId)
            
            // Listen for incoming messages (mainly for ping/pong or client-side events)
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        println("📩 WebSocket message received: $text")
                        // Echo back to confirm receipt
                        send(Frame.Text("✅ Received: $text"))
                    }
                    is Frame.Close -> {
                        println("🔌 WebSocket connection closed with reason: ${frame.readReason()}")
                        break
                    }
                    else -> println("🔄 Other frame type received: ${frame.frameType}")
                }
            }
        } catch (e: Exception) {
            println("⚠️ WebSocket error: ${e.message}")
        } finally {
            // Unregister the session when it's closed
            webSocketManager.unregisterSession(this)
            println("🔚 WebSocket connection ended")
        }
    }
    
    // REST endpoint to trigger notifications for profile updates
    post("/api/notifications/profile-updated") {
        try {
            val request = call.receive<ProfileUpdateRequest>()
            
            // Validate received data
            if (request.username.isBlank() || request.email.isBlank()) {
                call.respond(ApiResponse(
                    success = false,
                    error = "Username and email are required"
                ))
                return@post
            }
            
            println("🔔 Sending profile update notification for user: ${request.username} - ${request.email}")
            if (request.clientId != null) {
                println("🆔 Client ID: ${request.clientId} (este dispositivo no recibirá la notificación)")
            }
            
            // Verificar si hay un cambio de username
            if (request.newUsername != null && request.newUsername != request.username) {
                println("👤 Cambio de nombre de usuario detectado: ${request.username} → ${request.newUsername}")
                
                // Notificar usando el username antiguo
                webSocketManager.notifyProfileUpdate(
                    username = request.username,
                    email = request.email,
                    updatedFields = request.updatedFields,
                    clientId = request.clientId
                )
                
                // También notificar usando el nuevo username si está disponible
                // Esto es útil para sesiones que ya estén usando el nuevo nombre
                webSocketManager.notifyProfileUpdate(
                    username = request.newUsername,
                    email = request.email,
                    updatedFields = request.updatedFields,
                    clientId = request.clientId
                )
            } else {
                // Caso normal sin cambio de username
                webSocketManager.notifyProfileUpdate(
                    username = request.username,
                    email = request.email,
                    updatedFields = request.updatedFields,
                    clientId = request.clientId
                )
            }
            
            // Use the serializable data class instead of a map
            call.respond(ApiResponse(
                success = true,
                message = "Notification sent to connected sessions (excluding originating device)",
                connections = webSocketManager.getActiveConnectionsCount()
            ))
        } catch (e: Exception) {
            println("❌ Error sending profile update notification: ${e.message}")
            e.printStackTrace()
            
            call.respond(ApiResponse(
                success = false,
                error = "Failed to process notification: ${e.message}"
            ))
        }
    }
    
    // Endpoint to check WebSocket server status
    get("/api/websocket-status") {
        call.respond(ApiResponse(
            success = true,
            connections = webSocketManager.getActiveConnectionsCount()
        ))
    }
}