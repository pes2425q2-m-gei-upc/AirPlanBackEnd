package org.example.models

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MissatgeTest {

    // Test para verificar la creación del objeto Missatge
    @Test
    fun testMissatgeCreation() {
        val dataEnviament = LocalDateTime(2024, 5, 1, 10, 0, 0)
        val missatge = Missatge(
            usernameSender = "bob",
            usernameReceiver = "alice",
            dataEnviament = dataEnviament,
            missatge = "Hola Alice!"
        )

        assertEquals("bob", missatge.usernameSender)
        assertEquals("alice", missatge.usernameReceiver)
        assertEquals(dataEnviament, missatge.dataEnviament)
        assertEquals("Hola Alice!", missatge.missatge)
    }

    // Test para verificar la serialización de Missatge
    @Test
    fun testMissatgeSerialization() {
        val dataEnviament = LocalDateTime(2024, 5, 1, 10, 0, 0)
        val missatge = Missatge(
            usernameSender = "bob",
            usernameReceiver = "alice",
            dataEnviament = dataEnviament,
            missatge = "Hola Alice!"
        )

        val json = Json.encodeToString(missatge)
        println(json) // Imprime el JSON generado

        // Usamos un enfoque más flexible para la comparación de fechas
        assertTrue(json.contains("\"usernameSender\":\"bob\""))
        assertTrue(json.contains("\"usernameReceiver\":\"alice\""))

        // Comprobamos que la fecha está en el formato adecuado
        assertTrue(json.contains("\"dataEnviament\":\"2024-05-01T10:00"))
    }


    // Test para verificar la deserialización de Missatge
    @Test
    fun testMissatgeDeserialization() {
        val json = """
            {
                "usernameSender": "bob",
                "usernameReceiver": "alice",
                "dataEnviament": "2024-05-01T10:00:00",
                "missatge": "Hola Alice!"
            }
        """
        val missatge = Json.decodeFromString<Missatge>(json)

        assertEquals("bob", missatge.usernameSender)
        assertEquals("alice", missatge.usernameReceiver)
        assertEquals(LocalDateTime(2024, 5, 1, 10, 0, 0), missatge.dataEnviament)
        assertEquals("Hola Alice!", missatge.missatge)
    }

    // Test para verificar que la fecha se serializa/deserializa correctamente como LocalDateTime
    @Test
    fun testLocalDateTimeHandling() {
        val dataEnviament = LocalDateTime(2024, 5, 1, 10, 0, 0)
        val missatge = Missatge(
            usernameSender = "bob",
            usernameReceiver = "alice",
            dataEnviament = dataEnviament,
            missatge = "Hola Alice!"
        )

        val json = Json.encodeToString(missatge)
        val missatgeDeserialized = Json.decodeFromString<Missatge>(json)

        assertEquals(missatge.dataEnviament, missatgeDeserialized.dataEnviament)
    }
}

