package org.example.models

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class InvitacioTest {

    private lateinit var invitacio: Invitacio

    @BeforeEach
    fun setUp() {
        invitacio = Invitacio(
            id_act = 1,
            us_anfitrio = "anfitrioUser",
            us_destinatari = "destinatarioUser"
        )
    }

    @Test
    @DisplayName("Test de creación de invitación")
    fun testCreacionInvitacio() {
        assertEquals(1, invitacio.id_act)
        assertEquals("anfitrioUser", invitacio.us_anfitrio)
        assertEquals("destinatarioUser", invitacio.us_destinatari)
    }

    @Test
    @DisplayName("Test de serialización de invitación")
    fun testSerializacionInvitacio() {
        val json = kotlinx.serialization.json.Json.encodeToString(Invitacio.serializer(), invitacio)
        assertTrue(json.contains("\"id_act\":1"))
        assertTrue(json.contains("\"us_anfitrio\":\"anfitrioUser\""))
        assertTrue(json.contains("\"us_destinatari\":\"destinatarioUser\""))
    }

    @Test
    @DisplayName("Test de deserialización de invitación")
    fun testDeserializacionInvitacio() {
        val json = """{"id_act":1,"us_anfitrio":"anfitrioUser","us_destinatari":"destinatarioUser"}"""
        val deserializedInvitacio = kotlinx.serialization.json.Json.decodeFromString(Invitacio.serializer(), json)
        assertEquals(1, deserializedInvitacio.id_act)
        assertEquals("anfitrioUser", deserializedInvitacio.us_anfitrio)
        assertEquals("destinatarioUser", deserializedInvitacio.us_destinatari)
    }
}