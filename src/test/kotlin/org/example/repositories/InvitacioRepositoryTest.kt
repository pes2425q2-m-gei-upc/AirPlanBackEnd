package org.example.repositories

import org.example.database.ActivitatTable
import org.example.database.InvitacioTable
import org.example.models.Activitat
import org.example.models.Invitacio
import org.example.models.Localitzacio
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InvitacioRepositoryTest {

    private lateinit var repository: InvitacioRepository

    @BeforeAll
    fun setUpDatabase() {
        // Connect to an in-memory H2 database
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.create(ActivitatTable, InvitacioTable) // Create the schema
        }
        repository = InvitacioRepository()
    }

    @BeforeEach
    fun cleanDatabase() {
        transaction {
            SchemaUtils.drop(ActivitatTable, InvitacioTable)
            SchemaUtils.create(ActivitatTable, InvitacioTable)
        }
    }

    @Test
    fun `test afegirInvitacio`() {
        // Insert the activity into ActivitatTable first
        val activitat = Activitat(
            id = 1,
            nom = "Excursió",
            descripcio = "Excursió a la muntanya",
            ubicacio = Localitzacio(41.40338f, 2.17403f),
            dataInici = kotlinx.datetime.LocalDateTime(2024, 5, 1, 10, 0),
            dataFi = kotlinx.datetime.LocalDateTime(2024, 5, 1, 18, 0),
            creador = "anfitrioUser"
        )
        transaction {
            ActivitatTable.insert {
                it[id_activitat] = activitat.id
                it[nom] = activitat.nom
                it[descripcio] = activitat.descripcio
                it[latitud] = activitat.ubicacio.latitud
                it[longitud] = activitat.ubicacio.longitud
                it[dataInici] = activitat.dataInici
                it[dataFi] = activitat.dataFi
                it[username_creador] = activitat.creador
            }
        }

        // Now insert the invitation
        val invitacio = Invitacio(1, "anfitrioUser", "destinatariUser")
        val result = repository.afegirInvitacio(invitacio)
        assertTrue(result)
    }

    @Test
    fun `test eliminarInvitacio`() {
        // Insert the activity into ActivitatTable first
        val activitat = Activitat(
            id = 1,
            nom = "Excursió",
            descripcio = "Excursió a la muntanya",
            ubicacio = Localitzacio(41.40338f, 2.17403f),
            dataInici = kotlinx.datetime.LocalDateTime(2024, 5, 1, 10, 0),
            dataFi = kotlinx.datetime.LocalDateTime(2024, 5, 1, 18, 0),
            creador = "anfitrioUser"
        )
        transaction {
            ActivitatTable.insert {
                it[id_activitat] = activitat.id
                it[nom] = activitat.nom
                it[descripcio] = activitat.descripcio
                it[latitud] = activitat.ubicacio.latitud
                it[longitud] = activitat.ubicacio.longitud
                it[dataInici] = activitat.dataInici
                it[dataFi] = activitat.dataFi
                it[username_creador] = activitat.creador
            }
        }

        val invitacio = Invitacio(1, "anfitrioUser", "destinatariUser")
        repository.afegirInvitacio(invitacio)

        val result = repository.eliminarInvitacio(invitacio)
        assertTrue(result)
    }

    @Test
    fun `test obtenirTotesInvitacions`() {
        val activitat = Activitat(
            id = 1,
            nom = "Excursió",
            descripcio = "Excursió a la muntanya",
            ubicacio = Localitzacio(41.40338f, 2.17403f),
            dataInici = kotlinx.datetime.LocalDateTime(2024, 5, 1, 10, 0),
            dataFi = kotlinx.datetime.LocalDateTime(2024, 5, 1, 18, 0),
            creador = "anfitrioUser"
        )
        transaction {
            ActivitatTable.insert {
                it[id_activitat] = activitat.id
                it[nom] = activitat.nom
                it[descripcio] = activitat.descripcio
                it[latitud] = activitat.ubicacio.latitud
                it[longitud] = activitat.ubicacio.longitud
                it[dataInici] = activitat.dataInici
                it[dataFi] = activitat.dataFi
                it[username_creador] = activitat.creador
            }
        }

        val activitat2 = Activitat(
            id = 2,
            nom = "Concert",
            descripcio = "Concert a la platja",
            ubicacio = Localitzacio(41.40338f, 2.17403f),
            dataInici = kotlinx.datetime.LocalDateTime(2024, 6, 1, 20, 0),
            dataFi = kotlinx.datetime.LocalDateTime(2024, 6, 1, 23, 0),
            creador = "anfitrioUser"
        )
        transaction {
            ActivitatTable.insert {
                it[id_activitat] = activitat2.id
                it[nom] = activitat2.nom
                it[descripcio] = activitat2.descripcio
                it[latitud] = activitat2.ubicacio.latitud
                it[longitud] = activitat2.ubicacio.longitud
                it[dataInici] = activitat2.dataInici
                it[dataFi] = activitat2.dataFi
                it[username_creador] = activitat2.creador
            }
        }

        val invitacio1 = Invitacio(1, "anfitrioUser", "destinatariUser1")
        val invitacio2 = Invitacio(2, "anfitrioUser", "destinatariUser2")
        repository.afegirInvitacio(invitacio1)
        repository.afegirInvitacio(invitacio2)

        val invitacions = repository.obtenirTotesInvitacions()
        assertEquals(2, invitacions.size)
    }

    @Test
    fun `test obtenirActivitatsAmbInvitacionsPerUsuari`() {
        val activitat = Activitat(
            id = 1,
            nom = "Excursió",
            descripcio = "Excursió a la muntanya",
            ubicacio = Localitzacio(41.40338f, 2.17403f),
            dataInici = kotlinx.datetime.LocalDateTime(2024, 5, 1, 10, 0),
            dataFi = kotlinx.datetime.LocalDateTime(2024, 5, 1, 18, 0),
            creador = "anfitrioUser"
        )
        transaction {
            ActivitatTable.insert {
                it[id_activitat] = activitat.id
                it[nom] = activitat.nom
                it[descripcio] = activitat.descripcio
                it[latitud] = activitat.ubicacio.latitud
                it[longitud] = activitat.ubicacio.longitud
                it[dataInici] = activitat.dataInici
                it[dataFi] = activitat.dataFi
                it[username_creador] = activitat.creador
            }
        }

        val invitacio = Invitacio(1, "anfitrioUser", "destinatariUser")
        repository.afegirInvitacio(invitacio)

        val activitats = repository.obtenirActivitatsAmbInvitacionsPerUsuari("destinatariUser")
        assertEquals(1, activitats.size)
        assertEquals("Excursió", activitats[0].nom)
    }
}