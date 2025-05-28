package org.example.controllers

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.example.repositories.MissatgeRepository

class ControladorMissatges (private val repo: MissatgeRepository) {

    suspend fun getConversation(call: ApplicationCall) {
        val user1 = call.parameters["user1"] ?: return call.respond(HttpStatusCode.BadRequest)
        val user2 = call.parameters["user2"] ?: return call.respond(HttpStatusCode.BadRequest)
        val messages = repo.getMessagesBetweenUsers(user1, user2)
        call.respond(messages)
    }

    suspend fun getLatestChatsForUser(call: ApplicationCall) {
        val username = call.parameters["username"]
        if (username == null) {
            call.respond(HttpStatusCode.BadRequest, "Falta el nom d'usuari")
            return
        }

        val chats = repo.getLatestChatsForUser(username)
        call.respond(HttpStatusCode.OK, chats)
    }
}