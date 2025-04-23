package org.example.repositories

import org.example.database.ClienteTable
import org.example.database.UsuarioTable
import org.example.enums.Idioma
import org.example.models.Usuario
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.sql.Connection

class UsuarioRepositoryTest {

    private lateinit var usuarioRepository: UsuarioRepository
    private val testEmail = "test@example.com"
    private val testUsername = "testuser"

    @BeforeEach
    fun setUp() {
        // Configurar base de datos H2 en memoria para tests
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        
        // Establecer el nivel de aislamiento de la transacción
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_REPEATABLE_READ
        
        // Crear las tablas en la base de datos
        transaction {
            SchemaUtils.create(UsuarioTable, ClienteTable)
        }
        
        usuarioRepository = UsuarioRepository()
    }
    
    @AfterEach
    fun tearDown() {
        // Eliminar las tablas después de cada test
        transaction {
            SchemaUtils.drop(UsuarioTable, ClienteTable)
        }
    }
    
    @Test
    @DisplayName("Test agregar un usuario")
    fun testAgregarUsuario() {
        // Crear usuario de prueba
        val usuario = Usuario(
            username = testUsername,
            nom = "Test User",
            email = testEmail,
            idioma = Idioma.Catala,
            sesionIniciada = false,
            isAdmin = false
        )
        
        // Agregar usuario a la base de datos
        val resultado = usuarioRepository.agregarUsuario(usuario)
        
        // Verificar que la operación fue exitosa
        assertTrue(resultado)
        
        // Obtener el usuario recién agregado
        val usuarioRecuperado = usuarioRepository.obtenerUsuarioPorEmail(testEmail)
        
        // Verificar que se recuperó correctamente
        assertNotNull(usuarioRecuperado)
        assertEquals(testUsername, usuarioRecuperado?.username)
        assertEquals("Test User", usuarioRecuperado?.nom)
        assertEquals(testEmail, usuarioRecuperado?.email)
        assertEquals(Idioma.Catala, usuarioRecuperado?.idioma)
        assertFalse(usuarioRecuperado?.sesionIniciada ?: true)
        assertFalse(usuarioRecuperado?.isAdmin ?: true)
    }
    
    @Test
    @DisplayName("Test obtener usuario por email")
    fun testObtenerUsuarioPorEmail() {
        // Primero agregar un usuario para probarlo
        val usuario = Usuario(
            username = testUsername,
            nom = "Test User",
            email = testEmail,
            idioma = Idioma.Catala,
            sesionIniciada = false,
            isAdmin = false
        )
        usuarioRepository.agregarUsuario(usuario)
        
        // Obtener usuario existente
        val usuarioExistente = usuarioRepository.obtenerUsuarioPorEmail(testEmail)
        assertNotNull(usuarioExistente)
        assertEquals(testEmail, usuarioExistente?.email)
        
        // Intentar obtener usuario inexistente
        val usuarioInexistente = usuarioRepository.obtenerUsuarioPorEmail("noexiste@example.com")
        assertNull(usuarioInexistente)
    }
    
    @Test
    @DisplayName("Test obtener usuario por username")
    fun testObtenerUsuarioPorUsername() {
        // Primero agregar un usuario para probarlo
        val usuario = Usuario(
            username = testUsername,
            nom = "Test User",
            email = testEmail,
            idioma = Idioma.Catala,
            sesionIniciada = false,
            isAdmin = false
        )
        usuarioRepository.agregarUsuario(usuario)
        
        // Obtener usuario existente por username
        val usuarioExistente = usuarioRepository.obtenerUsuarioPorUsername(testUsername)
        assertNotNull(usuarioExistente)
        assertEquals(testUsername, usuarioExistente?.username)
        
        // Intentar obtener usuario inexistente
        val usuarioInexistente = usuarioRepository.obtenerUsuarioPorUsername("noexiste")
        assertNull(usuarioInexistente)
    }
    
    @Test
    @DisplayName("Test actualizar sesión del usuario")
    fun testActualizarSesion() {
        // Primero agregar un usuario para probarlo
        val usuario = Usuario(
            username = testUsername,
            nom = "Test User",
            email = testEmail,
            idioma = Idioma.Catala,
            sesionIniciada = false,
            isAdmin = false
        )
        usuarioRepository.agregarUsuario(usuario)
        
        // Verificar estado inicial
        val usuarioInicial = usuarioRepository.obtenerUsuarioPorEmail(testEmail)
        assertFalse(usuarioInicial?.sesionIniciada ?: true)
        
        // Actualizar sesión a true
        usuarioRepository.actualizarSesion(testEmail, true)
        
        // Verificar que se actualizó
        val usuarioActualizado = usuarioRepository.obtenerUsuarioPorEmail(testEmail)
        assertTrue(usuarioActualizado?.sesionIniciada ?: false)
    }
    
