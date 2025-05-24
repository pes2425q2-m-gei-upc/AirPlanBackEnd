package org.example.repositories

import kotlinx.datetime.LocalDateTime
import org.example.database.ActivitatTable
import org.example.models.Activitat
import org.example.models.Localitzacio
import repositories.ActivitatRepository
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ActivitatRepositoryTest {

    private lateinit var repository: ActivitatRepository
    private lateinit var database: Database

    @BeforeAll
    fun setUpDatabase() {
        // Connect to an in-memory H2 database
        database = Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver", user = "sa",
            password = "")
        transaction {
            SchemaUtils.create(ActivitatTable) // Create the schema
        }
        repository = ActivitatRepository()
    }

    @BeforeEach
    fun cleanDatabase() {
        transaction (database){
            SchemaUtils.drop(ActivitatTable)
            SchemaUtils.create(ActivitatTable)
        }
    }

    @AfterEach
    fun tearDown() {
        transaction (database){
            SchemaUtils.drop(ActivitatTable)
        }
    }

    

    @Test
    fun `test afegirActivitat`() {
        val activitat = Activitat(
            id = 0,
            nom = "Excursió",
            descripcio = "Excursió a la muntanya",
            ubicacio = Localitzacio(41.40338f, 2.17403f),
            dataInici = LocalDateTime(2024, 5, 1, 10, 0),
            dataFi = LocalDateTime(2024, 5, 1, 18, 0),
            creador = "anfitrioUser"
        )

        val id = repository.afegirActivitat(activitat)
        assertNotNull(id)
    }

    @Test
    fun `test obtenirActivitats`() {
        val activitat = Activitat(
            id = 0,
            nom = "Excursió",
            descripcio = "Excursió a la muntanya",
            ubicacio = Localitzacio(41.40338f, 2.17403f),
            dataInici = LocalDateTime(2024, 5, 1, 10, 0),
            dataFi = LocalDateTime(2024, 5, 1, 18, 0),
            creador = "anfitrioUser"
        )
        repository.afegirActivitat(activitat)

        val activitats = repository.obtenirActivitats()
        assertEquals(1, activitats.size)
        assertEquals("Excursió", activitats[0].nom)
    }

    @Test
    fun `test eliminarActividad`() {
        val activitat = Activitat(
            id = 0,
            nom = "Excursió",
            descripcio = "Excursió a la muntanya",
            ubicacio = Localitzacio(41.40338f, 2.17403f),
            dataInici = LocalDateTime(2024, 5, 1, 10, 0),
            dataFi = LocalDateTime(2024, 5, 1, 18, 0),
            creador = "anfitrioUser"
        )
        val id = repository.afegirActivitat(activitat)

        val result = repository.eliminarActividad(id!!)
        assertTrue(result)
    }

    @Test
    fun `test obtenirActivitatsExcluintUsuaris`() {
        val activitat = Activitat(
            id = 0,
            nom = "Excursió",
            descripcio = "Excursió a la muntanya",
            ubicacio = Localitzacio(41.40338f, 2.17403f),
            dataInici = LocalDateTime(2024, 5, 1, 10, 0),
            dataFi = LocalDateTime(2024, 5, 1, 18, 0),
            creador = "blockedUser"
        )
        repository.afegirActivitat(activitat)

        val activitats = repository.obtenirActivitatsExcluintUsuaris(listOf("blockedUser"))
        assertTrue(activitats.isEmpty())
    }

    @Test
    fun `test modificarActivitat`() {
        val activitat = Activitat(
            id = 0,
            nom = "Excursió",
            descripcio = "Excursió a la muntanya",
            ubicacio = Localitzacio(41.40338f, 2.17403f),
            dataInici = LocalDateTime(2024, 5, 1, 10, 0),
            dataFi = LocalDateTime(2024, 5, 1, 18, 0),
            creador = "anfitrioUser"
        )
        val id = repository.afegirActivitat(activitat)

        val updated = repository.modificarActivitat(
            id!!,
            "Nova Excursió",
            "Excursió actualitzada",
            Localitzacio(40.0f, 3.0f),
            LocalDateTime(2024, 6, 1, 10, 0),
            LocalDateTime(2024, 6, 1, 18, 0)
        )
        assertTrue(updated)

        val updatedActivitat = repository.getActivitatPerId(id)
        assertEquals("Nova Excursió", updatedActivitat.nom)
    }

    @Test
    fun `test getActivitatPerId`() {
        val activitat = Activitat(
            id = 0,
            nom = "Excursió",
            descripcio = "Excursió a la muntanya",
            ubicacio = Localitzacio(41.40338f, 2.17403f),
            dataInici = LocalDateTime(2024, 5, 1, 10, 0),
            dataFi = LocalDateTime(2024, 5, 1, 18, 0),
            creador = "anfitrioUser"
        )
        val id = repository.afegirActivitat(activitat)

        val fetchedActivitat = repository.getActivitatPerId(id!!)
        assertEquals("Excursió", fetchedActivitat.nom)
    }
}