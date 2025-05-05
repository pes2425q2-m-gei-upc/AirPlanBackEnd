package org.example.routes

import io.ktor.http.*
import io.ktor.server.testing.*
import org.example.database.UserBlockTable
import org.example.database.UsuarioTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.testing.testApplication
import org.example.routes.userBlockRoutes
import io.ktor.server.config.*
import org.jetbrains.exposed.sql.Database
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*

class UserBlockRoutesTest {

    @BeforeEach
    fun setupDatabase() {
        // Initialize the database connection
        Database.connect(
            "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=FALSE",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )

        transaction {
            SchemaUtils.drop(UserBlockTable, UsuarioTable)
            SchemaUtils.create(UsuarioTable, UserBlockTable)

            // Insert mock users
            UsuarioTable.insert {
                it[username] = "user1"
                it[nom] = "User One"
                it[email] = "user1@example.com"
                it[idioma] = "en"
            }
            UsuarioTable.insert {
                it[username] = "user2"
                it[nom] = "User Two"
                it[email] = "user2@example.com"
                it[idioma] = "en"
            }
        }
    }

    @BeforeEach
    fun setupApplication() = testApplication {
        environment { config = MapApplicationConfig() }
        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                userBlockRoutes()
            }
        }
    }

    @Test
    fun `test block user route`() = testApplication {
        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                userBlockRoutes()
            }
        }
        val response = client.post("/api/blocks/create") {
            setBody("{\"blockerUsername\":\"user1\",\"blockedUsername\":\"user2\"}")
            contentType(ContentType.Application.Json)
        }
        println("Response status: "+response.status)
        println("Response body: "+response.bodyAsText())
        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals("User blocked successfully", response.bodyAsText())
    }

    @Test
    fun `test unblock user route`() = testApplication {
        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                userBlockRoutes()
            }
        }
        transaction {
            UserBlockTable.insert {
                it[blockerUsername] = "user1"
                it[blockedUsername] = "user2"
            }
        }

        val response = client.post("/api/blocks/remove") {
            setBody("{\"blockerUsername\":\"user1\",\"blockedUsername\":\"user2\"}")
            contentType(ContentType.Application.Json)
        }
        println("Response status: "+response.status)
        println("Response body: "+response.bodyAsText())
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("User unblocked successfully", response.bodyAsText())
    }

    @Test
    fun `test check block status route`() = testApplication {
        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                userBlockRoutes()
            }
        }
        transaction {
            UserBlockTable.insert {
                it[blockerUsername] = "user1"
                it[blockedUsername] = "user2"
            }
        }

        val response = client.get("/api/blocks/status/user1/user2")
        println("Response status: "+response.status)
        println("Response body: "+response.bodyAsText())
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("{\"isBlocked\":true}", response.bodyAsText())
    }

    @Test
    fun `test get blocked users route`() = testApplication {
        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                userBlockRoutes()
            }
        }
        transaction {
            UserBlockTable.insert {
                it[blockerUsername] = "user1"
                it[blockedUsername] = "user2"
            }
        }

        val response = client.get("/api/blocks/list/user1")
        println("Response status: "+response.status)
        println("Response body: "+response.bodyAsText())
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[{\"blockerUsername\":\"user1\",\"blockedUsername\":\"user2\"}]", response.bodyAsText())
    }
}