    @Test
    @DisplayName("Test cerrar sesión del usuario")
    fun testCerrarSesion() {
        // Primero agregar un usuario para probarlo y ponerle sesión iniciada
        val usuario = Usuario(
            username = testUsername,
            nom = "Test User",
            email = testEmail,
            idioma = Idioma.Catala,
            sesionIniciada = true,
            isAdmin = false
        )
        usuarioRepository.agregarUsuario(usuario)
        
        // Verificar estado inicial
        val usuarioInicial = usuarioRepository.obtenerUsuarioPorEmail(testEmail)
        assertTrue(usuarioInicial?.sesionIniciada ?: false)
        
        // Cerrar sesión
        val resultado = usuarioRepository.cerrarSesion(testEmail)
        
        // Verificar que la operación fue exitosa
        assertTrue(resultado)
        
        // Verificar que la sesión se cerró
        val usuarioActualizado = usuarioRepository.obtenerUsuarioPorEmail(testEmail)
        assertFalse(usuarioActualizado?.sesionIniciada ?: true)
    }
    
    @Test
    @DisplayName("Test actualizar usuario")
    fun testActualizarUsuario() {
        // Primero agregar un usuario para probarlo
        val usuario = Usuario(
            username = testUsername,
            nom = "Test User",
            email = testEmail,
            idioma = Idioma.Catala,
            sesionIniciada = false,
            isAdmin = false
        )
        usuarioRepository.agregarUsuario(usuario)
        
        // Actualizar varios campos del usuario
        val resultado = usuarioRepository.actualizarUsuario(
            currentEmail = testEmail,
            nuevoNom = "Updated Name",
            nuevoUsername = "updateduser",
            nuevoIdioma = Idioma.Castellano.toString(),
            nuevoCorreo = "updated@example.com"
        )
        
        // Verificar que la operación fue exitosa
        assertTrue(resultado)
        
        // Obtener el usuario con el nuevo email
        val usuarioActualizado = usuarioRepository.obtenerUsuarioPorEmail("updated@example.com")
        
        // Verificar los campos actualizados
        assertNotNull(usuarioActualizado)
        assertEquals("Updated Name", usuarioActualizado?.nom)
        assertEquals("updateduser", usuarioActualizado?.username)
        assertEquals(Idioma.Castellano.toString(), usuarioActualizado?.idioma.toString())
        assertEquals("updated@example.com", usuarioActualizado?.email)
    }
    
    @Test
    @DisplayName("Test actualizar correo directamente")
    fun testActualizarCorreoDirecto() {
        // Primero agregar un usuario para probarlo
        val usuario = Usuario(
            username = testUsername,
            nom = "Test User",
            email = testEmail,
            idioma = Idioma.Catala,
            sesionIniciada = false,
            isAdmin = false
        )
        usuarioRepository.agregarUsuario(usuario)
        
        // Actualizar solo el correo
        val nuevoEmail = "nuevo@example.com"
        val resultado = usuarioRepository.actualizarCorreoDirecto(testEmail, nuevoEmail)
        
        // Verificar que la operación fue exitosa
        assertTrue(resultado)
        
        // Verificar que el usuario ya no existe con el email anterior
        val usuarioViejo = usuarioRepository.obtenerUsuarioPorEmail(testEmail)
        assertNull(usuarioViejo)
        
        // Verificar que el usuario existe con el nuevo email
        val usuarioNuevo = usuarioRepository.obtenerUsuarioPorEmail(nuevoEmail)
        assertNotNull(usuarioNuevo)
        assertEquals(nuevoEmail, usuarioNuevo?.email)
    }
    
    @Test
    @DisplayName("Test eliminar usuario")
    fun testEliminarUsuario() {
        // Primero agregar un usuario para probarlo
        val usuario = Usuario(
            username = testUsername,
            nom = "Test User",
            email = testEmail,
            idioma = Idioma.Catala,
            sesionIniciada = false,
            isAdmin = false
        )
        usuarioRepository.agregarUsuario(usuario)
        
        // Verificar que existe
        val usuarioExistente = usuarioRepository.obtenerUsuarioPorEmail(testEmail)
        assertNotNull(usuarioExistente)
        
        // Eliminar el usuario
        val resultado = usuarioRepository.eliminarUsuario(testEmail)
        
        // Verificar que la operación fue exitosa
        assertTrue(resultado)
        
        // Verificar que el usuario ya no existe
        val usuarioEliminado = usuarioRepository.obtenerUsuarioPorEmail(testEmail)
        assertNull(usuarioEliminado)
    }
}