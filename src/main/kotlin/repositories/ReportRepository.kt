package org.example.repositories

import org.example.database.ReportTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.example.models.Report

class ReportRepository {

    fun createReport(reporterUsername: String, reportedUsername: String, reason: String): Boolean {
        return try {
            transaction {
                ReportTable.insert {
                    it[ReportTable.reporterUsername] = reporterUsername
                    it[ReportTable.reportedUsername] = reportedUsername
                    it[ReportTable.reason] = reason
                }
            }
            true
        } catch (e: Exception) {
            println("❌ Error creating report: ${e.message}")
            false
        }
    }

    fun deleteReport(reporterUsername: String, reportedUsername: String): Boolean {
        return try {
            transaction {
                ReportTable.deleteWhere {
                    (ReportTable.reporterUsername eq reporterUsername) and
                            (ReportTable.reportedUsername eq reportedUsername)
                }
            } > 0
        } catch (e: Exception) {
            println("❌ Error deleting report: ${e.message}")
            false
        }
    }

    fun reportExists(reporterUsername: String, reportedUsername: String): Boolean {
        return transaction {
            ReportTable.select {
                (ReportTable.reporterUsername eq reporterUsername) and
                        (ReportTable.reportedUsername eq reportedUsername)
            }.count() > 0
        }
    }

    fun getAllReports(): List<Report> {
        return transaction {
            ReportTable.selectAll().map {
                Report(
                    reporterUsername = it[ReportTable.reporterUsername],
                    reportedUsername = it[ReportTable.reportedUsername],
                    reason = it[ReportTable.reason],
                    timestamp = it[ReportTable.timestamp].toString() // Convert to LocalDateTime
                )
            }
        }
    }
}