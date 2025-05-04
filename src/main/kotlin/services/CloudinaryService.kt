package org.example.services

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import org.example.config.CloudinaryConfig
import java.io.File
import java.util.Base64

/**
 * Servicio para manejar operaciones con Cloudinary
 */
class CloudinaryService {
    
    private val cloudinary: Cloudinary
    
    companion object {
        private var instance: CloudinaryService? = null
        
        fun getInstance(): CloudinaryService {
            if (instance == null) {
                instance = CloudinaryService()
            }
            return instance!!
        }
    }
    
    init {
        // Inicializar con las credenciales de Cloudinary desde la configuración
        cloudinary = Cloudinary(ObjectUtils.asMap(
            "cloud_name", CloudinaryConfig.CLOUD_NAME,
            "api_key", CloudinaryConfig.API_KEY,
            "api_secret", CloudinaryConfig.API_SECRET,
            "secure", true
        ))
    }
    
    /**
     * Sube una imagen a Cloudinary desde un archivo Base64
     * @param base64Image Imagen codificada en Base64
     * @param publicId Identificador público para la imagen (opcional)
     * @return URL de la imagen subida
     */
    fun uploadImage(base64Image: String, publicId: String? = null): String {
        try {
            // Decodificar la imagen en Base64
            val imageBytes = Base64.getDecoder().decode(base64Image)
            
            // Crear un archivo temporal
            val tempFile = File.createTempFile("upload_", ".jpg")
            tempFile.deleteOnExit()
            tempFile.writeBytes(imageBytes)
            
            // Configurar opciones de carga
            val options = mutableMapOf<String, Any>(
                "resource_type" to "image",
                "folder" to "airplan_images"
            )
            
            // Añadir publicId si se proporciona
            publicId?.let { options["public_id"] = it }
            
            // Subir la imagen
            val uploadResult = cloudinary.uploader().upload(tempFile, options)
            
            // Eliminar el archivo temporal
            tempFile.delete()
            
            // Devolver la URL segura de la imagen
            return uploadResult["secure_url"].toString()
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Error al subir la imagen a Cloudinary: ${e.message}")
        }
    }
    
    /**
     * Elimina una imagen de Cloudinary por su URL o public_id
     * @param imageUrl URL de la imagen a eliminar
     * @return true si se eliminó correctamente, false en caso contrario
     */
    fun deleteImage(imageUrl: String): Boolean {
        try {
            // Extraer el public_id de la URL
            val publicId = extractPublicIdFromUrl(imageUrl)
            
            // Eliminar la imagen
            val result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap())
            
            // Comprobar si se eliminó correctamente
            return result.get("result") == "ok"
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Extrae el public_id de una URL de Cloudinary
     */
    private fun extractPublicIdFromUrl(url: String): String {
        // La URL típica de Cloudinary es como:
        // https://res.cloudinary.com/cloud_name/image/upload/v1234567890/folder/nombre_imagen.jpg
        
        // Quitamos la extensión del archivo
        val withoutExtension = url.substringBeforeLast(".")
        
        // Obtenemos la última parte de la URL que incluye el public_id
        val fullPath = withoutExtension.substringAfterLast("/upload/")
        
        // Si hay un número de versión, lo eliminamos
        return if (fullPath.contains("/v")) {
            fullPath.substringAfter("/")
        } else {
            fullPath
        }
    }
}