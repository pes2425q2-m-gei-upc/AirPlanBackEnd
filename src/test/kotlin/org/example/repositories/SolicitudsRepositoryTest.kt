package repositories

import org.example.database.SolicitudsTable
import org.example.database.ActivitatTable
import org.example.models.Activitat
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue

class SolicitudsRepositoryTest {

    private lateinit var repository: SolicitudRepository

    @Before
    fun setup() {
        Database.connect(
            "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=FALSE",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )
        transaction {
            SchemaUtils.create(SolicitudsTable, ActivitatTable)
        }
        repository = SolicitudRepository()
    }

    @After
    fun tearDown() {
        transaction {
            SchemaUtils.drop(SolicitudsTable, ActivitatTable)
        }
    }

    @Test
    fun `test enviarSolicitud inserts data successfully`() {
        val result = repository.enviarSolicitud("host", "user", 1)
        assertTrue(result)
    }

    @Test
    fun `test obtenirSolicitudesPerUsuari returns activities`() {
        transaction {
            // Insert mock data
            ActivitatTable.insert {
                it[id_activitat] = 1
                it[nom] = "Activity"
                it[descripcio] = "Description"
                it[latitud] = 0.0
                it[longitud] = 0.0
                it[dataInici] = null
                it[dataFi] = null
                it[username_creador] = "creator"
            }
            SolicitudsTable.insert {
                it[usernameAnfitrio] = "host"
                it[usernameSolicitant] = "user"
                it[idActivitat] = 1
            }
        }

        val result = repository.obtenirSolicitudesPerUsuari("user")
        assertTrue(result.isNotEmpty())
    }
}