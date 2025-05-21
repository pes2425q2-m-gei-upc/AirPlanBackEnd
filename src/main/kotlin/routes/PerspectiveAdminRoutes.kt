package org.example.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.config.PerspectiveCurrentSettings 
import org.example.config.PerspectiveSettingsManager

fun Route.perspectiveAdminRoutes() {
    route("/api/admin/perspective/settings") {
        get {
            try {
                val settings = PerspectiveSettingsManager.getSettings()
                call.respond(HttpStatusCode.OK, settings)
            } catch (e: Exception) {
                application.log.error("Failed to retrieve Perspective settings", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to retrieve Perspective settings: ${'$'}{e.message}"))
            }
        }

        put {
            try {
                val newSettings = call.receive<PerspectiveCurrentSettings>()

                var validRequest = true
                newSettings.attributeSettings.forEach { (attrName, attrSetting) ->
                    if (attrSetting.threshold < 0.0f || attrSetting.threshold > 1.0f) {
                        validRequest = false
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Threshold for attribute '$attrName' must be between 0.0 and 1.0."))
                        return@put 
                    }
                    if (!PerspectiveSettingsManager.supportedAttributes.contains(attrName)) {
                         application.log.warn("Received setting for unsupported attribute '$attrName'. It might be ignored by PerspectiveSettingsManager if not handled there.")
                    }
                }
                if (!validRequest) return@put

                val success = PerspectiveSettingsManager.updateSettings(newSettings)
                if (success) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Perspective settings updated successfully."))
                } else {
                    application.log.error("Failed to update Perspective settings after successful request parsing.")
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to update Perspective settings on the server."))
                }
            } catch (e: ContentTransformationException) {
                application.log.error("Invalid request body for Perspective settings update", e)
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid request body: ${'$'}{e.message}"))
            } catch (e: Exception) {
                application.log.error("Generic error during Perspective settings update", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "An unexpected error occurred: ${'$'}{e.message}"))
            }
        }
    }
}
