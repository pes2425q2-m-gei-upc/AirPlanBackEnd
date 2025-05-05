package org.example.routes

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.datetime.LocalDateTime
import org.example.database.MissatgesTable
import org.example.database.UsuarioTable
import org.example.models.Missatge
import org.example.repositories.MissatgeRepository
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import java.util.*
import kotlin.test.*
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MissatgeRoutesTest {

    companion object {
        private lateinit var database: Database

        @BeforeAll
        @JvmStatic
        fun setupClass() {
            database = Database.connect(
                "jdbc:h2:mem:test_${UUID.randomUUID()};DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                driver = "org.h2.Driver",
                user = "sa",
                password = ""
            )
        }
    }

    @BeforeEach
    fun setup() {
        transaction(database) {
            SchemaUtils.create(MissatgesTable, UsuarioTable)
        }
    }

    @AfterEach
    fun tearDown() {
        transaction(database) {
            SchemaUtils.drop(MissatgesTable, UsuarioTable)
        }
    }

    @AfterAll
    fun tearDownAll() {
        // Opcional: cerrar conexión si es necesario
    }

    @Test
    fun `test enviar mensaje exitoso`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            routing { missatgeRoutes() }
        }

        val testMessage = Missatge(
            usernameSender = "user1",
            usernameReceiver = "user2",
            dataEnviament = LocalDateTime.parse("2024-05-01T12:00:00"),
            missatge = "Hola desde test"
        )

        val response = client.post("/chat/send") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(testMessage))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals("Mensaje enviado correctamente", response.bodyAsText())
    }

    @Test
    fun `test obtener conversa entre usuarios`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            routing { missatgeRoutes() }
        }

        // Insertar datos de prueba
        transaction(database) {
            val repo = MissatgeRepository()
            repo.sendMessage(Missatge(
                "user1", "user2",
                LocalDateTime.parse("2024-05-01T10:00:00"),
                "Primer missatge"
            ))
            repo.sendMessage(Missatge(
                "user2", "user1",
                LocalDateTime.parse("2024-05-01T10:05:00"),
                "Resposta"
            ))
        }

        val response = client.get("/chat/user1/user2")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("Primer missatge") && body.contains("Resposta"))
    }

    @Test
    fun `test obtener conversaciones recientes`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            routing { missatgeRoutes() }
        }

        transaction(database) {
            val repo = MissatgeRepository()
            listOf(
                Missatge("user1", "user2", LocalDateTime.parse("2024-05-01T09:00:00"), "Hola"),
                Missatge("user3", "user1", LocalDateTime.parse("2024-05-01T10:00:00"), "Hola de nuevo"),
                Missatge("user2", "user1", LocalDateTime.parse("2024-05-01T11:00:00"), "Último mensaje")
            ).forEach { repo.sendMessage(it) }
        }

        val response = client.get("/chat/conversaciones/user1")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("Último mensaje") && body.contains("Hola de nuevo"))
    }
}