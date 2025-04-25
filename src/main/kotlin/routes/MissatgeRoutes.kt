package org.example.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import org.example.controllers.ControladorMissatges
import org.example.repositories.MissatgeRepository

fun Route.missatgeRoutes() {
    val missatgeController = ControladorMissatges(MissatgeRepository())

    route("/chat") {

        // Ruta per enviar un missatge
        post("/send") {
            missatgeController.sendMessage(call)
        }

        // Ruta per obtenir la conversa entre dos usuaris
        get("/{user1}/{user2}") {
            missatgeController.getConversation(call)
        }

        get("/conversaciones/{username}") {
            missatgeController.getLatestChatsForUser(call)
        }
    }
}
