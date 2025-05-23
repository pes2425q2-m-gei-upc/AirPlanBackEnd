package org.example.controllers

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import org.example.repositories.ReportRepository

class ControladorReport(private val reportRepository: ReportRepository = ReportRepository()) {

    @Serializable
    data class ReportRequest(val reporterUsername: String, val reportedUsername: String, val reason: String)

    suspend fun createReport(call: ApplicationCall) {
        try {
            val request = call.receive<ReportRequest>()

            val exists = reportRepository.reportExists(request.reporterUsername, request.reportedUsername)
            if (exists) {
                return call.respond(HttpStatusCode.Conflict, "Report already exists")
            }

            val success = reportRepository.createReport(request.reporterUsername, request.reportedUsername, request.reason)

            if (success) {
                call.respond(HttpStatusCode.Created, "Report created successfully")
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Failed to create report")
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, "Invalid report request: ${e.message}")
        }
    }

    suspend fun deleteReport(call: ApplicationCall) {
        println("Deleting report con los siguientes datos:")
        println("${call.request.queryParameters}")
        try {
            val request = call.receive<ReportRequest>()
            val success = reportRepository.deleteReport(request.reporterUsername, request.reportedUsername)

            if (success) {
                call.respond(HttpStatusCode.OK, "Report deleted successfully")
            } else {
                call.respond(HttpStatusCode.NotFound, "Report not found")
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, "Invalid delete request: ${e.message}")
        }
    }

    suspend fun getReports(call: ApplicationCall) {
        try {
            val reports = reportRepository.getAllReports()
            call.respond(HttpStatusCode.OK, reports)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve reports: ${e.message}")
        }
    }
}