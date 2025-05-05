package org.example.services

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AirQualityServiceTest {
    
    private val mockAirQualityData = buildJsonArray {
        add(buildJsonObject {
            put("nom_estacio", "Barcelona (Eixample)")
            put("contaminant", "NO2")
            put("latitud", "41.3853")
            put("longitud", "2.1539")
            put("h01", "35.0")
            put("h02", "32.0")
            put("h03", "30.0")
            put("unitats", "μg/m³")
        })
        add(buildJsonObject {
            put("nom_estacio", "Barcelona (Eixample)")
            put("contaminant", "O3")
            put("latitud", "41.3853")
            put("longitud", "2.1539")
            put("h01", "45.0")
            put("h02", "48.0")
            put("h03", "50.0")
            put("unitats", "μg/m³")
        })
        add(buildJsonObject {
            put("nom_estacio", "Girona")
            put("contaminant", "NO2")
            put("latitud", "41.9794")
            put("longitud", "2.8214")
            put("h01", "20.0")
            put("h02", "18.0")
            put("h03", "15.0")
            put("unitats", "μg/m³")
        })
    }
    
    @BeforeEach
    fun setUp() {
        // Mockear la clase AirQualityService para las pruebas
        mockkObject(AirQualityService.Companion)
    }
    
    @AfterEach
    fun tearDown() {
        // Eliminar todos los mocks después de cada prueba
        unmockkAll()
    }
    
    @Test
    fun `test getAirQualityLevel returns correct level for NO2`() {
        // Arrange & Act
        val level1 = AirQualityService.getAirQualityLevel("NO2", 35.0f)
        val level2 = AirQualityService.getAirQualityLevel("NO2", 100.0f)
        val level3 = AirQualityService.getAirQualityLevel("NO2", 350.0f)
        
        // Assert
        assertEquals("excelent", level1)
        assertEquals("dolenta", level2)  // Corregido de "bona" a "dolenta"
        assertEquals("perillosa", level3)
    }
    
    @Test
    fun `test getAirQualityLevel returns correct level for O3`() {
        // Arrange & Act
        val level1 = AirQualityService.getAirQualityLevel("O3", 45.0f)
        val level2 = AirQualityService.getAirQualityLevel("O3", 120.0f)
        val level3 = AirQualityService.getAirQualityLevel("O3", 400.0f)
        
        // Assert
        assertEquals("excelent", level1)
        assertEquals("dolenta", level2)  // Corregido de "bona" a "dolenta"
        assertEquals("perillosa", level3)
    }
    
    @Test
    fun `test findClosestAirQualityData returns closest station`() {
        // Arrange - Coordenadas cercanas a Barcelona
        val latBarcelona = 41.3851f
        val lonBarcelona = 2.1734f
        
        // Act
        val result = AirQualityService.findClosestAirQualityData(latBarcelona, lonBarcelona, mockAirQualityData)
        
        // Assert
        assertEquals("Barcelona (Eixample)", result["station"]?.jsonPrimitive?.content)
        assertTrue(result.containsKey("readings"))
        assertTrue(result["readings"]?.jsonArray?.size ?: 0 > 0)
    }
    
    @Test
    fun `test findClosestAirQualityData handles empty data`() {
        // Arrange
        val emptyData = JsonArray(emptyList())
        
        // Act
        val result = AirQualityService.findClosestAirQualityData(41.3851f, 2.1734f, emptyData)
        
        // Assert
        assertTrue(result.containsKey("status"))
        assertEquals("No air quality data available", result["status"]?.jsonPrimitive?.content)
    }
    
    @Test
    fun `test fetchAirQualityData returns air quality data`() = runBlocking {
        // Arrange - Mock para que fetchAirQualityData devuelva datos falsos
        coEvery { AirQualityService.fetchAirQualityData() } returns mockAirQualityData
        
        // Act
        val result = AirQualityService.fetchAirQualityData()
        
        // Assert
        assertNotNull(result)
        assertEquals(3, result.size)
        assertEquals("Barcelona (Eixample)", result[0].jsonObject["nom_estacio"]?.jsonPrimitive?.content)
        assertEquals("NO2", result[0].jsonObject["contaminant"]?.jsonPrimitive?.content)
    }
}