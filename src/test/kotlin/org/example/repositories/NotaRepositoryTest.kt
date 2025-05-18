package org.example.repositories

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.example.database.ClienteTable
import org.example.database.NotesTable
import org.example.database.UsuarioTable
import org.example.models.Nota
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotaRepositoryTest {
    private lateinit var repository: NotaRepository
    private lateinit var database: Database

    @BeforeAll
    fun setupDatabase() {
        database = Database.connect(
            "jdbc:h2:mem:test_notas_${UUID.randomUUID()};DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=FALSE",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )
    }

    @BeforeEach
    fun setup() {
        transaction(database) {
            SchemaUtils.drop(NotesTable, UsuarioTable, ClienteTable)
            SchemaUtils.create(NotesTable, UsuarioTable, ClienteTable)

            // Insert test user if needed
            try {
                SchemaUtils.create(UsuarioTable)
                UsuarioTable.insert {
                    it[username] = "testUser"
                    it[nom] = "Test User"
                    it[email] = "test@example.com"
                    it[idioma] = "Castellano"
                    it[sesionIniciada] = false
                    it[isAdmin] = false
                }
                SchemaUtils.create(ClienteTable)
                ClienteTable.insert {
                    it[username] = "testUser"
                    it[nivell] = 1
                }
            } catch (e: Exception) {
                // Table might already exist
            }
        }
        repository = NotaRepository()
    }

    @AfterEach
    fun tearDown() {
        transaction(database) {
            SchemaUtils.drop(NotesTable, UsuarioTable, ClienteTable)
        }
    }

    @Test
    fun `afegirNota should insert note into database`() {
        // Create a test client in the database first to satisfy foreign key constraint

        val nota = Nota(
            username = "testUser",
            fechaCreacion = LocalDate(2023, 6, 1),
            horaRecordatorio = LocalTime(14, 30),
            comentario = "Test note"
        )

        val result = repository.afegirNota(nota)
        assertTrue(result)
    }

    @Test
    fun `obtenirNotesPerUsuari should return notes for user`() {
        // Add test data and verify retrieval
        // This test depends on afegirNota working properly

        val username = "testUser"
        val testNota = Nota(
            username = username,
            fechaCreacion = LocalDate(2023, 6, 1),
            horaRecordatorio = LocalTime(14, 30),
            comentario = "Test note"
        )

        repository.afegirNota(testNota)

        val notes = repository.obtenirNotesPerUsuari(username)
        assertEquals(1, notes.size)
        assertEquals(username, notes[0].username)
        assertEquals("Test note", notes[0].comentario)
    }

    @Test
    fun `eliminarNota should remove note from database`() {
        // Add test data then verify deletion
        val username = "testUser"
        val testNota = Nota(
            username = username,
            fechaCreacion = LocalDate(2023, 6, 1),
            horaRecordatorio = LocalTime(14, 30),
            comentario = "Test note"
        )

        repository.afegirNota(testNota)

        // Get the ID of the inserted note
        val noteId = transaction {
            NotesTable.select { NotesTable.username eq username }
                .map { it[NotesTable.id] }
                .firstOrNull()
        } ?: -1

        val result = repository.eliminarNota(noteId)
        assertTrue(result)
    }

    @Test
    fun `editarNota should update note in database`() {
        // Add test data then verify update
        val username = "testUser"
        val testNota = Nota(
            username = username,
            fechaCreacion = LocalDate(2023, 6, 1),
            horaRecordatorio = LocalTime(14, 30),
            comentario = "Original note"
        )

        repository.afegirNota(testNota)

        // Get the ID of the inserted note
        val noteId = transaction {
            NotesTable.select { NotesTable.username eq username }
                .map { it[NotesTable.id] }
                .firstOrNull()
        } ?: -1

        val updatedNota = Nota(
            username = username,
            fechaCreacion = LocalDate(2023, 6, 2),
            horaRecordatorio = LocalTime(15, 0),
            comentario = "Updated note"
        )

        val result = repository.editarNota(noteId, updatedNota)
        assertTrue(result)
    }
}