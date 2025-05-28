import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.example.controllers.ControladorReport
import org.example.controllers.ControladorReport.ReportRequest
import org.example.database.ReportTable
import org.example.database.UsuarioTable
import org.example.models.Report
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.*

class ControladorReportTest {

    @BeforeTest
    fun setupDatabase() {
        Database.connect(
            "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=FALSE",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )
        transaction {
            SchemaUtils.create(UsuarioTable, ReportTable)
        }
    }

    @AfterTest
    fun tearDownDatabase() {
        transaction {
            SchemaUtils.drop(UsuarioTable, ReportTable)
        }
    }

    private fun Application.testModule() {
        install(ContentNegotiation) {
            json(Json { prettyPrint = true })
        }
        routing {
            val controladorReport = ControladorReport()
            post("/api/report") { controladorReport.createReport(call) }
            delete("/api/report") { controladorReport.deleteReport(call) }
            get("/api/report") { controladorReport.getReports(call) }
        }
    }

    @Test
    fun `test create report`() = testApplication {
        application { testModule() }

        transaction {
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

        val reportRequest = ReportRequest("user1", "user2", "Spam")
        val requestBody = Json.encodeToString(ReportRequest.serializer(), reportRequest)

        client.post("/api/report") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("Report created successfully", bodyAsText())
        }
    }

    @Test
    fun `test delete report`() = testApplication {
        application { testModule() }

        transaction {
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

        val reportRequest = ReportRequest("user1", "user2", "Spam")
        transaction {
            ReportTable.insert {
                it[reporterUsername] = reportRequest.reporterUsername
                it[reportedUsername] = reportRequest.reportedUsername
                it[reason] = reportRequest.reason
            }
        }

        val requestBody = Json.encodeToString(ReportRequest.serializer(), reportRequest)

        client.delete("/api/report") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Report deleted successfully", bodyAsText())
        }
    }

    @Test
    fun `test get reports`() = testApplication {
        application { testModule() }

        transaction {
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

        transaction {
            ReportTable.insert {
                it[reporterUsername] = "user1"
                it[reportedUsername] = "user2"
                it[reason] = "Spam"
            }
        }

        client.get("/api/report").apply {
            assertEquals(HttpStatusCode.OK, status)
            val reports = Json.decodeFromString<List<Report>>(bodyAsText())
            assertEquals(1, reports.size)
            assertEquals("user1", reports[0].reporterUsername)
            assertEquals("user2", reports[0].reportedUsername)
            assertEquals("Spam", reports[0].reason)
        }
    }
}