package org.example.models

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ActivitatTest {

    private lateinit var activitat: Activitat

    @BeforeEach
    fun setUp() {
        activitat = Activitat(
            id = 1,
            nom = "Excursió",
            descripcio = "Excursió a la muntanya",
            ubicacio = Localitzacio(41.40338f, 2.17403f),
            dataInici = LocalDateTime(2024, 5, 1, 10, 0),
            dataFi = LocalDateTime(2024, 5, 1, 18, 0),
            creador = "anfitrioUser",
            imatge = ""
        )
    }

    @Test
    @DisplayName("Test de creación de actividad")
    fun testCreacionActivitat() {
        assertEquals(1, activitat.id)
        assertEquals("Excursió", activitat.nom)
        assertEquals("Excursió a la muntanya", activitat.descripcio)
        assertEquals(41.40338f, activitat.ubicacio.latitud)
        assertEquals(2.17403f, activitat.ubicacio.longitud)
        assertEquals(LocalDateTime(2024, 5, 1, 10, 0), activitat.dataInici)
        assertEquals(LocalDateTime(2024, 5, 1, 18, 0), activitat.dataFi)
        assertEquals("anfitrioUser", activitat.creador)
    }

    @Test
    @DisplayName("Test de modificación de actividad")
    fun testModificarActivitat() {
        val nuevaUbicacio = Localitzacio(40.7128f, -74.0060f)
        activitat.modificarActivitat(
            nom = "Nova Excursió",
            descripcio = "Excursió a Nova York",
            ubicacio = nuevaUbicacio,
            dataInici = java.sql.Timestamp.valueOf("2024-06-01 09:00:00"),
            dataFi = java.sql.Timestamp.valueOf("2024-06-01 17:00:00"),
            imatge = "nova_imagen.jpg"
        )

        assertEquals("Nova Excursió", activitat.nom)
        assertEquals("Excursió a Nova York", activitat.descripcio)
        assertEquals(nuevaUbicacio, activitat.ubicacio)
        assertEquals(LocalDateTime(2024, 6, 1, 9, 0), activitat.dataInici)
        assertEquals(LocalDateTime(2024, 6, 1, 17, 0), activitat.dataFi)
        assertEquals("nova_imagen.jpg", activitat.imatge)
    }

    @Test
    @DisplayName("Test de serialización de actividad")
    fun testSerializacionActivitat() {
        val json = Json.encodeToString(Activitat.serializer(), activitat)
        assertTrue(json.contains("\"id\":1"))
        assertTrue(json.contains("\"nom\":\"Excursió\""))
        assertTrue(json.contains("\"descripcio\":\"Excursió a la muntanya\""))
        assertTrue(json.contains("\"creador\":\"anfitrioUser\""))
    }

    @Test
    @DisplayName("Test de deserialización de actividad")
    fun testDeserializacionActivitat() {
        val json = """
            {
                "id": 1,
                "nom": "Excursió",
                "descripcio": "Excursió a la muntanya",
                "ubicacio": {"latitud": 41.40338, "longitud": 2.17403},
                "dataInici": "2024-05-01T10:00:00",
                "dataFi": "2024-05-01T18:00:00",
                "creador": "anfitrioUser",
                "imatge": ""
            }
        """
        val deserializedActivitat = Json.decodeFromString(Activitat.serializer(), json)
        assertEquals(1, deserializedActivitat.id)
        assertEquals("Excursió", deserializedActivitat.nom)
        assertEquals("Excursió a la muntanya", deserializedActivitat.descripcio)
        assertEquals(41.40338f, deserializedActivitat.ubicacio.latitud)
        assertEquals(2.17403f, deserializedActivitat.ubicacio.longitud)
        assertEquals(LocalDateTime(2024, 5, 1, 10, 0), deserializedActivitat.dataInici)
        assertEquals(LocalDateTime(2024, 5, 1, 18, 0), deserializedActivitat.dataFi)
        assertEquals("anfitrioUser", deserializedActivitat.creador)
    }
}