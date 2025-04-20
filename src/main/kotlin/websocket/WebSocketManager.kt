package org.example.websocket

import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

// Clase serializable para los mensajes de notificación
@Serializable
data class ProfileUpdateNotification(
    val type: String,
    val username: String,
    val email: String,
    val updatedFields: List<String>,
    val timestamp: Long,
    val clientId: String? = null // Añadido para identificar al cliente que envía la notificación
)

// Clase serializable para la confirmación de conexión
@Serializable
data class ConnectionEstablishedMessage(
    val type: String,
    val message: String
)

/**
 * WebSocketManager handles connections from multiple clients and manages notifications.
 * It maintains a map of connections organized by username/email to target specific users.
 */
class WebSocketManager {
    // Store WebSocket sessions by username
    private val userSessions = ConcurrentHashMap<String, MutableSet<DefaultWebSocketSession>>()
    // Store WebSocket sessions by email
    private val emailSessions = ConcurrentHashMap<String, MutableSet<DefaultWebSocketSession>>()
    // Store clientId associated with each session for filtering
    private val sessionClientIds = ConcurrentHashMap<DefaultWebSocketSession, String>()
    
    companion object {
        // Usando solo una forma de acceder a la instancia del singleton
        val instance: WebSocketManager by lazy { WebSocketManager() }
    }
    
    /**
     * Register a new WebSocket session for a user
     */
    suspend fun registerSession(session: DefaultWebSocketSession, username: String?, email: String?, clientId: String? = null) {
        if (username != null && username.isNotEmpty()) {
            val sessions = userSessions.getOrPut(username) { mutableSetOf() }
            sessions.add(session)
            println("👤 WebSocket registered for username: $username (Total connections: ${sessions.size})")
        }
        
        if (email != null && email.isNotEmpty()) {
            val sessions = emailSessions.getOrPut(email) { mutableSetOf() }
            sessions.add(session)
            println("📧 WebSocket registered for email: $email (Total connections: ${sessions.size})")
        }
        
        // Store clientId if provided
        if (clientId != null && clientId.isNotEmpty()) {
            sessionClientIds[session] = clientId
            println("🆔 ClientId registered for session: $clientId")
        }
        
        println("📊 Current active connections: ${getActiveConnectionsCount()}")
        
        // Send confirmation of connection to the client
        try {
            val confirmationMessage = ConnectionEstablishedMessage(
                type = "CONNECTION_ESTABLISHED",
                message = "Connected successfully to WebSocket server"
            )
            session.send(Frame.Text(Json.encodeToString(confirmationMessage)))
        } catch (e: Exception) {
            println("Error sending connection confirmation: ${e.message}")
        }
    }
    
    /**
     * Unregister a WebSocket session when it closes
     */
    fun unregisterSession(session: DefaultWebSocketSession) {
        // Remove from username sessions
        userSessions.forEach { (username, sessions) ->
            if (sessions.remove(session)) {
                println("👤 WebSocket unregistered for username: $username (Remaining connections: ${sessions.size})")
                if (sessions.isEmpty()) {
                    userSessions.remove(username)
                    println("🗑️ Removed empty session list for username: $username")
                }
            }
        }
        
        // Remove from email sessions
        emailSessions.forEach { (email, sessions) ->
            if (sessions.remove(session)) {
                println("📧 WebSocket unregistered for email: $email (Remaining connections: ${sessions.size})")
                if (sessions.isEmpty()) {
                    emailSessions.remove(email)
                    println("🗑️ Removed empty session list for email: $email")
                }
            }
        }
        
        println("📊 Current active connections after unregister: ${getActiveConnectionsCount()}")
    }
    
    /**
     * Notify all devices for a specific user's profile update
     */
    suspend fun notifyProfileUpdate(username: String, email: String, updatedFields: List<String>, clientId: String? = null) {
        // Crear un objeto serializable en lugar de un Map
        val notification = ProfileUpdateNotification(
            type = "PROFILE_UPDATE",
            username = username,
            email = email,
            updatedFields = updatedFields,
            timestamp = System.currentTimeMillis(),
            clientId = clientId // Incluir el clientId en la notificación
        )
        
        // Serializar utilizando la clase con anotación @Serializable
        val message = Json.encodeToString(notification)
        
        // Get all relevant sessions
        val usernameSessions = userSessions[username] ?: emptySet()
        val emailSessions = emailSessions[email] ?: emptySet()
        
        // Combine sessions and send notification to all except the originating session
        val allSessions = (usernameSessions + emailSessions).toSet()
        
        // Filtrar sesiones para excluir la que tiene el mismo clientId
        val filteredSessions = if (clientId != null) {
            allSessions.filter { session ->
                val sessionClientId = sessionClientIds[session]
                // Conservar solo las sesiones con clientId diferente o nulo
                sessionClientId != clientId
            }
        } else {
            allSessions
        }
        
        println("📢 PROFILE UPDATE NOTIFICATION: Sending to ${filteredSessions.size} of ${allSessions.size} sessions for user: $username (email: $email)")
        if (clientId != null) {
            println("🔄 Excluding notifications to originating device with clientId: $clientId")
        }
        println("📋 Updated fields: $updatedFields")
        
        if (filteredSessions.isEmpty()) {
            println("⚠️ No active sessions to notify after filtering! No notifications will be sent.")
            return
        }
        
        filteredSessions.forEachIndexed { index, session ->
            try {
                session.send(Frame.Text(message))
                println("✅ Successfully sent notification #${index+1} to session for user: $username")
            } catch (e: ClosedSendChannelException) {
                println("⚠️ Failed to send notification (channel closed) to session #${index+1}: ${e.message}")
                unregisterSession(session)
            } catch (e: Exception) {
                println("⚠️ Error sending notification to session #${index+1}: ${e.message}")
            }
        }
    }
    
    /**
     * Get the number of active connections
     */
    fun getActiveConnectionsCount(): Int {
        val uniqueSessions = mutableSetOf<DefaultWebSocketSession>()
        userSessions.values.forEach { uniqueSessions.addAll(it) }
        emailSessions.values.forEach { uniqueSessions.addAll(it) }
        return uniqueSessions.size
    }
}