package org.example.models

import kotlinx.datetime.LocalDateTime
import org.example.enums.TipusVehicle
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test
import kotlin.test.assertEquals

class RutaTest {

    private lateinit var ruta: Ruta

    @BeforeEach
    fun setUp() {
        ruta = Ruta(
            origen = Localitzacio(1.0f, 2.0f),
            desti = Localitzacio(3.0f, 4.0f),
            clientUsername = "testuser",
            data = LocalDateTime(2023, 10, 1, 12, 0),
            id = 1,
            duracioMin = 10,
            duracioMax = 20,
            tipusVehicle = TipusVehicle.Cotxe
        )
    }

    @Test
    @DisplayName("Test de creació d'una ruta")
    fun testCreacionRuta() {
        assertEquals(1.0f, ruta.origen.latitud)
        assertEquals(2.0f, ruta.origen.longitud)
        assertEquals(3.0f, ruta.desti.latitud)
        assertEquals(4.0f, ruta.desti.longitud)
        assertEquals("testuser", ruta.clientUsername)
        assertEquals(LocalDateTime(2023, 10, 1, 12, 0), ruta.data)
        assertEquals(1, ruta.id)
        assertEquals(10, ruta.duracioMin)
        assertEquals(20, ruta.duracioMax)
        assertEquals(TipusVehicle.Cotxe, ruta.tipusVehicle)
    }
}