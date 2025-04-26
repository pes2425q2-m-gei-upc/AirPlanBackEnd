package org.example.routes

import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.serialization.kotlinx.json.* // Import JSON serialization
import io.ktor.server.plugins.contentnegotiation.* // Import ContentNegotiation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.example.routes.uploadImageRoute
import io.ktor.http.content.*
import java.io.File
import java.nio.file.Files
import java.util.UUID
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import io.ktor.utils.io.streams.asInput // Import for asInput

class ImageRoutesTest {

    private val uploadDir = File("uploads_test")
    private val testFileName = "test_image.png"
    private val testFile = File(uploadDir, testFileName)

    @BeforeEach
    fun setup() {
        // Create a temporary upload directory for tests
        if (!uploadDir.exists()) {
            uploadDir.mkdirs()
        }
        // Create a dummy file to upload
        testFile.createNewFile()
        testFile.writeBytes(byteArrayOf(1, 2, 3)) // Write some dummy content
    }

    @AfterEach
    fun tearDown() {
        // Clean up the temporary directory and file after tests
        if (uploadDir.exists()) {
            uploadDir.deleteRecursively()
        }
    }

    @Test
    fun `test upload image success`() {
        withTestApplication({ 
            // Install ContentNegotiation to handle JSON responses
            install(ContentNegotiation) {
                json()
            }
            // Configure routing within the test application
            routing {
                uploadImageRoute(uploadDirPath = uploadDir.path) // Pass test upload dir
            }
        }) {
            // Simulate a multipart form data request with a file part
            handleRequest(HttpMethod.Post, "/api/uploadImage") {
                val boundary = "WebAppBoundary"
                addHeader(HttpHeaders.ContentType, ContentType.MultiPart.FormData.withParameter("boundary", boundary).toString())
                setBody(boundary, listOf(
                    PartData.FileItem(
                        { testFile.inputStream().asInput() }, // Use imported asInput
                        {}, // No need to dispose manually here
                        headersOf(
                            HttpHeaders.ContentDisposition, 
                            ContentDisposition.File.withParameter(ContentDisposition.Parameters.Name, "image").withParameter(ContentDisposition.Parameters.FileName, testFileName).toString()
                        )
                    )
                ))
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status(), "Response status should be OK for successful upload") // Reverted to status()
                assertNotNull(response.content, "Response content should not be null")
                assertTrue(response.content!!.contains("\"imageUrl\":"), "Response should contain the image URL")
                // Verify the file was actually saved in the test directory (check for *any* saved file)
                val savedFiles = uploadDir.listFiles { file -> file.isFile }
                assertTrue(savedFiles?.isNotEmpty() ?: false, "An image file should be saved in the test upload directory")
                // Optional: More specific check if the filename pattern is known
                // assertTrue(savedFiles?.any { it.name.endsWith(testFileName) } ?: false, "Saved file should have the original extension")
            }
        }
    }

    @Test
    fun `test upload image with no file part`() {
        withTestApplication({ 
            // Install ContentNegotiation to handle JSON responses
            install(ContentNegotiation) {
                json()
            }
            routing {
                uploadImageRoute(uploadDirPath = uploadDir.path) // Pass test upload dir
            }
        }) {
            handleRequest(HttpMethod.Post, "/api/uploadImage") {
                // Send multipart request but without the actual file part
                val boundary = "WebAppBoundary"
                addHeader(HttpHeaders.ContentType, ContentType.MultiPart.FormData.withParameter("boundary", boundary).toString())
                setBody(boundary, listOf(
                    PartData.FormItem("sometext", {}, headersOf()) // Send some other form item
                ))
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status(), "Response status should be BadRequest when no file part is provided") // Reverted to status()
                // Optionally check error message in response content
                // assertTrue(response.content?.contains("No image file found") ?: false)
            }
        }
    }

    @Test
    fun `test upload image with incorrect part name`() {
        withTestApplication({ 
            // Install ContentNegotiation to handle JSON responses
            install(ContentNegotiation) {
                json()
            }
            routing {
                uploadImageRoute(uploadDirPath = uploadDir.path) // Pass test upload dir
            }
        }) {
            handleRequest(HttpMethod.Post, "/api/uploadImage") {
                val boundary = "WebAppBoundary"
                addHeader(HttpHeaders.ContentType, ContentType.MultiPart.FormData.withParameter("boundary", boundary).toString())
                setBody(boundary, listOf(
                    PartData.FileItem(
                        { testFile.inputStream().asInput() }, // Use imported asInput
                        {},
                        headersOf(
                            HttpHeaders.ContentDisposition, 
                            // Use a name other than "image"
                            ContentDisposition.File.withParameter(ContentDisposition.Parameters.Name, "wrongpartname").withParameter(ContentDisposition.Parameters.FileName, testFileName).toString()
                        )
                    )
                ))
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status(), "Response status should be BadRequest when part name is incorrect") // Reverted to status()
            }
        }
    }

    @Test
    fun `test upload image route with non-POST method`() {
        withTestApplication({ 
            // Install ContentNegotiation to handle JSON responses
            install(ContentNegotiation) {
                json()
            }
            routing {
                uploadImageRoute(uploadDirPath = uploadDir.path) // Pass test upload dir
            }
        }) {
            handleRequest(HttpMethod.Get, "/api/uploadImage").apply {
                val status = response.status() // Guardar el estado en una variable local
                
                // Aseguramos que status no sea nulo
                assertNotNull(status, "La respuesta debe tener un código de estado")
                
                // Continuamos con el resto de aserciones solo si status no es nulo
                if (status != null) {
                    assertNotEquals(HttpStatusCode.OK, status, "GET no debería devolver OK")
                    
                    // Verificamos que sea un código de error de cliente
                    assertTrue(
                        status.value >= 400 && status.value < 500,
                        "GET request to POST route should result in a client error status code (4xx)"
                    )
                }
            }
        }
    }

    // Removed the old test `test upload image path creation` as setup/teardown handles directory
    // Removed the old test `test upload image with no file` as `test upload image with no file part` is more specific
}