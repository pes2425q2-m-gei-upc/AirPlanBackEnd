package org.example.services

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserRecord.UpdateRequest
import java.io.FileInputStream
import java.io.IOException

/**
 * Servicio para interactuar con Firebase Admin SDK
 * Permite operaciones administrativas como actualizar emails, verificar tokens, etc.
 */
object FirebaseAdminService {
    private var initialized = false

    /**
     * Inicializa Firebase Admin SDK usando credenciales de archivo
     */
    fun initialize() {
        if (initialized) return
        
        try {
            // Intentar usar credenciales de archivo local primero
            val serviceAccount = FileInputStream("src/main/resources/firebase-service-account.json")
            
            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build()
            
            FirebaseApp.initializeApp(options)
            initialized = true
            println("✅ Firebase Admin SDK inicializado correctamente desde archivo")
        } catch (e: IOException) {
            // Si no hay archivo, intentar usar credenciales por defecto
            try {
                val options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .build()
                
                FirebaseApp.initializeApp(options)
                initialized = true
                println("✅ Firebase Admin SDK inicializado con credenciales por defecto")
            } catch (e2: Exception) {
                println("❌ Error al inicializar Firebase Admin SDK: ${e2.message}")
            }
        } catch (e: Exception) {
            println("❌ Error al inicializar Firebase Admin SDK: ${e.message}")
        }
    }

    /**
     * Verifica si el servicio está inicializado
     * 
     * @return true si Firebase Admin SDK está inicializado, false en caso contrario
     */
    fun isInitialized(): Boolean {
        return initialized
    }

    /**
     * Actualiza el correo electrónico de un usuario en Firebase
     * 
     * @param oldEmail Correo electrónico actual
     * @param newEmail Nuevo correo electrónico
     * @return true si la actualización fue exitosa, false en caso contrario
     */
    fun updateUserEmail(oldEmail: String, newEmail: String): Boolean {
        if (!initialized) {
            println("❌ Firebase Admin SDK no está inicializado")
            return false
        }
        
        return try {
            // Buscar el usuario por su correo electrónico actual
            val user = FirebaseAuth.getInstance().getUserByEmail(oldEmail)
            
            // Crear solicitud de actualización
            val request = UpdateRequest(user.uid)
                .setEmail(newEmail)
            
            // Ejecutar la actualización
            FirebaseAuth.getInstance().updateUser(request)
            
            println("✅ Email actualizado en Firebase para uid=${user.uid}: $oldEmail → $newEmail")
            true
        } catch (e: Exception) {
            println("❌ Error al actualizar email en Firebase: ${e.message}")
            false
        }
    }
    
    /**
     * Actualiza el correo electrónico y genera un token personalizado para mantener la sesión activa
     * 
     * @param oldEmail Correo electrónico actual
     * @param newEmail Nuevo correo electrónico
     * @return Mapa con el resultado y el token personalizado si fue exitoso
     */
    fun updateEmailAndCreateCustomToken(oldEmail: String, newEmail: String): Map<String, Any?> {
        if (!initialized) {
            println("❌ Firebase Admin SDK no está inicializado")
            return mapOf("success" to false, "error" to "Firebase Admin SDK no está inicializado")
        }
        
        return try {
            // Buscar el usuario por su correo electrónico actual
            val user = FirebaseAuth.getInstance().getUserByEmail(oldEmail)
            val uid = user.uid
            
            // Crear solicitud de actualización
            val request = UpdateRequest(uid)
                .setEmail(newEmail)
            
            // Ejecutar la actualización
            FirebaseAuth.getInstance().updateUser(request)
            
            // Generar token personalizado para mantener la sesión
            val customToken = FirebaseAuth.getInstance().createCustomToken(uid)
            
            println("✅ Email actualizado en Firebase para uid=$uid: $oldEmail → $newEmail")
            println("✅ Token personalizado generado para mantener sesión")
            
            mapOf(
                "success" to true,
                "customToken" to customToken,
                "message" to "Correo actualizado correctamente"
            )
        } catch (e: Exception) {
            println("❌ Error al actualizar email en Firebase: ${e.message}")
            mapOf(
                "success" to false,
                "error" to e.message
            )
        }
    }

    /**
     * Verifica si un usuario existe en Firebase por su correo electrónico
     * 
     * @param email Correo electrónico a verificar
     * @return true si el usuario existe, false en caso contrario
     */
    fun userExistsByEmail(email: String): Boolean {
        if (!initialized) return false
        
        return try {
            FirebaseAuth.getInstance().getUserByEmail(email)
            true
        } catch (e: Exception) {
            false
        }
    }
}