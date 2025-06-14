package org.example.websocket

import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.controllers.ControladorNotificacions
import java.util.concurrent.ConcurrentHashMap
import org.example.repositories.NotificationRepository


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

@Serializable
data class AccountDeletedNotification(
    val type: String,
    val username: String,
    val email: String,
    val timestamp: Long,
    val clientId: String
)

@Serializable
data class RealTimeEventNotification(
    val type: String,
    val message: String,
    val username: String,
    val timestamp: Long,
    val clientId: String? = null
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
    private val notificationController = ControladorNotificacions(notificationRepository = NotificationRepository())
    
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
     * Notifica a todos los dispositivos conectados que la cuenta ha sido eliminada
     */
    suspend fun notifyAccountDeleted(username: String, email: String, clientId: String? = null) {
        println("🗑️ Notificando eliminación de cuenta: $username ($email)")
        
        // Obtener todas las sesiones para este usuario/email
        val usernameSessions = userSessions[username] ?: emptySet()
        val emailSessions = emailSessions[email] ?: emptySet()
        
        // Combinar ambos conjuntos de sesiones
        val allSessions = (usernameSessions + emailSessions).toSet()
        
        // Filtrar las sesiones para excluir la que tiene el mismo clientId que el solicitante
        val filteredSessions = if (clientId != null && clientId.isNotEmpty()) {
            allSessions.filter { session ->
                val sessionClientId = sessionClientIds[session]
                // Si la sesión tiene un clientId, comparamos; si no, la incluimos por defecto
                sessionClientId == null || sessionClientId != clientId
            }
        } else {
            allSessions
        }
        
        println("📤 Enviando notificaciones de eliminación a ${filteredSessions.size} de ${allSessions.size} sesiones")
        if (clientId != null) {
            println("🔍 Excluyendo dispositivo con clientId: $clientId")
        }
        
        // Solo continuamos si hay sesiones después del filtrado
        if (filteredSessions.isEmpty()) {
            println("⚠️ No hay sesiones activas después del filtrado. No se enviarán notificaciones.")
            return
        }
        
        // Contador para depuración
        var notificationCount = 0
        
        // Preparar el mensaje una sola vez fuera del bucle
        val deletionMessage = Json.encodeToString(
            AccountDeletedNotification(
                type = "ACCOUNT_DELETED",
                username = username,
                email = email,
                timestamp = System.currentTimeMillis(),
                clientId = clientId ?: ""
            )
        )
        
        // Enviar la notificación a las sesiones filtradas
        filteredSessions.forEachIndexed { index, session ->
            try {
                session.send(Frame.Text(deletionMessage))
                notificationCount++
                println("✅ Notificación #${index + 1} enviada correctamente")
            } catch (e: ClosedSendChannelException) {
                println("❌ Error enviando notificación (canal cerrado) a la sesión #${index + 1}: ${e.message}")
                unregisterSession(session)
            } catch (e: Exception) {
                println("❌ Error enviando notificación a la sesión #${index + 1}: ${e.message}")
            }
        }
        
        println("📤 Total de notificaciones de eliminación enviadas: $notificationCount")
    }

    /**
     * Notify all devices for a specific real-time event
     */
    suspend fun notifyRealTimeEvent(username: String, message: String, clientId: String? = null, type: String) {
        // Crear un objeto serializable para el evento en tiempo real
        val eventNotification = RealTimeEventNotification(
            type = type,
            message = message,
            username = username,
            timestamp = System.currentTimeMillis(),
        )

        // Serializar el objeto de notificación a un mensaje JSON
        val notificationMessage = Json.encodeToString(eventNotification)

        notificationController.addNotification(username, eventNotification.type, eventNotification.message)

        // Obtener todas las sesiones activas para este usuario
        val usernameSessions = userSessions[username] ?: emptySet()
        val emailSessions = emailSessions[username] ?: emptySet()

        // Combinar todas las sesiones activas de este usuario
        val allSessions = (usernameSessions + emailSessions).toSet()

        // Filtrar las sesiones para excluir la que tiene el mismo clientId que el que envía el evento
        val filteredSessions = if (clientId != null) {
            allSessions.filter { session ->
                val sessionClientId = sessionClientIds[session]
                // Conservar solo las sesiones con un clientId diferente
                sessionClientId != clientId
            }
        } else {
            allSessions
        }

        println("📢 EVENT NOTIFICATION: Sending to ${filteredSessions.size} of ${allSessions.size} sessions for user: $username")
        if (clientId != null) {
            println("🔄 Excluding notifications to originating device with clientId: $clientId")
        }

        if (filteredSessions.isEmpty()) {
            println("⚠️ No active sessions to notify after filtering! No notifications will be sent.")
            return
        }

        // Enviar la notificación a todas las sesiones filtradas
        filteredSessions.forEachIndexed { index, session ->
            try {
                session.send(Frame.Text(notificationMessage))
                println("✅ Successfully sent event notification #${index+1} to session for user: $username")
            } catch (e: ClosedSendChannelException) {
                println("⚠️ Failed to send event notification (channel closed) to session #${index+1}: ${e.message}")
                unregisterSession(session)
            } catch (e: Exception) {
                println("⚠️ Error sending event notification to session #${index+1}: ${e.message}")
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