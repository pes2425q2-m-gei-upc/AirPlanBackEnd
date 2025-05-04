package org.example.controllers

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import org.example.models.UserBlock
import org.example.repositories.UserBlockRepository

class UserBlockController(private val blockRepository: UserBlockRepository = UserBlockRepository()) {
    
    @Serializable
    data class BlockRequest(val blockerUsername: String, val blockedUsername: String)
    
    @Serializable
    data class BlockStatusResponse(val isBlocked: Boolean)
    
    suspend fun blockUser(call: ApplicationCall) {
        try {
            println("⏳ Recibida solicitud de bloqueo")
            val request = call.receive<BlockRequest>()
            println("🔒 Intentando bloquear: ${request.blockerUsername} -> ${request.blockedUsername}")
            
            val success = blockRepository.blockUser(request.blockerUsername, request.blockedUsername)
            
            if (success) {
                println("✅ Bloqueo exitoso")
                call.respond(HttpStatusCode.Created, "User blocked successfully")
            } else {
                println("❌ Bloqueo fallido en la base de datos")
                call.respond(HttpStatusCode.InternalServerError, "Failed to block user")
            }
        } catch (e: Exception) {
            println("❌ Error al procesar solicitud de bloqueo: ${e.message}")
            e.printStackTrace()
            call.respond(HttpStatusCode.BadRequest, "Invalid block request: ${e.message}")
        }
    }
    
    suspend fun unblockUser(call: ApplicationCall) {
        try {
            val request = call.receive<BlockRequest>()
            val success = blockRepository.unblockUser(request.blockerUsername, request.blockedUsername)
            
            if (success) {
                call.respond(HttpStatusCode.OK, "User unblocked successfully")
            } else {
                call.respond(HttpStatusCode.NotFound, "Block not found or couldn't be removed")
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, "Invalid unblock request: ${e.message}")
        }
    }
    
    suspend fun checkBlockStatus(call: ApplicationCall) {
        try {
            val blocker = call.parameters["blocker"] ?: return call.respond(HttpStatusCode.BadRequest, "Missing blocker username")
            val blocked = call.parameters["blocked"] ?: return call.respond(HttpStatusCode.BadRequest, "Missing blocked username")
            
            val isBlocked = blockRepository.isUserBlocked(blocker, blocked)
            
            call.respond(HttpStatusCode.OK, BlockStatusResponse(isBlocked = isBlocked))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, "Error checking block status: ${e.message}")
        }
    }
    
    suspend fun getBlockedUsers(call: ApplicationCall) {
        try {
            val username = call.parameters["username"] ?: return call.respond(HttpStatusCode.BadRequest, "Missing username")
            
            val blockedUsers = blockRepository.getBlockedUsers(username)
            call.respond(HttpStatusCode.OK, blockedUsers)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, "Error getting blocked users: ${e.message}")
        }
    }

    // This function can be called directly from the websocket code
    fun isEitherUserBlocked(user1: String, user2: String): Boolean {
        return blockRepository.isEitherUserBlocked(user1, user2)
    }
}