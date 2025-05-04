package org.example.routes

import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.example.controllers.ControladorRuta
import org.example.database.RutaTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import junit.framework.TestCase.assertEquals
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.encodeToString
import org.example.enums.TipusVehicle
import org.example.models.Localitzacio
import org.example.models.Ruta
import java.sql.Connection
import io.ktor.server.plugins.contentnegotiation.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RutaRoutesTest {

    private lateinit var rutaController: ControladorRuta
    private lateinit var database: Database

    @BeforeAll
    fun setupDatabase() {
        database = Database.connect(
            "jdbc:h2:mem:test_ruta_routes;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=FALSE",
            driver = "org.h2.Driver",
            user = "test",
            password = ""
        )
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_REPEATABLE_READ
    }

    @BeforeEach
    fun setUp() {
        transaction(database) {
            SchemaUtils.drop(RutaTable)
            SchemaUtils.create(RutaTable)
        }
        rutaController = mockk()
    }

    @AfterEach
    fun tearDown() {
        transaction(database) {
            SchemaUtils.drop(RutaTable)
        }
    }

    @Test
    fun creatingValidRutaReturnsCreatedStatus() = testApplication {
        application {
            // Add JSON content negotiation configuration
            this@application.install(ContentNegotiation) {
                json()
            }
            routing {
                rutaRoutes(rutaController)
            }
        }

        val jsonBody = Json.encodeToString(
            mapOf(
                "origen" to "41.1,2.1",
                "desti" to "41.2,2.2",
                "clientUsername" to "testuser",
                "data" to "2023-10-01T12:00:00",
                "tipusVehicle" to "Cotxe"
            )
        )

        every { rutaController.crearRuta(any()) } returns Ruta(
            origen = Localitzacio(41.1f, 2.1f),
            desti = Localitzacio(41.2f, 2.2f),
            clientUsername = "testuser",
            data = LocalDateTime(2023, 10, 1, 12, 0),
            id = 1,
            duracioMin = 10,
            duracioMax = 20,
            tipusVehicle = TipusVehicle.Cotxe
        )

        val response = client.post("/api/rutas") {
            contentType(ContentType.Application.Json)
            setBody(jsonBody)
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun deletingExistingRutaReturnsOk() = testApplication {
        application {
            routing {
                rutaRoutes(rutaController)
            }
        }

        every { rutaController.eliminarRuta(1) } returns true

        val response = client.delete("/api/rutas/1")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun gettingRutasForValidUserReturnsOk() = testApplication {
        application {
            // Add JSON content negotiation configuration
            this@application.install(ContentNegotiation) {
                json()
            }
            routing {
                rutaRoutes(rutaController)
            }
        }

        every { rutaController.obtenirTotesRutesClient("testuser") } returns listOf()

        val response = client.get("/api/rutas?username=testuser")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun creatingInvalidRutaReturnsBadRequest() = testApplication {
        application {
            routing {
                rutaRoutes(rutaController)
            }
        }

        val invalidJson = "invalid json"

        val response = client.post("/api/rutas") {
            contentType(ContentType.Application.Json)
            setBody(invalidJson)
        }

        assertEquals(HttpStatusCode.Companion.BadRequest, response.status)
    }

    @Test
    fun deletingNonExistentRutaReturnsNotFound() = testApplication {
        application {
            routing {
                rutaRoutes(rutaController)
            }
        }

        every { rutaController.eliminarRuta(99) } returns false

        val response = client.delete("/api/rutas/99")

        assertEquals(HttpStatusCode.Companion.NotFound, response.status)
    }

    @Test
    fun calculatingPublicTransportRouteReturnsOk() = testApplication {
        application {
            routing {
                rutaRoutes(rutaController)
            }
        }

        val response = client.get("/api/rutas/calculate/publictransport?origin=41.1,2.1&destination=41.2,2.2")

        assertEquals(HttpStatusCode.Companion.OK, response.status)
    }

    @Test
    fun calculatingSimpleRouteReturnsOk() = testApplication {
        application {
            routing {
                rutaRoutes(rutaController)
            }
        }

        val response = client.get("/api/rutas/calculate/simple?origin=41.1,2.1&destination=41.2,2.2")

        assertEquals(HttpStatusCode.Companion.OK, response.status)
    }
}