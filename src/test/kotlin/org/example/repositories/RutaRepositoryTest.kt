package org.example.repositories

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.datetime.LocalDateTime
import org.example.database.RutaTable
import org.example.enums.TipusVehicle
import org.example.models.Localitzacio
import org.example.models.Ruta
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.TestInstance
import java.sql.Connection
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RutaRepositoryTest {

    private lateinit var rutaRepository: RutaRepository
    private lateinit var database: Database

    @BeforeAll
    fun setupDatabase() {
        database = Database.connect(
            "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=FALSE",
            driver = "org.h2.Driver",
            user = "test",
            password = ""
        )
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_REPEATABLE_READ
    }

    @BeforeEach
    fun setUp() {
        transaction(database) {
            SchemaUtils.drop(RutaTable)
            SchemaUtils.create(RutaTable)
        }
        rutaRepository = RutaRepository()
    }

    @AfterEach
    fun tearDown() {
        transaction(database) {
            SchemaUtils.drop(RutaTable)
            SchemaUtils.create(RutaTable)
        }
    }

    @Test
    @DisplayName("Test agregar una ruta")
    fun agregarRutaGuardaYRetornaRutaCorrecta() {
        val ruta = Ruta(
            origen = Localitzacio(41.0f, 2.0f),
            desti = Localitzacio(42.0f, 3.0f),
            clientUsername = "testUser",
            data = LocalDateTime.parse("2024-03-15T10:30:00"),
            id = 0,
            duracioMin = 30,
            duracioMax = 45,
            tipusVehicle = TipusVehicle.Cotxe
        )

        val resultado = transaction(database) {
            rutaRepository.afegirRuta(ruta)
        }

        assertNotEquals(0, resultado.id)
        assertEquals(ruta.origen.latitud, resultado.origen.latitud)
        assertEquals(ruta.origen.longitud, resultado.origen.longitud)
        assertEquals(ruta.desti.latitud, resultado.desti.latitud)
        assertEquals(ruta.desti.longitud, resultado.desti.longitud)
        assertEquals(ruta.clientUsername, resultado.clientUsername)
    }

    @Test
    @DisplayName("Test eliminar una ruta existente")
    fun eliminarRutaExistenteRetornaTrue() {
        val ruta = Ruta(
            origen = Localitzacio(41.0f, 2.0f),
            desti = Localitzacio(42.0f, 3.0f),
            clientUsername = "testUser",
            data = LocalDateTime.parse("2024-03-15T10:30:00"),
            id = 0,
            duracioMin = 30,
            duracioMax = 45,
            tipusVehicle = TipusVehicle.Cotxe
        )

        val rutaGuardada = transaction(database) {
            rutaRepository.afegirRuta(ruta)
        }

        val resultado = transaction(database) {
            rutaRepository.eliminarRuta(rutaGuardada.id)
        }

        assertTrue(resultado)
    }
}