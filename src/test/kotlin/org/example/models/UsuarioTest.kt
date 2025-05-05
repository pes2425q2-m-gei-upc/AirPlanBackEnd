package org.example.models

import org.example.enums.Idioma
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

class UsuarioTest {
    
    private lateinit var usuario: Usuario
    
    @BeforeEach
    fun setUp() {
        usuario = Usuario(
            username = "testuser",
            nom = "Test User",
            email = "test@example.com",
            idioma = Idioma.Catala,
            sesionIniciada = false,
            isAdmin = false
        )
    }
    
    @Test
    @DisplayName("Test de creación de usuario")
    fun testCreacionUsuario() {
        assertEquals("testuser", usuario.username)
        assertEquals("Test User", usuario.nom)
        assertEquals("test@example.com", usuario.email)
        assertEquals(Idioma.Catala, usuario.idioma)
        assertFalse(usuario.sesionIniciada)
        assertFalse(usuario.isAdmin)
        assertTrue(usuario.activitats.isEmpty())
    }
    
    @Test
    @DisplayName("Test de modificación de usuario")
    fun testModificarUsuario() {
        val resultado = usuario.modificarUsuario(
            nuevoNom = "Updated Name",
            nuevoIdioma = Idioma.Castellano
        )
        
        assertTrue(resultado)
        assertEquals("Updated Name", usuario.nom)
        assertEquals(Idioma.Castellano, usuario.idioma)
        // Verificar que otros campos no cambiaron
        assertEquals("testuser", usuario.username)
        assertEquals("test@example.com", usuario.email)
    }
    
    @Test
    @DisplayName("Test de modificación parcial de usuario")
    fun testModificarUsuarioParcial() {
        // Solo cambiar el nombre
        val resultado1 = usuario.modificarUsuario(nuevoNom = "Only Name Changed")
        
        assertTrue(resultado1)
        assertEquals("Only Name Changed", usuario.nom)
        assertEquals(Idioma.Catala, usuario.idioma) // No debe cambiar
        
        // Solo cambiar el idioma
        val resultado2 = usuario.modificarUsuario(nuevoIdioma = Idioma.English)
        
        assertTrue(resultado2)
        assertEquals("Only Name Changed", usuario.nom) // No debe cambiar
        assertEquals(Idioma.English, usuario.idioma)
    }
}