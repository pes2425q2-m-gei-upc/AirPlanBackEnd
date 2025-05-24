package org.example.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.utils.notificationRepository
import org.example.controllers.ControladorNotificacions

fun Route.notificacionsRoutes() {
    val controladorNotificacions = ControladorNotificacions(notificationRepository);
    route("/api/notificacions") {
        delete("/{id}") {
            val notificationId = call.parameters["id"]?.toIntOrNull()
            if (notificationId != null) {
                controladorNotificacions.deleteNotification(notificationId)
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.BadRequest, "Invalid notification ID")
            }
        }

        delete("/user/{username}") {
            val username = call.parameters["username"]
            if (username != null) {
                controladorNotificacions.deleteNotificationsUser(username)
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.BadRequest, "Username is required")
            }
        }

        get("/user/{username}") {
            val username = call.parameters["username"]

            if (username != null) {
                try {
                    // Obtener las notificaciones del usuario
                    val notifications = controladorNotificacions.getNotifications(username)

                    if (notifications.isNotEmpty()) {
                        // Responder con las notificaciones en formato JSON
                        call.respond(HttpStatusCode.OK, notifications)
                    } else {
                        // Si no hay notificaciones, respondemos con un 404 Not Found
                        call.respond(HttpStatusCode.NotFound, "No notifications found for user '$username'")
                    }
                } catch (e: Exception) {
                    // Manejo de errores
                    call.respond(HttpStatusCode.InternalServerError, "Error retrieving notifications")
                }
            } else {
                // Si el username no se proporciona, devolvemos un error
                call.respond(HttpStatusCode.BadRequest, "Username is required")
            }
        }
    }
}