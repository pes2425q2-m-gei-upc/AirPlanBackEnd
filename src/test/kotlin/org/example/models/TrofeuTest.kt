import kotlinx.serialization.json.Json
import org.example.enums.NivellTrofeu
import org.example.models.Trofeu
import kotlin.test.Test
import kotlin.test.assertEquals

class TrofeuTest {

    @Test
    fun `test serialization and deserialization of Trofeu`() {
        val trofeu = Trofeu(
            id = 1,
            nom = "Trofeu d'Or",
            descripcio = "Aquest és un trofeu d'or",
            nivell = NivellTrofeu.or,
            experiencia = 100,
            imatge = "https://example.com/trofeu.png"
        )

        // Serialize the Trofeu object to JSON
        val json = Json.encodeToString(Trofeu.serializer(), trofeu)
        val expectedJson = """
            {"id":1,"nom":"Trofeu d'Or","descripcio":"Aquest és un trofeu d'or","nivell":"or","experiencia":100,"imatge":"https://example.com/trofeu.png"}
        """.trimIndent()
        assertEquals(expectedJson, json)

        // Deserialize the JSON back to a Trofeu object
        val deserializedTrofeu = Json.decodeFromString(Trofeu.serializer(), json)
        assertEquals(trofeu, deserializedTrofeu)
    }
}