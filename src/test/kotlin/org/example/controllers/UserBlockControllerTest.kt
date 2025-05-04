package org.example.controllers

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import org.example.controllers.UserBlockController
import org.example.models.UserBlock
import org.example.repositories.UserBlockRepository
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserBlockControllerTest {
    private val mockRepo = MockUserBlockRepository()

    @Serializable
    data class BlockRequest(val blockerUsername: String, val blockedUsername: String)

    @Serializable
    data class BlockStatusResponse(val isBlocked: Boolean)

    @Test
    fun testBlockUserSuccess() = testApplication {
        mockRepo.blockUserResult = true
        configureTestApplication()

        val request = BlockRequest("userA", "userB")
        val response = client.post("/api/blocks/create") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(BlockRequest.serializer(), request))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals("userA" to "userB", mockRepo.blockCalledWith)
    }

    @Test
    fun testBlockUserFailure() = testApplication {
        mockRepo.blockUserResult = false
        configureTestApplication()

        val request = BlockRequest("userA", "userB")
        val response = client.post("/api/blocks/create") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(BlockRequest.serializer(), request))
        }

        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }

    @Test
    fun testUnblockUserSuccess() = testApplication {
        mockRepo.unblockUserResult = true
        configureTestApplication()

        val request = BlockRequest("userA", "userB")
        val response = client.post("/api/blocks/remove") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(BlockRequest.serializer(), request))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("userA" to "userB", mockRepo.unblockCalledWith)
    }

    @Test
    fun testUnblockUserNotFound() = testApplication {
        mockRepo.unblockUserResult = false
        configureTestApplication()

        val request = BlockRequest("userA", "userB")
        val response = client.post("/api/blocks/remove") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(BlockRequest.serializer(), request))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testCheckBlockStatus() = testApplication {
        mockRepo.isUserBlockedResult = true
        configureTestApplication()

        val response = client.get("/api/blocks/status/userA/userB")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        val statusResp = Json.decodeFromString<BlockStatusResponse>(body)
        assertTrue(statusResp.isBlocked)
        assertEquals("userA" to "userB", mockRepo.isUserBlockedCalledWith)
    }

    @Test
    fun testGetBlockedUsers() = testApplication {
        val blockedList = listOf(UserBlock("userA", "userB"))
        mockRepo.getBlockedUsersResult = blockedList
        configureTestApplication()

        val response = client.get("/api/blocks/list/userA")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("userB"))
        assertEquals("userA", mockRepo.getBlockedUsersCalledWith)
    }

    private fun ApplicationTestBuilder.configureTestApplication() {
        application {
            install(ContentNegotiation) {
                json()
            }
            install(CORS) {
                anyHost()
            }
            routing {
                route("/api/blocks") {
                    val controller = UserBlockController(mockRepo)
                    post("/create") { controller.blockUser(call) }
                    post("/remove") { controller.unblockUser(call) }
                    get("/status/{blocker}/{blocked}") { controller.checkBlockStatus(call) }
                    get("/list/{username}") { controller.getBlockedUsers(call) }
                }
            }
        }
    }

    // Mock repository
    class MockUserBlockRepository : UserBlockRepository() {
        var blockCalledWith: Pair<String, String>? = null
        var unblockCalledWith: Pair<String, String>? = null
        var isUserBlockedCalledWith: Pair<String, String>? = null
        var getBlockedUsersCalledWith: String? = null

        var blockUserResult: Boolean = true
        var unblockUserResult: Boolean = true
        var isUserBlockedResult: Boolean = false
        var getBlockedUsersResult: List<UserBlock> = emptyList()

        override fun blockUser(blockerUsername: String, blockedUsername: String): Boolean {
            blockCalledWith = blockerUsername to blockedUsername
            return blockUserResult
        }

        override fun unblockUser(blockerUsername: String, blockedUsername: String): Boolean {
            unblockCalledWith = blockerUsername to blockedUsername
            return unblockUserResult
        }

        override fun isUserBlocked(blockerUsername: String, blockedUsername: String): Boolean {
            isUserBlockedCalledWith = blockerUsername to blockedUsername
            return isUserBlockedResult
        }

        override fun getBlockedUsers(blockerUsername: String): List<UserBlock> {
            getBlockedUsersCalledWith = blockerUsername
            return getBlockedUsersResult
        }
    }
}
