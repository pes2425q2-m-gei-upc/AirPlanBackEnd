package org.example.repositories

import org.example.database.ReportTable
import org.example.database.UsuarioTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import java.sql.Connection
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReportRepositoryTest {

    private lateinit var reportRepository: ReportRepository
    private lateinit var database: Database

    @BeforeAll
    fun setupDatabase() {
        database = Database.connect(
            "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=FALSE",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_REPEATABLE_READ
    }

    @BeforeEach
    fun setUp() {
        transaction(database) {
            SchemaUtils.drop(ReportTable, UsuarioTable)
            SchemaUtils.create(UsuarioTable, ReportTable)
        }
        reportRepository = ReportRepository()
    }

    @AfterEach
    fun tearDown() {
        transaction(database) {
            SchemaUtils.drop(ReportTable, UsuarioTable)
        }
    }

    @Test
    @DisplayName("Test crear un reporte")
    fun testCreateReport() {
        transaction(database) {
            UsuarioTable.insert {
                it[username] = "user1"
                it[nom] = "User One"
                it[email] = "user1@example.com"
                it[idioma] = "es"
                it[isAdmin] = false
                it[esExtern] = false
                it[photourl] = null
                it[sesionIniciada] = false
            }
            UsuarioTable.insert {
                it[username] = "user2"
                it[nom] = "User Two"
                it[email] = "user2@example.com"
                it[idioma] = "es"
                it[isAdmin] = false
                it[esExtern] = false
                it[photourl] = null
                it[sesionIniciada] = false
            }
        }

        val result = reportRepository.createReport("user1", "user2", "Spam")
        assertTrue(result)

        val reports = reportRepository.getAllReports()
        assertEquals(1, reports.size)
        val report = reports.first()
        assertEquals("user1", report.reporterUsername)
        assertEquals("user2", report.reportedUsername)
        assertEquals("Spam", report.reason)
    }

    @Test
    @DisplayName("Test eliminar un reporte")
    fun testDeleteReport() {
        transaction(database) {
            UsuarioTable.insert {
                it[username] = "user1"
                it[nom] = "User One"
                it[email] = "user1@example.com"
                it[idioma] = "es"
                it[isAdmin] = false
                it[esExtern] = false
                it[photourl] = null
                it[sesionIniciada] = false
            }
            UsuarioTable.insert {
                it[username] = "user2"
                it[nom] = "User Two"
                it[email] = "user2@example.com"
                it[idioma] = "es"
                it[isAdmin] = false
                it[esExtern] = false
                it[photourl] = null
                it[sesionIniciada] = false
            }
        }

        reportRepository.createReport("user1", "user2", "Spam")
        val deleteResult = reportRepository.deleteReport("user1", "user2")
        assertTrue(deleteResult)

        val reports = reportRepository.getAllReports()
        assertTrue(reports.isEmpty())
    }

    @Test
    @DisplayName("Test verificar si un reporte existe")
    fun testReportExists() {
        transaction(database) {
            UsuarioTable.insert {
                it[username] = "user1"
                it[nom] = "User One"
                it[email] = "user1@example.com"
                it[idioma] = "es"
                it[isAdmin] = false
                it[esExtern] = false
                it[photourl] = null
                it[sesionIniciada] = false
            }
            UsuarioTable.insert {
                it[username] = "user2"
                it[nom] = "User Two"
                it[email] = "user2@example.com"
                it[idioma] = "es"
                it[isAdmin] = false
                it[esExtern] = false
                it[photourl] = null
                it[sesionIniciada] = false
            }
        }

        reportRepository.createReport("user1", "user2", "Spam")
        val exists = reportRepository.reportExists("user1", "user2")
        assertTrue(exists)

        val notExists = reportRepository.reportExists("user3", "user4")
        assertFalse(notExists)
    }

    @Test
    @DisplayName("Test obtener todos los reportes")
    fun testGetAllReports() {
        transaction(database) {
            UsuarioTable.insert {
                it[username] = "user1"
                it[nom] = "User One"
                it[email] = "user1@example.com"
                it[idioma] = "es"
                it[isAdmin] = false
                it[esExtern] = false
                it[photourl] = null
                it[sesionIniciada] = false
            }
            UsuarioTable.insert {
                it[username] = "user2"
                it[nom] = "User Two"
                it[email] = "user2@example.com"
                it[idioma] = "es"
                it[isAdmin] = false
                it[esExtern] = false
                it[photourl] = null
                it[sesionIniciada] = false
            }
            UsuarioTable.insert {
                it[username] = "user3"
                it[nom] = "User Three"
                it[email] = "user3@example.com"
                it[idioma] = "es"
                it[isAdmin] = false
                it[esExtern] = false
                it[photourl] = null
                it[sesionIniciada] = false
            }
            UsuarioTable.insert {
                it[username] = "user4"
                it[nom] = "User Four"
                it[email] = "user4@example.com"
                it[idioma] = "es"
                it[isAdmin] = false
                it[esExtern] = false
                it[photourl] = null
                it[sesionIniciada] = false
            }
        }

        reportRepository.createReport("user1", "user2", "Spam")
        reportRepository.createReport("user3", "user4", "Harassment")

        val reports = reportRepository.getAllReports()
        assertEquals(2, reports.size)

        val firstReport = reports[0]
        assertEquals("user1", firstReport.reporterUsername)
        assertEquals("user2", firstReport.reportedUsername)
        assertEquals("Spam", firstReport.reason)

        val secondReport = reports[1]
        assertEquals("user3", secondReport.reporterUsername)
        assertEquals("user4", secondReport.reportedUsername)
        assertEquals("Harassment", secondReport.reason)
    }
}