package org.example.controllers

import kotlinx.datetime.LocalDateTime
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
import org.mockito.kotlin.never
import org.mockito.kotlin.eq
import org.example.services.PerspectiveService
import kotlinx.coroutines.runBlocking

class ControladorActivitatTest {

    private lateinit var activitatRepository: ActivitatRepository
    private lateinit var participantsActivitatsRepository: ParticipantsActivitatsRepository
    private lateinit var activitatFavoritaRepository: ActivitatFavoritaRepository
    private lateinit var perspectiveService: PerspectiveService
    private lateinit var controladorActivitat: ControladorActivitat

    @BeforeEach
    fun setUp() {
        activitatRepository = mock(ActivitatRepository::class.java)
        participantsActivitatsRepository = mock(ParticipantsActivitatsRepository::class.java)
        activitatFavoritaRepository = mock(ActivitatFavoritaRepository::class.java)
        perspectiveService = mock(PerspectiveService::class.java)
        controladorActivitat = ControladorActivitat(
            activitatRepository,
            participantsActivitatsRepository,
            activitatFavoritaRepository,
            perspectiveService
        )
    }    @Test
    @DisplayName("Test afegir activitat")
    fun testAfegirActivitat() {
        val ubicacio = Localitzacio(41.40338f, 2.17403f)
        val dataInici = LocalDateTime(2024, 5, 1, 10, 0)
        val dataFi = LocalDateTime(2024, 5, 1, 18, 0)
        val creador = "anfitrioUser"

        // Mock PerspectiveService to allow appropriate content
        runBlocking {
            whenever(perspectiveService.analyzeMessages(any())).thenReturn(listOf(false, false))
        }
        
        whenever(activitatRepository.afegirActivitat(any())).thenReturn(1)
        whenever(participantsActivitatsRepository.afegirParticipant(any())).thenReturn(true)

        controladorActivitat.afegirActivitat(
            "Excursió",
            "Excursió a la muntanya",
            ubicacio,
            dataInici,
            dataFi,
            creador,
            ""
        )

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
            creador = "anfitrioUser",
            imatge = ""
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
            creador = "anfitrioUser",
            imatge = ""
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
            creador = "anfitrioUser",
            imatge = ""
        )

        `when`(activitatRepository.obtenirActivitatsExcluintUsuaris(listOf("blockedUser"))).thenReturn(listOf(activitat))

        val activitats = controladorActivitat.obtenirActivitatsExcluintUsuaris(listOf("blockedUser"))

