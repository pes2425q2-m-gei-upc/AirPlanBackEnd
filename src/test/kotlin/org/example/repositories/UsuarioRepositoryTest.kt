package org.example.repositories

import org.example.database.ClienteTable
import org.example.database.UsuarioTable
import org.example.database.UserBlockTable
import org.example.enums.Idioma
import org.example.models.Usuario
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.sql.Connection

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UsuarioRepositoryTest {

    private lateinit var usuarioRepository: UsuarioRepository
    private val testEmail = "test@example.com"
    private val testUsername = "testuser"
    private lateinit var database: Database

    @BeforeAll
    fun setupDatabase() {
        // Configurar base de datos H2 en memoria para tests con opciones de compatibilidad PostgreSQL
        database = Database.connect(
            "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=FALSE",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )
        
        // Establecer el nivel de aislamiento de la transacción
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_REPEATABLE_READ
    }
    
    @BeforeEach
    fun setUp() {
        transaction(database) {
            // Ensure tables are created in the correct order (dependencies first)
            SchemaUtils.create(UsuarioTable, ClienteTable) // Create base tables
            SchemaUtils.create(UserBlockTable) // Create tables dependent on UsuarioTable
        }
        usuarioRepository = UsuarioRepository()
    }
    
    @AfterEach
    fun tearDown() {
        transaction(database) {
            // Drop tables in reverse order of creation/dependency
            SchemaUtils.drop(UserBlockTable) // Drop dependent tables first
            SchemaUtils.drop(ClienteTable, UsuarioTable) // Drop base tables
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
        
        // Agregar usuario a la base de datos usando la conexión específica
        val resultado = transaction(database) {
            usuarioRepository.agregarUsuario(usuario)
        }
        
        // Verificar que la operación fue exitosa
        assertTrue(resultado)
        
        // Obtener el usuario recién agregado
        val usuarioRecuperado = transaction(database) {
            usuarioRepository.obtenerUsuarioPorEmail(testEmail)
        }
        
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
        
        transaction(database) {
            usuarioRepository.agregarUsuario(usuario)
        }
        
        // Obtener usuario existente
        val usuarioExistente = transaction(database) {
            usuarioRepository.obtenerUsuarioPorEmail(testEmail)
        }
        assertNotNull(usuarioExistente)
        assertEquals(testEmail, usuarioExistente?.email)
        
        // Intentar obtener usuario inexistente
        val usuarioInexistente = transaction(database) {
            usuarioRepository.obtenerUsuarioPorEmail("noexiste@example.com")
        }
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
        
        transaction(database) {
            usuarioRepository.agregarUsuario(usuario)
        }
        
        // Obtener usuario existente por username
        val usuarioExistente = transaction(database) {
            usuarioRepository.obtenerUsuarioPorUsername(testUsername)
        }
        assertNotNull(usuarioExistente)
        assertEquals(testUsername, usuarioExistente?.username)
        
        // Intentar obtener usuario inexistente
        val usuarioInexistente = transaction(database) {
            usuarioRepository.obtenerUsuarioPorUsername("noexiste")
        }
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
        
        transaction(database) {
            usuarioRepository.agregarUsuario(usuario)
        }
        
        // Verificar estado inicial
        val usuarioInicial = transaction(database) {
            usuarioRepository.obtenerUsuarioPorEmail(testEmail)
        }
        assertFalse(usuarioInicial?.sesionIniciada ?: true)
        
        // Actualizar sesión a true
        transaction(database) {
            usuarioRepository.actualizarSesion(testEmail, true)
        }
        
        // Verificar que se actualizó
        val usuarioActualizado = transaction(database) {
            usuarioRepository.obtenerUsuarioPorEmail(testEmail)
        }
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
        
        transaction(database) {
            usuarioRepository.agregarUsuario(usuario)
        }
        
        // Verificar estado inicial
        val usuarioInicial = transaction(database) {
            usuarioRepository.obtenerUsuarioPorEmail(testEmail)
        }
        assertTrue(usuarioInicial?.sesionIniciada ?: false)
        
        // Cerrar sesión
        val resultado = transaction(database) {
            usuarioRepository.cerrarSesion(testEmail)
        }
        
        // Verificar que la operación fue exitosa
        assertTrue(resultado)
        
        // Verificar que la sesión se cerró
        val usuarioActualizado = transaction(database) {
            usuarioRepository.obtenerUsuarioPorEmail(testEmail)
        }
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
        
        transaction(database) {
            usuarioRepository.agregarUsuario(usuario)
        }
        
        // Actualizar varios campos del usuario
        val resultado = transaction(database) {
            usuarioRepository.actualizarUsuario(
                currentEmail = testEmail,
                nuevoNom = "Updated Name",
                nuevoUsername = "updateduser",
                nuevoIdioma = Idioma.Castellano.name,
                nuevoCorreo = "updated@example.com"
            )
        }
        
        // Verificar que la operación fue exitosa
        assertTrue(resultado)
        
        // Obtener el usuario con el nuevo email
        val usuarioActualizado = transaction(database) {
            usuarioRepository.obtenerUsuarioPorEmail("updated@example.com")
        }
        
        // Verificar los campos actualizados
        assertNotNull(usuarioActualizado)
        assertEquals("Updated Name", usuarioActualizado?.nom)
        assertEquals("updateduser", usuarioActualizado?.username)
        assertEquals(Idioma.Castellano.name, usuarioActualizado?.idioma.toString())
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
        
        transaction(database) {
            usuarioRepository.agregarUsuario(usuario)
        }
        
        // Actualizar solo el correo
        val nuevoEmail = "nuevo@example.com"
        val resultado = transaction(database) {
            usuarioRepository.actualizarCorreoDirecto(testEmail, nuevoEmail)
        }
        
        // Verificar que la operación fue exitosa
        assertTrue(resultado)
        
        // Verificar que el usuario ya no existe con el email anterior
        val usuarioViejo = transaction(database) {
            usuarioRepository.obtenerUsuarioPorEmail(testEmail)
        }
        assertNull(usuarioViejo)
        
        // Verificar que el usuario existe con el nuevo email
        val usuarioNuevo = transaction(database) {
            usuarioRepository.obtenerUsuarioPorEmail(nuevoEmail)
        }
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
        
        transaction(database) {
            usuarioRepository.agregarUsuario(usuario)
        }
        
        // Verificar que existe
        val usuarioExistente = transaction(database) {
            usuarioRepository.obtenerUsuarioPorEmail(testEmail)
        }
        assertNotNull(usuarioExistente)
        
        // Eliminar el usuario
        val resultado = transaction(database) {
            usuarioRepository.eliminarUsuario(testEmail)
        }
        
        // Verificar que la operación fue exitosa
        assertTrue(resultado)
        
        // Verificar que el usuario ya no existe
        val usuarioEliminado = transaction(database) {
            usuarioRepository.obtenerUsuarioPorEmail(testEmail)
        }
        assertNull(usuarioEliminado)
    }
}