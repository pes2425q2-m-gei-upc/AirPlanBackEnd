package org.example.services

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.lang.reflect.Field

/**
 * Tests para FirebaseAdminService usando un enfoque simplificado.
 * Estos tests se centran en verificar la lógica básica sin requerir
 * conexiones reales a Firebase.
 */
class FirebaseAdminServiceTest {
    
    @BeforeEach
    fun setUp() {
        // Reiniciar el estado del servicio antes de cada prueba
        resetInitializedState()
    }
    
    /**
     * Usa reflexión para reiniciar el estado de initialized a false
     */
    private fun resetInitializedState() {
        val initializedField: Field = FirebaseAdminService::class.java.getDeclaredField("initialized")
        initializedField.isAccessible = true
        initializedField.set(null, false)
    }
    
    @Test
    @DisplayName("Test estado inicial no inicializado")
    fun testInitialState() {
        // Verificar que inicialmente no está inicializado
        assertFalse(FirebaseAdminService.isInitialized())
    }
    
    @Test
    @DisplayName("Test métodos de actualización requieren inicialización")
    fun testUpdateMethodsRequireInitialization() {
        // Sin inicializar, los métodos de actualización deben devolver false o errores
        val result = FirebaseAdminService.updateUserEmail("test@example.com", "new@example.com")
        assertFalse(result, "El método debería devolver false cuando el servicio no está inicializado")
    }
    
    @Test
    @DisplayName("Test verificación de existencia de usuario cuando no inicializado")
    fun testUserExistsWhenNotInitialized() {
        // Sin inicializar, el método debe devolver false
        val result = FirebaseAdminService.userExistsByEmail("test@example.com")
        assertFalse(result, "El método debería devolver false cuando el servicio no está inicializado")
    }
    
    @Test
    @DisplayName("Test respuesta correcta al actualizar email con servicio no inicializado")
    fun testUpdateEmailAndCreateTokenWhenNotInitialized() {
        // Sin inicializar, el método debe devolver un mapa con success=false
        val result = FirebaseAdminService.updateEmailAndCreateCustomToken("old@example.com", "new@example.com")
        
        assertFalse(result["success"] as Boolean)
        assertNotNull(result["error"])
        assertTrue(result["error"].toString().contains("no está inicializado", ignoreCase = true))
    }
    
    @Test
    @DisplayName("Test manejo de estado inicializado")
    fun testInitializedState() {
        // Simular que el servicio está inicializado
        setInitializedState(true)
        
        // Verificar que isInitialized devuelve true
        assertTrue(FirebaseAdminService.isInitialized())
    }
    
    /**
     * Función auxiliar para establecer el estado de inicialización para pruebas
     */
    private fun setInitializedState(state: Boolean) {
        val initializedField: Field = FirebaseAdminService::class.java.getDeclaredField("initialized")
        initializedField.isAccessible = true
        initializedField.set(null, state)
    }
}