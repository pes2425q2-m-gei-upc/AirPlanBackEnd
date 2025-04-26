package org.example.models

import kotlinx.serialization.Serializable

/**
 * Clase para respuestas de la API.
 * Esta clase serializable proporciona una estructura estándar para las respuestas JSON.
 * 
 * @param success indica si la operación fue exitosa
 * @param message mensaje de éxito (opcional)
 * @param error mensaje de error (opcional)
 */
@Serializable
data class ApiResponse(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null
)