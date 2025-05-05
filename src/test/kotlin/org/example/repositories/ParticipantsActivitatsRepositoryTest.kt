package org.example.repositories

import org.example.database.ActivitatTable
import org.example.database.ParticipantsActivitatsTable
import org.example.database.UsuarioTable
import org.example.models.ParticipantsActivitats
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ParticipantsActivitatsRepositoryTest {

    private lateinit var repository: ParticipantsActivitatsRepository

    @BeforeAll
    fun setUpDatabase() {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.create(ActivitatTable, ParticipantsActivitatsTable, UsuarioTable)
        }
        repository = ParticipantsActivitatsRepository()
    }

    @BeforeEach
    fun cleanDatabase() {
        transaction {
            SchemaUtils.drop(ParticipantsActivitatsTable, ActivitatTable, UsuarioTable)
            SchemaUtils.create(ActivitatTable, ParticipantsActivitatsTable, UsuarioTable)
        }
    }

    private fun insertTestActivity() {
        transaction {
            ActivitatTable.insert {
                it[id_activitat] = 1
                it[nom] = "Test Activity"
                it[descripcio] = "Description"
                it[latitud] = 41.40338f
                it[longitud] = 2.17403f
                it[dataInici] = kotlinx.datetime.LocalDateTime(2024, 5, 1, 10, 0)
                it[dataFi] = kotlinx.datetime.LocalDateTime(2024, 5, 1, 18, 0)
                it[username_creador] = "creatorUser"
            }
        }
    }

    private fun insertTestUser(username: String) {
        transaction {
            UsuarioTable.insert {
                it[UsuarioTable.username] = username
                it[UsuarioTable.nom] = "Test User"
                it[UsuarioTable.email] = "$username@example.com"
                it[UsuarioTable.idioma] = "en"
                it[UsuarioTable.sesionIniciada] = false
                it[UsuarioTable.isAdmin] = false
                it[UsuarioTable.photourl] = null
            }
        }
    }

    @Test
    fun `test afegirParticipant`() {
        insertTestActivity()
        insertTestUser("user1")
        val participant = ParticipantsActivitats(id_act = 1, us_participant = "user1")
        val result = repository.afegirParticipant(participant)
        assertTrue(result)
    }

    @Test
    fun `test eliminarParticipantsPerActivitat`() {
        insertTestActivity()
        insertTestUser("user1")
        val participant = ParticipantsActivitats(id_act = 1, us_participant = "user1")
        repository.afegirParticipant(participant)

        val result = repository.eliminarParticipantsPerActivitat(1)
        assertTrue(result)
    }

    @Test
    fun `test esCreador`() {
        insertTestActivity()
        insertTestUser("user1")
        val participant = ParticipantsActivitats(id_act = 1, us_participant = "user1")
        repository.afegirParticipant(participant)

        val result = repository.esCreador(1, "user1")
        assertTrue(result)

        val nonExistentResult = repository.esCreador(1, "user2")
        assertFalse(nonExistentResult)
    }

    @Test
    fun `test eliminarParticipant`() {
        insertTestActivity()
        insertTestUser("user1")
        val participant = ParticipantsActivitats(id_act = 1, us_participant = "user1")
        repository.afegirParticipant(participant)

        val result = repository.eliminarParticipant(1, "user1")
        assertTrue(result)

        val nonExistentResult = repository.eliminarParticipant(1, "user2")
        assertFalse(nonExistentResult)
    }

    @Test
    fun `test obtenirParticipantsPerActivitat`() {
        insertTestActivity()
        insertTestUser("user1")
        insertTestUser("user2")
        val participant1 = ParticipantsActivitats(id_act = 1, us_participant = "user1")
        val participant2 = ParticipantsActivitats(id_act = 1, us_participant = "user2")
        repository.afegirParticipant(participant1)
        repository.afegirParticipant(participant2)

        val participants = repository.obtenirParticipantsPerActivitat(1)
        assertEquals(2, participants.size)
        assertTrue(participants.contains("user1"))
        assertTrue(participants.contains("user2"))
    }
}