        assertEquals(1, activitats.size)
        assertEquals("Excursió", activitats[0].nom)
        verify(activitatRepository).obtenirActivitatsExcluintUsuaris(listOf("blockedUser"))
    }

    @Test
    @DisplayName("Test obtenir activitats per participant")
    fun testObtenirActivitatsPerParticipant() {
        val activitat = Activitat(
            id = 1,
            nom = "Excursió",
            descripcio = "Excursió a la muntanya",
            ubicacio = Localitzacio(41.40338f, 2.17403f),
            dataInici = LocalDateTime(2024, 5, 1, 10, 0),
            dataFi = LocalDateTime(2024, 5, 1, 18, 0),
            creador = "anfitrioUser",
            imatge = ""
        )

        `when`(participantsActivitatsRepository.obtenirActivitatsPerParticipant("user1")).thenReturn(listOf(activitat))

        val activitats = controladorActivitat.obtenirActivitatsPerParticipant("user1")

        assertEquals(1, activitats.size)
        assertEquals("Excursió", activitats[0].nom)
        verify(participantsActivitatsRepository).obtenirActivitatsPerParticipant("user1")
    }


    @Test
    @DisplayName("Test afegir activitat amb contingut inapropiat")
    fun testAfegirActivitatAmbContingutInapropiat() {
        val ubicacio = Localitzacio(41.40338f, 2.17403f)
        val dataInici = LocalDateTime(2024, 5, 1, 10, 0)
        val dataFi = LocalDateTime(2024, 5, 1, 18, 0)
        val creador = "anfitrioUser"
        val titolInapropiat = "Títol inapropiat"
        val descripcioBona = "Descripció normal"

        // Mock PerspectiveService to detect inappropriate content in title
        runBlocking {
            whenever(perspectiveService.analyzeMessages(listOf(titolInapropiat, descripcioBona)))
                .thenReturn(listOf(true, false))
        }

        // Assert that creating activity with inappropriate title throws exception
        val exception = assertThrows(IllegalArgumentException::class.java) {
            controladorActivitat.afegirActivitat(titolInapropiat, descripcioBona, ubicacio, dataInici, dataFi, creador, imatge = "")
        }

        assertEquals("Títol o descripció bloquejats per ser inapropiats", exception.message)
        
        // Verify PerspectiveService was called
        runBlocking {
            verify(perspectiveService).analyzeMessages(listOf(titolInapropiat, descripcioBona))
        }
        
        // Verify activitatRepository was NOT called (because of inappropriate content)
        verify(activitatRepository, never()).afegirActivitat(any())
    }

    @Test
    @DisplayName("Test afegir activitat amb descripció inapropiada")
    fun testAfegirActivitatAmbDescripcioInapropiada() {
        val ubicacio = Localitzacio(41.40338f, 2.17403f)
        val dataInici = LocalDateTime(2024, 5, 1, 10, 0)
        val dataFi = LocalDateTime(2024, 5, 1, 18, 0)
        val creador = "anfitrioUser"
        val titolBo = "Títol normal"
        val descripcioInapropiada = "Descripció inapropiada"

        // Mock PerspectiveService to detect inappropriate content in description
        runBlocking {
            whenever(perspectiveService.analyzeMessages(listOf(titolBo, descripcioInapropiada)))
                .thenReturn(listOf(false, true))
        }

        // Assert that creating activity with inappropriate description throws exception
        val exception = assertThrows(IllegalArgumentException::class.java) {
            controladorActivitat.afegirActivitat(titolBo, descripcioInapropiada, ubicacio, dataInici, dataFi, creador, imatge = "")
        }

        assertEquals("Títol o descripció bloquejats per ser inapropiats", exception.message)
        
        // Verify PerspectiveService was called
        runBlocking {
            verify(perspectiveService).analyzeMessages(listOf(titolBo, descripcioInapropiada))
        }
        
        // Verify activitatRepository was NOT called (because of inappropriate content)
        verify(activitatRepository, never()).afegirActivitat(any())
    }

    @Test
    @DisplayName("Test modificar activitat amb contingut inapropiat")
    fun testModificarActivitatAmbContingutInapropiat() {
        val activitatId = 1
        val ubicacio = Localitzacio(41.40338f, 2.17403f)
        val dataInici = LocalDateTime(2024, 5, 1, 10, 0)
        val dataFi = LocalDateTime(2024, 5, 1, 18, 0)
        val titolInapropiat = "Títol inapropiat"
        val descripcioBona = "Descripció normal"

        // Mock PerspectiveService to detect inappropriate content in title
        runBlocking {
            whenever(perspectiveService.analyzeMessages(listOf(titolInapropiat, descripcioBona)))
                .thenReturn(listOf(true, false))
        }

        // Assert that modifying activity with inappropriate title throws exception
        val exception = assertThrows(IllegalArgumentException::class.java) {
            controladorActivitat.modificarActivitat(activitatId, titolInapropiat, descripcioBona, ubicacio, dataInici, dataFi)
        }

        assertEquals("Títol o descripció bloquejats per ser inapropiats", exception.message)
        
        // Verify PerspectiveService was called
        runBlocking {
            verify(perspectiveService).analyzeMessages(listOf(titolInapropiat, descripcioBona))
        }
        
        // Verify activitatRepository was NOT called (because of inappropriate content)
        verify(activitatRepository, never()).modificarActivitat(eq(activitatId), any(), any(), any(), any(), any())
    }

    @Test
    @DisplayName("Test modificar activitat amb descripció inapropiada")
    fun testModificarActivitatAmbDescripcioInapropiada() {
        val activitatId = 1
        val ubicacio = Localitzacio(41.40338f, 2.17403f)
        val dataInici = LocalDateTime(2024, 5, 1, 10, 0)
        val dataFi = LocalDateTime(2024, 5, 1, 18, 0)
        val titolBo = "Títol normal"
        val descripcioInapropiada = "Descripció inapropiada"

        // Mock PerspectiveService to detect inappropriate content in description
        runBlocking {
            whenever(perspectiveService.analyzeMessages(listOf(titolBo, descripcioInapropiada)))
                .thenReturn(listOf(false, true))
        }

        // Assert that modifying activity with inappropriate description throws exception
        val exception = assertThrows(IllegalArgumentException::class.java) {
            controladorActivitat.modificarActivitat(activitatId, titolBo, descripcioInapropiada, ubicacio, dataInici, dataFi)
        }

        assertEquals("Títol o descripció bloquejats per ser inapropiats", exception.message)
        
        // Verify PerspectiveService was called
        runBlocking {
            verify(perspectiveService).analyzeMessages(listOf(titolBo, descripcioInapropiada))
        }
        
        // Verify activitatRepository was NOT called (because of inappropriate content)
        verify(activitatRepository, never()).modificarActivitat(eq(activitatId), any(), any(), any(), any(), any())
    }

    @Test
    @DisplayName("Test afegir activitat amb contingut apropiat")
    fun testAfegirActivitatAmbContingutApropiat() {
        val ubicacio = Localitzacio(41.40338f, 2.17403f)
        val dataInici = LocalDateTime(2024, 5, 1, 10, 0)
        val dataFi = LocalDateTime(2024, 5, 1, 18, 0)
        val creador = "anfitrioUser"
        val titolBo = "Excursió"
        val descripcioBona = "Excursió a la muntanya"

        // Mock PerspectiveService to allow appropriate content
        runBlocking {
            whenever(perspectiveService.analyzeMessages(listOf(titolBo, descripcioBona)))
                .thenReturn(listOf(false, false))
        }
        
        whenever(activitatRepository.afegirActivitat(any())).thenReturn(1)
        whenever(participantsActivitatsRepository.afegirParticipant(any())).thenReturn(true)

        // No exception should be thrown here
        controladorActivitat.afegirActivitat(titolBo, descripcioBona, ubicacio, dataInici, dataFi, creador, imatge = "")

        // Verify PerspectiveService was called
        runBlocking {
            verify(perspectiveService).analyzeMessages(listOf(titolBo, descripcioBona))
        }
        
        // Verify repository methods were called
        verify(activitatRepository).afegirActivitat(any())
        verify(participantsActivitatsRepository).afegirParticipant(any())
    }

    @Test
    @DisplayName("Test modificar activitat amb contingut apropiat")
    fun testModificarActivitatAmbContingutApropiat() {
        val activitatId = 1
        val ubicacio = Localitzacio(41.40338f, 2.17403f)
        val dataInici = LocalDateTime(2024, 5, 1, 10, 0)
        val dataFi = LocalDateTime(2024, 5, 1, 18, 0)
        val titolBo = "Excursió"
        val descripcioBona = "Excursió a la muntanya"

        // Mock PerspectiveService to allow appropriate content
        runBlocking {
            whenever(perspectiveService.analyzeMessages(listOf(titolBo, descripcioBona)))
                .thenReturn(listOf(false, false))
        }
        
        whenever(activitatRepository.modificarActivitat(eq(activitatId), any(), any(), any(), any(), any())).thenReturn(true)

        // No exception should be thrown here
        val result = controladorActivitat.modificarActivitat(activitatId, titolBo, descripcioBona, ubicacio, dataInici, dataFi)
        
        assertTrue(result)

        // Verify PerspectiveService was called
        runBlocking {
            verify(perspectiveService).analyzeMessages(listOf(titolBo, descripcioBona))
        }
        
        // Verify repository method was called
        verify(activitatRepository).modificarActivitat(eq(activitatId), eq(titolBo), eq(descripcioBona), eq(ubicacio), eq(dataInici), eq(dataFi))

    }
}