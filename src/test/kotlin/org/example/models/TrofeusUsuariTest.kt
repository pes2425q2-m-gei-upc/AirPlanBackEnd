import kotlinx.serialization.json.Json
import org.example.models.TrofeusUsuari
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class TrofeusUsuariTest {

    @Test
    fun `test serialization and deserialization of TrofeusUsuari`() {
        val dataObtencio = LocalDateTime.of(2023, 10, 1, 12, 0)
        val trofeusUsuari = TrofeusUsuari(
            usuari = "user1",
            trofeuId = 1,
            dataObtencio = dataObtencio
        )

        // Serialize the TrofeusUsuari object to JSON
        val json = Json.encodeToString(TrofeusUsuari.serializer(), trofeusUsuari)
        val expectedJson = """
            {"usuari":"user1","trofeuId":1,"dataObtencio":"2023-10-01T12:00:00"}
        """.trimIndent()
        assertEquals(expectedJson, json)

        // Deserialize the JSON back to a TrofeusUsuari object
        val deserializedTrofeusUsuari = Json.decodeFromString(TrofeusUsuari.serializer(), json)
        assertEquals(trofeusUsuari, deserializedTrofeusUsuari)
    }
}