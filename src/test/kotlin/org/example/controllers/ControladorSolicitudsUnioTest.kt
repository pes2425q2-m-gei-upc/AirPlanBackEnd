package org.example.controllers

import org.example.models.Activitat
import org.example.models.SolicitudUnio
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import repositories.SolicitudRepository
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ControladorSolicitudsUnioTest {

    private lateinit var solicitudRepository: SolicitudRepository
    private lateinit var controlador: ControladorSolicitudsUnio

    @Before
    fun setup() {
        solicitudRepository = mock(SolicitudRepository::class.java)
        controlador = ControladorSolicitudsUnio(solicitudRepository)
    }

    @Test
    fun `test enviarSolicitud returns true when repository succeeds`() {
        `when`(solicitudRepository.enviarSolicitud("host", "user", 1)).thenReturn(true)

        val result = controlador.enviarSolicitud("host", "user", 1)

        assertTrue(result)
        verify(solicitudRepository).enviarSolicitud("host", "user", 1)
    }

    @Test
    fun `test eliminarSolicitud returns false when repository fails`() {
        `when`(solicitudRepository.eliminarSolicitud("user", 1)).thenReturn(false)

        val result = controlador.eliminarSolicitud("user", 1)

        assertFalse(result)
        verify(solicitudRepository).eliminarSolicitud("user", 1)
    }

    @Test
    fun `test obtenirSolicitudesPerUsuari returns list of activities`() {
        val activities = listOf(Activitat(1, "Activity", "Description", null, null, null, "creator"))
        `when`(solicitudRepository.obtenirSolicitudesPerUsuari("user")).thenReturn(activities)

        val result = controlador.obtenirSolicitudesPerUsuari("user")

        assertTrue(result.isNotEmpty())
        verify(solicitudRepository).obtenirSolicitudesPerUsuari("user")
    }
}