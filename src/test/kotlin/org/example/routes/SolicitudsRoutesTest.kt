package org.example.routes

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.example.controllers.ControladorSolicitudsUnio
import org.example.database.SolicitudsTable
import org.example.database.ActivitatTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.*

class SolicitudsRoutesTest {

    @BeforeTest
    fun setupDatabase() {
        Database.connect(
            "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=FALSE",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )
        transaction {
            SchemaUtils.create(SolicitudsTable, ActivitatTable)
        }
    }

    @AfterTest
    fun tearDownDatabase() {
        transaction {
            SchemaUtils.drop(SolicitudsTable, ActivitatTable)
        }
    }

    @Test
    fun `test POST enviarSolicitud`() = testApplication {
        application {
            routing { solicitudRoutes() }
        }

        val response = client.post("/api/solicituds/host/user/1")
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `test GET obtenirSolicitudsUsuari`() = testApplication {
        application {
            routing { solicitudRoutes() }
        }

        transaction {
            SolicitudsTable.insert {
                it[usernameAnfitrio] = "host"
                it[usernameSolicitant] = "user"
                it[idActivitat] = 1
            }
        }

        val response = client.get("/api/solicituds/user")
        assertEquals(HttpStatusCode.OK, response.status)
    }
}