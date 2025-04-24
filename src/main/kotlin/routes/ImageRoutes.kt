package org.example.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.http.content.*
import java.io.File
import java.util.UUID

// Add optional parameter for upload directory path, defaulting to "uploads"
fun Route.uploadImageRoute(uploadDirPath: String = "uploads") {
    // Ensure the uploads directory exists
    val uploadsDir = File(uploadDirPath).apply {
        if (!exists()) mkdirs()
    }
    
    post("/api/uploadImage") {
        try {
            val multipart = call.receiveMultipart()
            var savedFileName: String? = null // Store the saved filename

            multipart.forEachPart { part ->
                if (part is PartData.FileItem && part.name == "image") { // Check part name
                    val originalFileName = part.originalFileName ?: "unnamed"
                    // Sanitize filename to prevent directory traversal
                    val sanitizedFileName = originalFileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
                    val uniqueFileName = "${UUID.randomUUID()}_$sanitizedFileName"
                    val file = File(uploadsDir, uniqueFileName)

                    part.streamProvider().use { inputStream ->
                        file.outputStream().buffered().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    savedFileName = uniqueFileName // Store the actual saved name
                    call.application.log.info("File saved successfully: ${file.absolutePath}")
                }
                part.dispose()
            }

            if (savedFileName != null) {
                // Construct URL relative to the server base
                val relativeImageUrl = "/uploads/$savedFileName"
                // Respond with JSON containing the URL
                call.respond(HttpStatusCode.OK, mapOf("imageUrl" to relativeImageUrl))
            } else {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No image file found in 'image' part"))
            }
        } catch (e: Exception) {
            call.application.log.error("Error uploading file", e) // Log the error
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error uploading file: ${e.message}"))
        }
    }
}