package org.example.controllers

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.example.models.Nota
import org.example.repositories.NotaRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ControladorNotesTest {
    private lateinit var mockRepository: NotaRepository
    private lateinit var controlador: ControladorNotes

    @BeforeEach
    fun setUp() {
        mockRepository = mockk()
        controlador = ControladorNotes(mockRepository)
    }

    @Test
    fun `afegirNota should call repository and return result`() {
        // Arrange
        val nota = Nota(
            username = "testUser",
            fechaCreacion = LocalDate(2023, 6, 1),
            horaRecordatorio = LocalTime(14, 30),
            comentario = "Test note"
        )
        every { mockRepository.afegirNota(any()) } returns true

        // Act
        val result = controlador.afegirNota(nota)

        // Assert
        assertTrue(result)
        verify { mockRepository.afegirNota(nota) }
    }

    @Test
    fun `eliminarNota should call repository and return result`() {
        // Arrange
        val id = 1
        every { mockRepository.eliminarNota(id) } returns true

        // Act
        val result = controlador.eliminarNota(id)

        // Assert
        assertTrue(result)
        verify { mockRepository.eliminarNota(id) }
    }

    @Test
    fun `obtenirNotesPerUsuari should return notes from repository`() {
        // Arrange
        val username = "testUser"
        val expectedNotes = listOf(
            Nota(
                id = 1,
                username = username,
                fechaCreacion = LocalDate(2023, 6, 1),
                horaRecordatorio = LocalTime(14, 30),
                comentario = "Test note 1"
            )
        )
        every { mockRepository.obtenirNotesPerUsuari(username) } returns expectedNotes

        // Act
        val result = controlador.obtenirNotesPerUsuari(username)

        // Assert
        assertEquals(expectedNotes, result)
        verify { mockRepository.obtenirNotesPerUsuari(username) }
    }

    @Test
    fun `editarNota should call repository and return result`() {
        // Arrange
        val id = 1
        val nota = Nota(
            username = "testUser",
            fechaCreacion = LocalDate(2023, 6, 1),
            horaRecordatorio = LocalTime(14, 30),
            comentario = "Updated note"
        )
        every { mockRepository.editarNota(id, nota) } returns true

        // Act
        val result = controlador.editarNota(id, nota)

        // Assert
        assertTrue(result)
        verify { mockRepository.editarNota(id, nota) }
    }
}