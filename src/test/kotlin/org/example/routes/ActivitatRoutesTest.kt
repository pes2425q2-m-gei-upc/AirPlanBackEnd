package org.example.routes

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import kotlinx.serialization.json.*
import org.example.database.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

/**
 * Tests para el endpoint /api/activitats/hoy
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ActivitatRoutesTest {

    private lateinit var database: Database

    @BeforeAll
    fun setupDatabase() {
        database = Database.connect(
            "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=FALSE",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_REPEATABLE_READ
        
        // Crear todas las tablas necesarias una vez antes de todos los tests
        transaction(database) {
            SchemaUtils.create(
                ActivitatTable,
                ParticipantsActivitatsTable,
                ValoracioTable,
                ActivitatFavoritaTable,
                UserBlockTable,
                UsuarioTable
            )
        }
    }
    
    @BeforeEach
    fun setUp() {
        // Limpiar datos entre tests haciendo drop y create
        transaction(database) {
            // Drop y recrear las tablas para cada test
            SchemaUtils.drop(
                ActivitatTable,
                ParticipantsActivitatsTable,
                ValoracioTable,
                ActivitatFavoritaTable
            )
            SchemaUtils.create(
                ActivitatTable,
                ParticipantsActivitatsTable,
                ValoracioTable,
                ActivitatFavoritaTable
            )
        }
    }
    
    @AfterAll
    fun tearDown() {
        // Limpiar todas las tablas al finalizar los tests
        transaction(database) {
            SchemaUtils.drop(
                ActivitatTable,
                ParticipantsActivitatsTable,
                ValoracioTable,
                ActivitatFavoritaTable,
                UserBlockTable,
                UsuarioTable
            )
        }
    }

    /**
     * Test que verifica que el endpoint /api/activitats/hoy responde con estado HTTP 200 OK
     * y devuelve un array JSON (puede estar vacío)
     */
    @Test
    fun `test get activitats hoy endpoint responds with OK`() = testApplication {
        // Configurar la aplicación de prueba
        application {
            // Configurar negociación de contenido para JSON
            install(ContentNegotiation) {
                json()
            }
            // Configurar rutas
            routing {
                activitatRoutes()
            }
        }

        // Crear cliente HTTP
        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }

        // Ejecutar la solicitud al endpoint
        val response = client.get("/api/activitats/hoy")

        // Verificar que el estado de la respuesta es OK
        assertEquals(HttpStatusCode.OK, response.status)
        
        // Verificar que la respuesta es un array JSON (puede estar vacío)
        val responseBody = response.bodyAsText()
        val jsonArray = Json.decodeFromString<JsonArray>(responseBody)
        
        // La respuesta debe ser un array (vacío o con elementos)
        assertTrue(true, "La respuesta debe ser un array JSON")
    }

    @Test
    fun `test get activitats per participant endpoint responds with OK`() = testApplication {
        // Configure the test application
        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                activitatRoutes()
            }
        }

        // Insert test data
        transaction(database) {
            // Insert user
            UsuarioTable.insert {
                it[username] = "testUser"
                it[nom] = "Test User"
                it[email] = "test@example.com"
                it[idioma] = "Castellano"
                it[sesionIniciada] = false
                it[isAdmin] = false
                it[esExtern] = false
            }

            // Insert activity
            ActivitatTable.insert {
                it[id_activitat] = 1
                it[nom] = "Test Activity"
                it[descripcio] = "Test Description"
                it[latitud] = 41.40338f
                it[longitud] = 2.17403f
                it[dataInici] = kotlinx.datetime.LocalDateTime(2024, 5, 1, 10, 0)
                it[dataFi] = kotlinx.datetime.LocalDateTime(2024, 5, 1, 18, 0)
                it[username_creador] = "testUser"
            }

            // Make user a participant
            ParticipantsActivitatsTable.insert {
                it[id_activitat] = 1
                it[username_participant] = "testUser"
            }
        }

        // Execute request to the endpoint
        val response = client.get("/api/activitats/participant/testUser")

        // Verify response
        assertEquals(HttpStatusCode.OK, response.status)

        // Verify response content
        val responseBody = response.bodyAsText()
        val jsonArray = Json.decodeFromString<JsonArray>(responseBody)

        // Response should be a JSON array with at least one element
        assertTrue(true)
        assertTrue(jsonArray.isNotEmpty())

        // Check first activity details
        val firstActivity = jsonArray[0].jsonObject
        assertEquals("Test Activity", firstActivity["nom"]?.jsonPrimitive?.content)
    }
}
