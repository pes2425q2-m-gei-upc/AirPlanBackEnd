package org.example.models

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ParticipantsActivitatsTest {

    private lateinit var participantsActivitats: ParticipantsActivitats

    @BeforeEach
    fun setUp() {
        participantsActivitats = ParticipantsActivitats(
            id_act = 1,
            us_participant = "participantUser"
        )
    }

    @Test
    @DisplayName("Test de creación de ParticipantsActivitats")
    fun testCreacionParticipantsActivitats() {
        assertEquals(1, participantsActivitats.id_act)
        assertEquals("participantUser", participantsActivitats.us_participant)
    }

    @Test
    @DisplayName("Test de serialización de ParticipantsActivitats")
    fun testSerializacionParticipantsActivitats() {
        val json = Json.encodeToString(ParticipantsActivitats.serializer(), participantsActivitats)
        assertTrue(json.contains("\"id_act\":1"))
        assertTrue(json.contains("\"us_participant\":\"participantUser\""))
    }

    @Test
    @DisplayName("Test de deserialización de ParticipantsActivitats")
    fun testDeserializacionParticipantsActivitats() {
        val json = """{"id_act":1,"us_participant":"participantUser"}"""
        val deserialized = Json.decodeFromString(ParticipantsActivitats.serializer(), json)
        assertEquals(1, deserialized.id_act)
        assertEquals("participantUser", deserialized.us_participant)
    }
}