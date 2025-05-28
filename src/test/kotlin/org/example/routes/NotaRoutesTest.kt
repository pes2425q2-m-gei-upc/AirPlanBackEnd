package org.example.routes

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.database.ClienteTable
import org.example.database.NotesTable
import org.example.database.UsuarioTable
import org.example.models.Nota
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotaRoutesTest {

    private lateinit var database: Database

    @BeforeAll
    fun setupDatabase() {
        database = Database.connect(
            "jdbc:h2:mem:test_notas_${UUID.randomUUID()};DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=FALSE",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )
    }

    @BeforeEach
    fun setup() {
        transaction(database) {
            SchemaUtils.drop(NotesTable, UsuarioTable, ClienteTable)
            SchemaUtils.create(NotesTable, UsuarioTable, ClienteTable)

            // Insert test user if needed
            try {
                SchemaUtils.create(UsuarioTable)
                UsuarioTable.insert {
                    it[username] = "testUser"
                    it[nom] = "Test User"
                    it[email] = "test@example.com"
                    it[idioma] = "Castellano"
                    it[sesionIniciada] = false
                    it[isAdmin] = false
                }
                SchemaUtils.create(ClienteTable)
                ClienteTable.insert {
                    it[username] = "testUser"
                    it[nivell] = 1
                }
            } catch (e: Exception) {
                // Table might already exist
            }
        }
    }

    @AfterEach
    fun tearDown() {
        transaction(database) {
            SchemaUtils.drop(NotesTable, UsuarioTable, ClienteTable)
        }
    }

    @Test
    fun `GET notes should return OK status`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            routing { notaRoutes() }
        }

        val response = client.get("/notas/testUser")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `POST new note should return Created status`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            routing { notaRoutes() }
        }

        val nota = Nota(
            username = "testUser",
            fechaCreacion = LocalDate(2023, 6, 1),
            horaRecordatorio = LocalTime(14, 30),
            comentario = "Test note"
        )

        val jsonNota = Json.encodeToString(nota)

        val response = client.post("/notas") {
            contentType(ContentType.Application.Json)
            setBody(jsonNota)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("correctament"))
    }

    @Test
    fun `PUT update note should return OK status`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            routing { notaRoutes() }
        }

        // First create a note
        transaction(database) {
            NotesTable.insert {
                it[id] = 1
                it[username] = "testUser"
                it[fechaCreacion] = LocalDate(2023, 6, 1)
                it[horaRecordatorio] = LocalTime(14, 30)
                it[comentario] = "Original note"
            }
        }

        val updatedNota = Nota(
            username = "testUser",
            fechaCreacion = LocalDate(2023, 6, 1),
            horaRecordatorio = LocalTime(14, 30),
            comentario = "Updated note"
        )

        val response = client.put("/notas/1") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(updatedNota))
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `DELETE note should return OK status`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            routing { notaRoutes() }
        }

        // First create a note
        transaction(database) {
            NotesTable.insert {
                it[id] = 1
                it[username] = "testUser"
                it[fechaCreacion] = LocalDate(2023, 6, 1)
                it[horaRecordatorio] = LocalTime(14, 30)
                it[comentario] = "Test note to delete"
            }
        }

        val response = client.delete("/notas/1")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `POST invalid JSON should return BadRequest`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            routing { notaRoutes() }
        }

        val response = client.post("/notas") {
            contentType(ContentType.Application.Json)
            setBody("{invalid json}")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}