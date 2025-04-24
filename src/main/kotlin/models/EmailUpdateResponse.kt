package org.example.models

import kotlinx.serialization.Serializable

/**
 * Clase para respuestas de actualización de email con token personalizado
 * 
 * @param success indica si la operación fue exitosa
 * @param message mensaje de éxito (opcional)
 * @param error mensaje de error (opcional)
 * @param customToken token personalizado para mantener la sesión (opcional)
 */
@Serializable
data class EmailUpdateResponse(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null,
    val customToken: String? = null
)