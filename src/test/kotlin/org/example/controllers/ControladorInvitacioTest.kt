package org.example.controllers

import kotlinx.coroutines.test.runTest
import org.example.models.Invitacio
import org.example.models.ParticipantsActivitats
import org.example.repositories.InvitacioRepository
import org.example.repositories.ParticipantsActivitatsRepository
import org.example.repositories.UsuarioRepository
import org.example.websocket.WebSocketManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class ControladorInvitacioTest {

    private lateinit var participantsActivitatsRepository: ParticipantsActivitatsRepository
    private lateinit var invitacioRepository: InvitacioRepository
    private lateinit var usuarioRepository: UsuarioRepository
    private lateinit var webSocketManager: WebSocketManager
    private lateinit var controladorInvitacions: ControladorInvitacions

    @BeforeEach
    fun setUp() {
        participantsActivitatsRepository = mock()
        invitacioRepository = mock()
        usuarioRepository = mock()
        webSocketManager = mock()
        controladorInvitacions = ControladorInvitacions(
            participantsActivitatsRepository,
            invitacioRepository,
            usuarioRepository,
            webSocketManager
        )
    }

    @Test
    @DisplayName("Test crear invitació")
    fun testCrearInvitacio() = runTest {
        whenever(usuarioRepository.existeUsuario("destinatariUser")).thenReturn(true)

        val result = controladorInvitacions.crearInvitacio(1, "anfitrioUser", "destinatariUser")

        assertTrue(result)
        verify(invitacioRepository).afegirInvitacio(any())
    }

    @Test
    @DisplayName("Test crear invitació con usuario inexistente")
    fun testCrearInvitacioUsuarioInexistente() = runTest {
        whenever(usuarioRepository.existeUsuario("destinatariUser")).thenReturn(false)

        val result = controladorInvitacions.crearInvitacio(1, "anfitrioUser", "destinatariUser")

        assertFalse(result)
        verify(invitacioRepository, never()).afegirInvitacio(any())
    }

    @Test
    @DisplayName("Test aceptar invitación")
    fun testAcceptarInvitacio() = runTest {
        val invitacio = Invitacio(1, "anfitrioUser", "destinatariUser")
        whenever(invitacioRepository.eliminarInvitacio(invitacio)).thenReturn(true)
        whenever(participantsActivitatsRepository.afegirParticipant(any())).thenReturn(true)

        val result = controladorInvitacions.acceptarInvitacio(invitacio)

        assertTrue(result)
        verify(participantsActivitatsRepository).afegirParticipant(any())
    }

    @Test
    @DisplayName("Test rechazar invitación")
    fun testRebutjarInvitacio() = runTest {
        val invitacio = Invitacio(1, "anfitrioUser", "destinatariUser")
        whenever(invitacioRepository.eliminarInvitacio(invitacio)).thenReturn(true)

        val result = controladorInvitacions.rebutjarInvitacio(invitacio)

        assertTrue(result)
        verify(invitacioRepository).eliminarInvitacio(invitacio)
    }

    @Test
    @DisplayName("Test listar invitaciones")
    fun testListarInvitacions() = runTest {
        val invitacio = Invitacio(1, "anfitrioUser", "destinatariUser")
        whenever(invitacioRepository.obtenirTotesInvitacions()).thenReturn(listOf(invitacio))

        val invitacions = controladorInvitacions.listarInvitacions()

        assertEquals(1, invitacions.size)
        assertEquals("anfitrioUser", invitacions[0].us_anfitrio)
        verify(invitacioRepository).obtenirTotesInvitacions()
    }
}