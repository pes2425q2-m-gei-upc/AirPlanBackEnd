package org.example.controllers

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.example.enums.TipusVehicle
import org.example.models.Localitzacio
import org.example.models.Ruta
import org.example.repositories.RutaRepository
import org.junit.jupiter.api.Assertions
import kotlin.test.Test

class ControladorRutaTest {
    @Test
    fun createdRouteHasCorrectOriginAndDestination() {
        val repository = mockk<RutaRepository>()
        val controller = ControladorRuta(repository)
        val jsonInput = buildJsonObject {
            put("origen", buildJsonObject {
                put("latitud", JsonPrimitive(41.123))
                put("longitud", JsonPrimitive(2.234))
            })
            put("desti", buildJsonObject {
                put("latitud", JsonPrimitive(41.456))
                put("longitud", JsonPrimitive(2.567))
            })
            put("client", JsonPrimitive("testUser"))
            put("data", JsonPrimitive("2024-03-15T10:30:00"))
            put("duracioMin", JsonPrimitive(30))
            put("duracioMax", JsonPrimitive(45))
            put("tipusVehicle", JsonPrimitive("Cotxe"))
        }

        every { repository.afegirRuta(any()) } returns Ruta(
            Localitzacio(41.123f, 2.234f),
            Localitzacio(41.456f, 2.567f),
            "testUser",
            LocalDateTime.Companion.parse("2024-03-15T10:30:00"),
            1,
            30,
            45,
            TipusVehicle.Cotxe
        )

        val result = controller.crearRuta(jsonInput)

        Assertions.assertEquals(41.123f, result.origen.latitud)
        Assertions.assertEquals(2.234f, result.origen.longitud)
        Assertions.assertEquals(41.456f, result.desti.latitud)
        Assertions.assertEquals(2.567f, result.desti.longitud)
        verify { repository.afegirRuta(any()) }
    }

    @Test
    fun deleteRouteReturnsTrue() {
        val repository = mockk<RutaRepository>()
        val controller = ControladorRuta(repository)
        every { repository.eliminarRuta(1) } returns true

        val result = controller.eliminarRuta(1)

        Assertions.assertTrue(result)
        verify { repository.eliminarRuta(1) }
    }

    @Test
    fun getClientRoutesReturnsCorrectList() {
        val repository = mockk<RutaRepository>()
        val controller = ControladorRuta(repository)
        val expectedRoutes = listOf(
            Ruta(
                Localitzacio(41.0f, 2.0f),
                Localitzacio(42.0f, 3.0f),
                "testUser",
                LocalDateTime.Companion.parse("2024-03-15T10:30:00"),
                1,
                30,
                45,
                TipusVehicle.Cotxe
            )
        )
        every { repository.obtenirRutesClient("testUser") } returns expectedRoutes

        val result = controller.obtenirTotesRutesClient("testUser")

        Assertions.assertEquals(expectedRoutes, result)
        verify { repository.obtenirRutesClient("testUser") }
    }

    @Test
    fun createRouteWithMissingOptionalFieldsUsesDefaults() {
        val repository = mockk<RutaRepository>()
        val controller = ControladorRuta(repository)
        val jsonInput = buildJsonObject {
            put("client", JsonPrimitive("testUser"))
            put("data", JsonPrimitive("2024-03-15T10:30:00"))
            put("duracioMin", JsonPrimitive(30))
            put("duracioMax", JsonPrimitive(45))
            put("tipusVehicle", JsonPrimitive("Cotxe"))
        }

        every { repository.afegirRuta(any()) } returns Ruta(
            Localitzacio(0.0f, 0.0f),
            Localitzacio(0.0f, 0.0f),
            "testUser",
            LocalDateTime.Companion.parse("2024-03-15T10:30:00"),
            1,
            30,
            45,
            TipusVehicle.Cotxe
        )

        val result = controller.crearRuta(jsonInput)

        Assertions.assertEquals(0.0f, result.origen.latitud)
        Assertions.assertEquals(0.0f, result.origen.longitud)
        verify { repository.afegirRuta(any()) }
    }
}