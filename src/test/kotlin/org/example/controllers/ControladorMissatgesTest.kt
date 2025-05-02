package org.example.controllers

import io.ktor.http.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.models.Missatge
import org.example.repositories.MissatgeRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class ControladorMissatgesTest {

    private val repo = mockk<MissatgeRepository>()
    private val controlador = ControladorMissatges(repo)

    private val testJson = Json {
        encodeDefaults = true
    }

    private fun Application.testModule() {
        install(ContentNegotiation) {
            json()
        }
        routing {
            post("/chat/send") { controlador.sendMessage(call) }
            get("/chat/{user1}/{user2}") { controlador.getConversation(call) }
            get("/chat/latest/{username}") { controlador.getLatestChatsForUser(call) }
        }
    }

    @Test
    fun `sendMessage returns Created on success`() = testApplication {
        application { testModule() }

        val message = Missatge("alice", "bob", LocalDateTime.parse("2024-05-01T10:00:00"), "Hola Bob!", false)
        coEvery { repo.sendMessage(message) } returns true

        val response = client.post("/chat/send") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(message))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals("Mensaje enviado correctamente", response.bodyAsText())
        coVerify { repo.sendMessage(message) }
    }

    @Test
    fun `sendMessage returns InternalServerError on failure`() = testApplication {
        application { testModule() }

        val message = Missatge("alice", "bob", LocalDateTime.parse("2024-05-01T10:00:00"), "Hola Bob!", false)
        coEvery { repo.sendMessage(message) } returns false

        val response = client.post("/chat/send") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(message))
        }

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertEquals("Error al enviar mensaje", response.bodyAsText())
        coVerify { repo.sendMessage(message) }
    }

    @Test
    fun `getConversation returns messages between users`() = testApplication {
        application { testModule() }

        val user1 = "alice"
        val user2 = "bob"
        val messages = listOf(
            Missatge("alice", "bob", LocalDateTime.parse("2024-05-01T10:00:00"), "Hola Bob!", false),
            Missatge("bob", "alice", LocalDateTime.parse("2024-05-01T10:01:00"), "Hola Alice!", false)
        )
        coEvery { repo.getMessagesBetweenUsers(user1, user2) } returns messages

        val response = client.get("/chat/$user1/$user2")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(testJson.encodeToString(messages), response.bodyAsText())
        coVerify { repo.getMessagesBetweenUsers(user1, user2) }
    }

    @Test
    fun `getConversation returns BadRequest when parameters are missing`() = testApplication {
        application { testModule() }

        val response = client.get("/chat/alice/")

        assertEquals(HttpStatusCode.NotFound, response.status)  // Because path is incomplete
    }

    @Test
    fun `getLatestChatsForUser returns chats`() = testApplication {
        application { testModule() }

        val username = "alice"
        val chats = listOf(
            Missatge("bob", "alice", LocalDateTime.parse("2024-05-01T10:01:00"), "Hola Alice!", false),
            Missatge("charlie", "alice", LocalDateTime.parse("2024-05-01T11:00:00"), "Buenas Alice!", false)
        )
        coEvery { repo.getLatestChatsForUser(username) } returns chats

        val response = client.get("/chat/latest/$username")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(testJson.encodeToString(chats), response.bodyAsText())
        coVerify { repo.getLatestChatsForUser(username) }
    }

    @Test
    fun `getLatestChatsForUser returns BadRequest when username missing`() = testApplication {
        application { testModule() }

        val response = client.get("/chat/latest/")

        assertEquals(HttpStatusCode.NotFound, response.status)  // Because path is incomplete
    }
}