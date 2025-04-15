import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.testing.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.routing.*
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import org.example.models.Valoracio
import org.example.models.ValoracioInput
import org.example.routes.valoracioRoutes
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValoracioTest {
    // Mock del repositorio para pruebas
    private val mockRepository = MockValoracioRepository()

    @Test
    fun testAfegirValoracio() = testApplication {
        configureTestApplication()

        // Preparar datos de prueba
        val valoracioInput = ValoracioInput(
            username = "testUser",
            idActivitat = 1,
            valoracion = 5,
            comentario = "Excelente actividad"
        )

        // Enviar solicitud POST
        val response = client.post("/valoracions") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(ValoracioInput.serializer(), valoracioInput))
        }

        // Verificar respuesta
        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(mockRepository.valoracioAfegida)
    }

    @Test
    fun testValoracionsPerUsuari() = testApplication {
        configureTestApplication()

        // Configurar datos de prueba en el repositorio mock
        val username = "testUser"
        mockRepository.valoracionsPerUsuari = listOf(
            Valoracio(
                username = username,
                idActivitat = 1,
                valoracion = 5,
                comentario = "Excelente actividad",
                fechaValoracion = LocalDateTime(2023, 5, 15, 10, 30)
            )
        )

        // Enviar solicitud GET
        val response = client.get("/valoracions/usuari/$username")

        // Verificar respuesta
        assertEquals(HttpStatusCode.OK, response.status)
        val responseText = response.bodyAsText()
        assertTrue(responseText.contains("testUser"))
        assertTrue(responseText.contains("Excelente actividad"))
    }

    @Test
    fun testValoracionsPerActivitat() = testApplication {
        configureTestApplication()

        // Configurar datos de prueba en el repositorio mock
        val idActivitat = 1
        mockRepository.valoracionsPerActivitat = listOf(
            Valoracio(
                username = "user1",
                idActivitat = idActivitat,
                valoracion = 4,
                comentario = "Buena actividad",
                fechaValoracion = LocalDateTime(2023, 5, 15, 10, 30)
            ),
            Valoracio(
                username = "user2",
                idActivitat = idActivitat,
                valoracion = 5,
                comentario = "Excelente",
                fechaValoracion = LocalDateTime(2023, 5, 16, 14, 20)
            )
        )

        // Enviar solicitud GET
        val response = client.get("/valoracions/activitat/$idActivitat")

        // Verificar respuesta
        assertEquals(HttpStatusCode.OK, response.status)
        val responseText = response.bodyAsText()
        assertTrue(responseText.contains("user1"))
        assertTrue(responseText.contains("user2"))
        assertTrue(responseText.contains("Buena actividad"))
        assertTrue(responseText.contains("Excelente"))
    }

    // Método auxiliar para configurar la aplicación de prueba
    private fun ApplicationTestBuilder.configureTestApplication() {
        application {
            install(ContentNegotiation) {
                json()
            }
            install(CORS) {
                anyHost()
            }
            routing {
                // Aquí inyectamos nuestro controlador con el repositorio mock
                route("") {
                    val controlador = ControladorValoracio(mockRepository)
                    route("/valoracions") {
                        post {
                            controlador.afegirValoracio(call)
                        }
                        get("/usuari/{username}") {
                            controlador.valoracionsPerUsuari(call)
                        }
                        get("/activitat/{idActivitat}") {
                            controlador.valoracionsPerActivitat(call)
                        }
                    }
                }
            }
        }
    }
}

// Repositorio mock para pruebas
class MockValoracioRepository : ValoracioRepository() {
    var valoracioAfegida = false
    var valoracionsPerUsuari: List<Valoracio> = emptyList()
    var valoracionsPerActivitat: List<Valoracio> = emptyList()

    override fun afegirValoracio(input: ValoracioInput): Boolean {
        valoracioAfegida = true
        return true
    }

    override fun obtenirValoracionsPerUsuari(username: String): List<Valoracio> {
        return valoracionsPerUsuari
    }

    override fun obtenirValoracionsPerActivitat(idActivitat: Int): List<Valoracio> {
        return valoracionsPerActivitat
    }
}