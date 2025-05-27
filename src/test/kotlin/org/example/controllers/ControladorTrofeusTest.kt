import io.mockk.every
import io.mockk.mockk
import org.example.controllers.ControladorTrofeus
import org.example.enums.NivellTrofeu
import org.example.models.Trofeu
import org.example.repositories.TrofeuRepository
import org.example.repositories.TrofeuUsuariInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class ControladorTrofeusTest {

    private val mockRepository = mockk<TrofeuRepository>()
    private val controladorTrofeus = ControladorTrofeus(mockRepository)

    @Test
    fun `test obtenirTrofeusPerUsuari returns correct data`() {
        // Arrange
        val usuari = "user1"
        val mockTrofeus = listOf(
            TrofeuUsuariInfo(
                trofeu = Trofeu(
                    id = 1,
                    nom = "Trofeu d'Or",
                    descripcio = "Descripció del trofeu d'or",
                    nivell = NivellTrofeu.or,
                    experiencia = 100,
                    imatge = "or.png"
                ),
                obtingut = true,
                dataObtencio = Instant.parse("2023-01-01T00:00:00Z")
            ),
            TrofeuUsuariInfo(
                trofeu = Trofeu(
                    id = 2,
                    nom = "Trofeu de Plata",
                    descripcio = "Descripció del trofeu de plata",
                    nivell = NivellTrofeu.plata,
                    experiencia = 50,
                    imatge = "plata.png"
                ),
                obtingut = false,
                dataObtencio = null
            )
        )

        every { mockRepository.obtenirTrofeusPerUsuari(usuari) } returns mockTrofeus

        // Act
        val result = controladorTrofeus.obtenirTrofeusPerUsuari(usuari)

        // Assert
        assertEquals(2, result.size)
        assertEquals("Trofeu d'Or", result[0].trofeu.nom)
        assertEquals(true, result[0].obtingut)
        assertEquals("Trofeu de Plata", result[1].trofeu.nom)
        assertEquals(false, result[1].obtingut)
    }
}