package org.example.routes

import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.example.controllers.UserBlockController
import org.example.repositories.UserBlockRepository

fun Route.userBlockRoutes() {
    val blockRepository = UserBlockRepository()
    val blockController = UserBlockController(blockRepository)
    
    route("/api/blocks") {
        // Block a user
        post("/create") {
            blockController.blockUser(call)
        }
        
        // Unblock a user
        post("/remove") {
            blockController.unblockUser(call)
        }
        
        // Check if a user is blocked by another user
        get("/status/{blocker}/{blocked}") {
            blockController.checkBlockStatus(call)
        }
        
        // Get all users blocked by a specific user
        get("/list/{username}") {
            blockController.getBlockedUsers(call)
        }
    }
}