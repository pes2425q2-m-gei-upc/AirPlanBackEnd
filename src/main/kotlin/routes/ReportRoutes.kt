package org.example.routes

import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.example.controllers.ControladorReport

fun Route.reportRoutes() {
    val reportController = ControladorReport()

    route("/api/report") {
        // Create a report
        post {
            reportController.createReport(call)
        }

        // Delete a report
        delete {
            reportController.deleteReport(call)
        }

        get {
            reportController.getReports(call)
        }
    }
}