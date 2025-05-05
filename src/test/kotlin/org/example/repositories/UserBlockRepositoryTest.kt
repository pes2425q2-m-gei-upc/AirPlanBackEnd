package org.example.repositories

import org.example.models.UserBlock
import org.example.database.UserBlockTable
import org.example.database.UsuarioTable
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.BeforeAll
import java.sql.Connection
import org.junit.jupiter.api.AfterEach
import org.example.database.ActivitatTable


class UserBlockRepositoryTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun setupDatabase() {
            Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;",
                driver = "org.h2.Driver",
                user = "sa",
                password = ""
                )
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_REPEATABLE_READ
            transaction {
                SchemaUtils.create(UsuarioTable,UserBlockTable)
            }
        }
    }

    @BeforeEach
    fun clearAndSetupDatabase() {
        transaction {
            SchemaUtils.drop(UserBlockTable, UsuarioTable, ActivitatTable)
            SchemaUtils.create(UsuarioTable, UserBlockTable)

            // Insert mock users
            UsuarioTable.insert {
                it[username] = "user1"
                it[nom] = "User One"
                it[email] = "user1@example.com"
                it[idioma] = "en"
            }
            UsuarioTable.insert {
                it[username] = "user2"
                it[nom] = "User Two"
                it[email] = "user2@example.com"
                it[idioma] = "en"
            }
        }
    }

    @AfterEach
    fun tearDown() {
        transaction {
            SchemaUtils.drop(UsuarioTable, UserBlockTable)
        }
    }

    private val repository = UserBlockRepository()

    @Test
    fun `test blockUser successfully blocks a user`() {
        transaction {
            // Arrange
            val blocker = "user1"
            val blocked = "user2"

            // Act
            val result = repository.blockUser(blocker, blocked)

            // Assert
            assertTrue(result)
            val isBlocked = UserBlockTable.select {
                (UserBlockTable.blockerUsername eq blocker) and
                (UserBlockTable.blockedUsername eq blocked)
            }.count() > 0
            assertTrue(isBlocked)
        }
    }

    @Test
    fun `test unblockUser successfully unblocks a user`() {
        transaction {
            // Arrange
            val blocker = "user1"
            val blocked = "user2"
            UserBlockTable.insert {
                it[blockerUsername] = blocker
                it[blockedUsername] = blocked
            }

            // Act
            val result = repository.unblockUser(blocker, blocked)

            // Assert
            assertTrue(result)
            val isBlocked = UserBlockTable.select {
                (UserBlockTable.blockerUsername eq blocker) and
                (UserBlockTable.blockedUsername eq blocked)
            }.count().toInt() == 0
            assertTrue(isBlocked)
        }
    }

    @Test
    fun `test isUserBlocked returns true if user is blocked`() {
        transaction {
            // Arrange
            val blocker = "user1"
            val blocked = "user2"
            UserBlockTable.insert {
                it[blockerUsername] = blocker
                it[blockedUsername] = blocked
            }

            // Act
            val result = repository.isUserBlocked(blocker, blocked)

            // Assert
            assertTrue(result)
        }
    }

    @Test
    fun `test isUserBlocked returns false if user is not blocked`() {
        transaction {
            // Arrange
            val blocker = "user1"
            val blocked = "user2"

            // Act
            val result = repository.isUserBlocked(blocker, blocked)

            // Assert
            assertFalse(result)
        }
    }
}