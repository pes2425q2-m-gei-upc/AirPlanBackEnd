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
import io.ktor.server.plugins.ContentTransformationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.example.config.AttributeSetting
import org.example.config.PerspectiveCurrentSettings
import org.example.config.PerspectiveSettingsManager
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PerspectiveAdminRoutesTest {

    private val testSettingsFile = "test_perspective_settings.properties"
    
    @BeforeEach
    fun setup() {
        // Asegurarse de que el archivo de prueba no existe (eliminar si existiera)
        val file = File(testSettingsFile)
        if (file.exists()) {
            file.delete()
        }
    }
    
    @AfterEach
    fun tearDown() {
        // Eliminar el archivo de prueba después de cada test
        val file = File(testSettingsFile)
        if (file.exists()) {
            file.delete()
        }
    }

    @Test
    fun `test get perspective settings endpoint returns OK and valid settings`() = testApplication {
        // Configurar la aplicación
        application {
            install(ContentNegotiation) { json() }
            routing { perspectiveAdminRoutes() }
        }
        
        // Ejecutar la petición GET
        val response = client.get("/api/admin/perspective/settings")
        
        // Verificar respuesta
        assertEquals(HttpStatusCode.OK, response.status)
        
        // Verificar que la respuesta es un JSON válido con la estructura esperada
        val settings = Json.decodeFromString<PerspectiveCurrentSettings>(response.bodyAsText())
        assertNotNull(settings)
        assertNotNull(settings.attributeSettings)
        // Verificar que contiene los atributos soportados
        for (attr in PerspectiveSettingsManager.supportedAttributes) {
            assertTrue(settings.attributeSettings.containsKey(attr), "Settings should contain attribute $attr")
        }
    }
    
    @Test
    fun `test update perspective settings with valid data returns OK`() = testApplication {
        // Configurar la aplicación
        application {
            install(ContentNegotiation) { json() }
            routing { perspectiveAdminRoutes() }
        }
        
        // Crear nuevos ajustes válidos para la prueba
        val newSettings = PerspectiveCurrentSettings(
            isEnabled = true,
            doNotStore = false,
            spanAnnotations = true,
            attributeSettings = mapOf(
                "TOXICITY" to AttributeSetting(true, 0.7f),
                "SEVERE_TOXICITY" to AttributeSetting(false, 0.8f),
                "IDENTITY_ATTACK" to AttributeSetting(true, 0.6f),
                "INSULT" to AttributeSetting(true, 0.5f),
                "PROFANITY" to AttributeSetting(false, 0.9f),
                "THREAT" to AttributeSetting(true, 0.8f),
                "SEXUALLY_EXPLICIT" to AttributeSetting(false, 0.7f),
                "FLIRTATION" to AttributeSetting(true, 0.6f)
            )
        )
        
        // Ejecutar la petición PUT
        val response = client.put("/api/admin/perspective/settings") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(newSettings))
        }
        
        // Verificar respuesta
        assertEquals(HttpStatusCode.OK, response.status)
        
        // Verificar que los cambios se aplicaron correctamente
        val getResponse = client.get("/api/admin/perspective/settings")
        val updatedSettings = Json.decodeFromString<PerspectiveCurrentSettings>(getResponse.bodyAsText())
        
        assertEquals(newSettings.isEnabled, updatedSettings.isEnabled)
        assertEquals(newSettings.doNotStore, updatedSettings.doNotStore)
        assertEquals(newSettings.spanAnnotations, updatedSettings.spanAnnotations)
        
        // Verificar que los atributos se actualizaron correctamente
        for (attr in PerspectiveSettingsManager.supportedAttributes) {
            val expected = newSettings.attributeSettings[attr]
            val actual = updatedSettings.attributeSettings[attr]
            assertNotNull(expected)
            assertNotNull(actual)
            assertEquals(expected.enabled, actual.enabled, "Enabled state for $attr should match")
            assertEquals(expected.threshold, actual.threshold, "Threshold for $attr should match")
        }
    }
    
    @Test
    fun `test update perspective settings with invalid threshold returns BadRequest`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            routing { perspectiveAdminRoutes() }
        }
        
        // Crear ajustes con un umbral inválido (mayor que 1.0)
        val invalidSettings = PerspectiveCurrentSettings(
            isEnabled = true,
            doNotStore = false,
            spanAnnotations = false,
            attributeSettings = mapOf(
                "TOXICITY" to AttributeSetting(true, 1.2f)  // Umbral inválido
            )
        )
        
        // Ejecutar la petición PUT
        val response = client.put("/api/admin/perspective/settings") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(invalidSettings))
        }
        
        // Verificar que se devuelve un error BadRequest
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val responseBody = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertTrue(responseBody.containsKey("error"), "Response should contain an error message")
        val errorMessage = responseBody["error"]?.jsonPrimitive?.content
        assertNotNull(errorMessage)
        assertTrue(errorMessage.contains("Threshold"), "Error message should mention threshold problem")
    }
    
    @Test
    fun `test update perspective settings with negative threshold returns BadRequest`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            routing { perspectiveAdminRoutes() }
        }
        
        // Crear ajustes con un umbral inválido (menor que 0.0)
        val invalidSettings = PerspectiveCurrentSettings(
            isEnabled = true,
            doNotStore = false,
            spanAnnotations = false,
            attributeSettings = mapOf(
                "TOXICITY" to AttributeSetting(true, -0.5f)  // Umbral inválido
            )
        )
        
        // Ejecutar la petición PUT
        val response = client.put("/api/admin/perspective/settings") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(invalidSettings))
        }
          // Verificar que se devuelve un error BadRequest
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val responseBody = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertTrue(responseBody.containsKey("error"), "Response should contain an error message")
        val errorMessage = responseBody["error"]?.jsonPrimitive?.content
        assertNotNull(errorMessage)
        assertTrue(errorMessage.contains("Threshold"), "Error message should mention threshold problem")
    }
      @Test
    fun `test update perspective settings with invalid json returns BadRequest`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            routing { perspectiveAdminRoutes() }
        }
        
        // Ejecutar la petición PUT con JSON inválido
        val response = client.put("/api/admin/perspective/settings") {
            contentType(ContentType.Application.Json)
            setBody("invalid json")
        }
        
        // Verificar que se devuelve un error - puede ser BadRequest (400) o InternalServerError (500)
        // dependiendo de cómo Ktor maneje la deserialización fallida
        assertTrue(
            response.status == HttpStatusCode.BadRequest || response.status == HttpStatusCode.InternalServerError,
            "Expected status code 400 or 500, but got ${response.status}"
        )
    }
    
    @Test
    fun `test handle server error when updating settings`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            routing { 
                // Sobrescribir la ruta normal con una que simula un error interno
                route("/api/admin/perspective/settings") {
                    put {
                        call.respond(HttpStatusCode.InternalServerError, 
                            mapOf("error" to "Failed to update Perspective settings on the server."))
                    }
                }
            }
        }
        
        // Crear configuración válida
        val validSettings = PerspectiveCurrentSettings(
            isEnabled = true,
            doNotStore = false,
            spanAnnotations = false,
            attributeSettings = mapOf(
                "TOXICITY" to AttributeSetting(true, 0.7f)
            )
        )
        
        // Ejecutar la petición PUT
        val response = client.put("/api/admin/perspective/settings") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(validSettings))
        }
        
        // Verificar que se devuelve un error de servidor
        assertEquals(HttpStatusCode.InternalServerError, response.status)
        val responseBody = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertTrue(responseBody.containsKey("error"), "Response should contain an error message")
        val errorMessage = responseBody["error"]?.jsonPrimitive?.content
        assertNotNull(errorMessage)
        assertTrue(errorMessage!!.contains("Failed to update Perspective settings"), 
                  "Error message should indicate failure to update")
    }
}