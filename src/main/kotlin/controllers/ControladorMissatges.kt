package org.example.controllers

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.example.repositories.MissatgeRepository
import org.example.models.Missatge

class ControladorMissatges (private val repo: MissatgeRepository) {
    suspend fun sendMessage(call: ApplicationCall) {
        val message = call.receive<Missatge>()
        val success = repo.sendMessage(message)
        if (success) {
            call.respond(HttpStatusCode.Created, "Mensaje enviado correctamente")
        } else {
            call.respond(HttpStatusCode.InternalServerError, "Error al enviar mensaje")
        }
    }

    suspend fun getConversation(call: ApplicationCall) {
        val user1 = call.parameters["user1"] ?: return call.respond(HttpStatusCode.BadRequest)
        val user2 = call.parameters["user2"] ?: return call.respond(HttpStatusCode.BadRequest)
        val messages = repo.getMessagesBetweenUsers(user1, user2)
        call.respond(messages)
    }
}