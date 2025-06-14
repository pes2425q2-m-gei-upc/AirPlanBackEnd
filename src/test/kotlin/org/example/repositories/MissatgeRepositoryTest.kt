package org.example.repositories

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.datetime.LocalDateTime
import org.example.database.MissatgesTable
import org.example.database.UsuarioTable
import org.example.models.Missatge
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.TestInstance
import java.sql.Connection
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import org.example.database.UserBlockTable


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MissatgeRepositoryTest {

    private lateinit var missatgeRepository: MissatgeRepository
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
            SchemaUtils.drop(UserBlockTable, MissatgesTable, UsuarioTable)
            SchemaUtils.create(MissatgesTable, UsuarioTable)
        }
        missatgeRepository = MissatgeRepository()
    }

    @AfterEach
    fun tearDown() {
        transaction(database) {
            SchemaUtils.drop(MissatgesTable, UsuarioTable)
        }
    }

    @Test
    @DisplayName("Test enviar un missatge")
    fun testSendMessage() = runBlocking {
        val missatge = Missatge(
            usernameSender = "user1",
            usernameReceiver = "user2",
            dataEnviament = LocalDateTime.parse("2025-05-01T12:00:00"),
            missatge = "Hola, com estàs?",
            isEdited = false
        )

        val wasSaved = missatgeRepository.sendMessage(missatge) { _ -> }

        assertTrue(wasSaved)
        val messages = missatgeRepository.getMessagesBetweenUsers("user1", "user2")
        assertEquals(1, messages.size)
        val savedMessage = messages.first()
        assertEquals("user1", savedMessage.usernameSender)
        assertEquals("user2", savedMessage.usernameReceiver)
        assertEquals("Hola, com estàs?", savedMessage.missatge)
    }

    @Test
    @DisplayName("Test obtenir missatges entre dos usuaris")
    fun testGetMessagesBetweenUsers() = runBlocking {
        val missatge1 = Missatge(
            usernameSender = "user1",
            usernameReceiver = "user2",
            dataEnviament = LocalDateTime.parse("2025-05-01T12:00:00"),
            missatge = "Hola!",
            isEdited = false
        )
        val missatge2 = Missatge(
            usernameSender = "user2",
            usernameReceiver = "user1",
            dataEnviament = LocalDateTime.parse("2025-05-01T12:05:00"),
            missatge = "Bones!",
            isEdited = false
        )

        missatgeRepository.sendMessage(missatge1) { _ -> }
        missatgeRepository.sendMessage(missatge2) { _ -> }

        val messages = missatgeRepository.getMessagesBetweenUsers("user1", "user2")

        assertEquals(2, messages.size)
        assertTrue(messages.any { it.missatge == "Hola!" })
        assertTrue(messages.any { it.missatge == "Bones!" })
    }

    @Test
    @DisplayName("Test obtenir últims xats per a un usuari")
    fun testGetLatestChatsForUser() = runBlocking {
        val missatge1 = Missatge(
            usernameSender = "user1",
            usernameReceiver = "user2",
            dataEnviament = LocalDateTime.parse("2025-05-01T12:00:00"),
            missatge = "Hola!",
            isEdited = false
        )
        val missatge2 = Missatge(
            usernameSender = "user3",
            usernameReceiver = "user1",
            dataEnviament = LocalDateTime.parse("2025-05-01T12:10:00"),
            missatge = "Ei!",
            isEdited = false
        )

        missatgeRepository.sendMessage(missatge1) { _ -> }
        missatgeRepository.sendMessage(missatge2) { _ -> }

        val chats = missatgeRepository.getLatestChatsForUser("user1")

        assertEquals(2, chats.size)
        assertTrue(chats.any { it.usernameSender == "user1" || it.usernameReceiver == "user1" })
    }

    @Test
    @DisplayName("Test editar un missatge")
    fun testEditMessage() = runBlocking {
        val originalMessage = Missatge(
            usernameSender = "user1",
            usernameReceiver = "user2",
            dataEnviament = LocalDateTime.parse("2025-05-01T12:00:00"),
            missatge = "Original message",
            isEdited = false
        )

        missatgeRepository.sendMessage(originalMessage) { _ -> }

        val timestampStr = "2025-05-01T12:00:00"
        val newContent = "Edited message"
        val editSuccessful = missatgeRepository.editMessage("user1", timestampStr, newContent)

        assertTrue(editSuccessful)
        val messages = missatgeRepository.getMessagesBetweenUsers("user1", "user2")
        assertEquals(1, messages.size)
        val editedMessage = messages.first()
        assertEquals(newContent, editedMessage.missatge)
        assertTrue(editedMessage.isEdited)
    }
}