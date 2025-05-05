package org.example.controllers

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import org.example.models.*
import org.example.repositories.*
import repositories.ActivitatRepository
import repositories.ActivitatFavoritaRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.whenever
import org.mockito.kotlin.any
import org.mockito.kotlin.verify


class ControladorActivitatTest {

    private lateinit var activitatRepository: ActivitatRepository
    private lateinit var participantsActivitatsRepository: ParticipantsActivitatsRepository
    private lateinit var activitatFavoritaRepository: ActivitatFavoritaRepository
    private lateinit var controladorActivitat: ControladorActivitat

    @BeforeEach
    fun setUp() {
        activitatRepository = mock(ActivitatRepository::class.java)
        participantsActivitatsRepository = mock(ParticipantsActivitatsRepository::class.java)
        activitatFavoritaRepository = mock(ActivitatFavoritaRepository::class.java)
        controladorActivitat = ControladorActivitat(
            activitatRepository,
            participantsActivitatsRepository,
            activitatFavoritaRepository
        )
    }

    @Test
    @DisplayName("Test afegir activitat")
    fun testAfegirActivitat() {
        val ubicacio = Localitzacio(41.40338f, 2.17403f)
        val dataInici = LocalDateTime(2024, 5, 1, 10, 0)
        val dataFi = LocalDateTime(2024, 5, 1, 18, 0)
        val creador = "anfitrioUser"

        whenever(activitatRepository.afegirActivitat(any())).thenReturn(1)
        whenever(participantsActivitatsRepository.afegirParticipant(any())).thenReturn(true)

        controladorActivitat.afegirActivitat("Excursió", "Excursió a la muntanya", ubicacio, dataInici, dataFi, creador)

        verify(activitatRepository).afegirActivitat(any())
        verify(participantsActivitatsRepository).afegirParticipant(any())
    }

    @Test
    @DisplayName("Test obtenir activitats")
    fun testObtenirActivitats() {
        val activitat = Activitat(
            id = 1,
            nom = "Excursió",
            descripcio = "Excursió a la muntanya",
            ubicacio = Localitzacio(41.40338f, 2.17403f),
            dataInici = LocalDateTime(2024, 5, 1, 10, 0),
            dataFi = LocalDateTime(2024, 5, 1, 18, 0),
            creador = "anfitrioUser"
        )

        `when`(activitatRepository.obtenirActivitats()).thenReturn(listOf(activitat))

        val activitats = controladorActivitat.obtenirTotesActivitats()

        assertEquals(1, activitats.size)
        assertEquals("Excursió", activitats[0].nom)
        verify(activitatRepository).obtenirActivitats()
    }

    @Test
    @DisplayName("Test eliminar activitat")
    fun testEliminarActivitat() {
        `when`(activitatRepository.eliminarActividad(1)).thenReturn(true)

        val result = controladorActivitat.eliminarActividad(1)

        assertTrue(result)
        verify(activitatRepository).eliminarActividad(1)
    }

    @Test
    @DisplayName("Test afegir activitat favorita")
    fun testAfegirActivitatFavorita() {
        val dataAfegida = LocalDateTime(2024, 5, 1, 10, 0)
        `when`(activitatFavoritaRepository.afegirActivitatFavorita(1, "user", dataAfegida)).thenReturn(true)

        val result = controladorActivitat.afegirActivitatFavorita(1, "user", dataAfegida)

        assertTrue(result)
        verify(activitatFavoritaRepository).afegirActivitatFavorita(1, "user", dataAfegida)
    }

    @Test
    @DisplayName("Test eliminar activitat favorita")
    fun testEliminarActivitatFavorita() {
        `when`(activitatFavoritaRepository.eliminarActivitatFavorita(1, "user")).thenReturn(true)

        val result = controladorActivitat.eliminarActivitatFavorita(1, "user")

        assertTrue(result)
        verify(activitatFavoritaRepository).eliminarActivitatFavorita(1, "user")
    }

    @Test
    @DisplayName("Test obtenir activitats favorites per usuari")
    fun testObtenirActivitatsFavoritesPerUsuari() {
        val activitat = Activitat(
            id = 1,
            nom = "Excursió",
            descripcio = "Excursió a la muntanya",
            ubicacio = Localitzacio(41.40338f, 2.17403f),
            dataInici = LocalDateTime(2024, 5, 1, 10, 0),
            dataFi = LocalDateTime(2024, 5, 1, 18, 0),
            creador = "anfitrioUser"
        )

        whenever(activitatFavoritaRepository.obtenirActivitatsFavoritesPerUsuari("user")).thenReturn(listOf(activitat))

        val activitats = controladorActivitat.obtenirActivitatsFavoritesPerUsuari("user")

        assertEquals(1, activitats.size)
        assertEquals("Excursió", activitats[0].nom)
        verify(activitatFavoritaRepository).obtenirActivitatsFavoritesPerUsuari("user")
    }

    @Test
    @DisplayName("Test obtenir participants de activitat")
    fun testObtenirParticipantsDeActivitat() {
        `when`(participantsActivitatsRepository.obtenirParticipantsPerActivitat(1)).thenReturn(listOf("user1", "user2"))

        val participants = controladorActivitat.obtenirParticipantsDeActivitat(1)

        assertEquals(2, participants.size)
        assertTrue(participants.contains("user1"))
        assertTrue(participants.contains("user2"))
        verify(participantsActivitatsRepository).obtenirParticipantsPerActivitat(1)
    }

    @Test
    @DisplayName("Test obtenir activitats excluint usuaris bloqueados")
    fun testObtenirActivitatsExcluintUsuaris() {
        val activitat = Activitat(
            id = 1,
            nom = "Excursió",
            descripcio = "Excursió a la muntanya",
            ubicacio = Localitzacio(41.40338f, 2.17403f),
            dataInici = LocalDateTime(2024, 5, 1, 10, 0),
            dataFi = LocalDateTime(2024, 5, 1, 18, 0),
            creador = "anfitrioUser"
        )

        `when`(activitatRepository.obtenirActivitatsExcluintUsuaris(listOf("blockedUser"))).thenReturn(listOf(activitat))

        val activitats = controladorActivitat.obtenirActivitatsExcluintUsuaris(listOf("blockedUser"))

        assertEquals(1, activitats.size)
        assertEquals("Excursió", activitats[0].nom)
        verify(activitatRepository).obtenirActivitatsExcluintUsuaris(listOf("blockedUser"))
    }
}