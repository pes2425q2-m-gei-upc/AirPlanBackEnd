package org.example.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.http.content.*
import java.io.File
import java.util.UUID

fun Route.uploadImageRoute() {
    post("/api/uploadImage") {
        try {
            val multipart = call.receiveMultipart()
            var imageUrl: String? = null

            multipart.forEachPart { part ->
                if (part is PartData.FileItem) {
                    val fileName = "${UUID.randomUUID()}_${part.originalFileName ?: "unnamed"}"
                    val file = File("uploads/$fileName")

                    // Asegurar que el directorio existe
                    file.parentFile.mkdirs()

                    part.streamProvider().use { inputStream ->
                        file.outputStream().buffered().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    imageUrl = "http://localhost:8080/uploads/$fileName"
                }
                part.dispose()
            }

            if (imageUrl != null) {
                call.respond(HttpStatusCode.OK, imageUrl!!)
            } else {
                call.respond(HttpStatusCode.BadRequest, "No file uploaded")
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, "Error uploading file: ${e.message}")
        }
    }
}