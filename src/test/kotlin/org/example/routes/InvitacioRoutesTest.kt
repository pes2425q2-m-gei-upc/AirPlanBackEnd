package org.example.routes

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.just
import io.mockk.Runs
import io.mockk.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.example.database.*
import org.example.websocket.WebSocketManager
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import kotlin.test.assertEquals
import org.example.controllers.ControladorInvitacions
import org.example.repositories.InvitacioRepository
import org.example.repositories.ParticipantsActivitatsRepository
import org.example.repositories.UsuarioRepository
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InvitacioRoutesTest {

    private lateinit var database: Database

    @BeforeAll
    fun setupDatabase() {
        database = Database.connect(
            "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=FALSE",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )
        transaction(database) {
            SchemaUtils.create(
                ActivitatTable,
                InvitacioTable,
                ParticipantsActivitatsTable,
                UsuarioTable
            )
        }
    }

    @BeforeEach
    fun setup() {
        transaction(database) {
            SchemaUtils.drop(
                ActivitatTable,
                InvitacioTable,
                ParticipantsActivitatsTable,
                UsuarioTable
            )
            SchemaUtils.create(
                ActivitatTable,
                InvitacioTable,
                ParticipantsActivitatsTable,
                UsuarioTable
            )
        }
    }

    private fun insertTestData() {
        transaction {
            UsuarioTable.insert {
                it[username] = "user1"
                it[nom] = "User One"
                it[email] = "user1@example.com"
                it[idioma] = "en"
                it[sesionIniciada] = false
                it[isAdmin] = false
                it[esExtern] = false
                it[photourl] = null
            }
            UsuarioTable.insert {
                it[username] = "user2"
                it[nom] = "User Two"
                it[email] = "user2@example.com"
                it[idioma] = "en"
                it[sesionIniciada] = false
                it[isAdmin] = false
                it[esExtern] = false
                it[photourl] = null
            }
            ActivitatTable.insert {
                it[id_activitat] = 1
                it[nom] = "Test Activity"
                it[descripcio] = "Description"
                it[latitud] = 41.40338f
                it[longitud] = 2.17403f
                it[dataInici] = kotlinx.datetime.LocalDateTime(2024, 5, 1, 10, 0)
                it[dataFi] = kotlinx.datetime.LocalDateTime(2024, 5, 1, 18, 0)
                it[username_creador] = "user1"
            }
        }
    }

    @Test
    fun `test get invitacions for a user`() = testApplication {
        insertTestData()
        application {
            install(ContentNegotiation) { json() }
            routing { invitacioRoutes() }
        }

        val response = client.get("/api/invitacions/user1")
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("[]")) // Expecting an empty list initially
    }

    @Test
    fun `test create invitacio with mocked dependencies`() = testApplication {
        insertTestData()

        // Crear mocks de todos los repositorios
        val mockParticipantsRepo = mockk<ParticipantsActivitatsRepository>()
        val mockInvitacionsRepo = mockk<InvitacioRepository>()
        val mockUsuarioRepo = mockk<UsuarioRepository>()
        val mockWebSocketManager = mockk<WebSocketManager>()

        // Configurar comportamiento de los mocks
        every { mockUsuarioRepo.existeUsuario("user2") } returns true
        // Para el repositorio de invitaciones (función no suspendida)
        every { mockInvitacionsRepo.afegirInvitacio(any()) } returns true

        coEvery {
            mockWebSocketManager.notifyRealTimeEvent(any(), any(), any(), any())
        } just Runs

        // Crear controlador con mocks
        val controlador = ControladorInvitacions(
            mockParticipantsRepo,
            mockInvitacionsRepo,
            mockUsuarioRepo,
            mockWebSocketManager
        )

        application {
            install(ContentNegotiation) { json() }
            routing {
                post("/api/invitacions/invitar") {
                    val request = call.receive<Map<String, String>>()
                    val activityId = request["activityId"]?.toIntOrNull()
                    val creator = request["creator"]
                    val username = request["username"]

                    if (activityId == null || creator.isNullOrBlank() || username.isNullOrBlank()) {
                        call.respond(HttpStatusCode.BadRequest, "Faltan datos requeridos")
                        return@post
                    }

                    val success = controlador.crearInvitacio(
                        activityId,
                        creator,
                        username
                    )

                    if (success) {
                        call.respond(HttpStatusCode.Created, "Invitación creada correctamente")
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "Error al crear invitación")
                    }
                }
            }
        }

        val response = client.post("/api/invitacions/invitar") {
            contentType(ContentType.Application.Json)
            setBody(
                """
            {
                "activityId": 1,
                "creator": "user1",
                "username": "user2"
            }
            """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals("Invitación creada correctamente", response.bodyAsText())

        // Verificar que se llamaron los métodos esperados
        verify(exactly = 1) { mockUsuarioRepo.existeUsuario("user2") }
        verify(exactly = 1) { mockInvitacionsRepo.afegirInvitacio(any()) }
        coVerify(exactly = 1) {
            mockWebSocketManager.notifyRealTimeEvent(
                username = "user2",
                message = "1,user1",
                clientId = null,
                type = "INVITACIONS"
            )
        }
    }

    @Test
    fun `test accept invitacio`() = testApplication {
        insertTestData()
        transaction {
            InvitacioTable.insert {
                it[id_activitat] = 1
                it[username_anfitrio] = "user1"
                it[username_convidat] = "user2"
            }
        }
        application {
            install(ContentNegotiation) { json() }
            routing { invitacioRoutes() }
        }

        val response = client.post("/api/invitacions/acceptar") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "activityId": 1,
                    "username": "user2"
                }
                """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Invitación aceptada", response.bodyAsText())
    }

    @Test
    fun `test reject invitacio`() = testApplication {
        insertTestData()
        transaction {
            InvitacioTable.insert {
                it[id_activitat] = 1
                it[username_anfitrio] = "user1"
                it[username_convidat] = "user2"
            }
        }
        application {
            install(ContentNegotiation) { json() }
            routing { invitacioRoutes() }
        }

        val response = client.post("/api/invitacions/rebutjar") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "activityId": 1,
                    "username": "user2"
                }
                """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Invitación rechazada", response.bodyAsText())
    }

    @Test
    fun `test check invitacio`() = testApplication {
        insertTestData()
        transaction {
            InvitacioTable.insert {
                it[id_activitat] = 1
                it[username_anfitrio] = "user1"
                it[username_convidat] = "user2"
            }
        }
        application {
            install(ContentNegotiation) { json() }
            routing { invitacioRoutes() }
        }

        val response = client.get("/api/invitacions/check/1/user2")
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertTrue(responseBody["hasInvitation"]?.toString()?.toBoolean() == true)
    }
}