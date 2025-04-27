package org.example.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import org.example.controllers.ControladorUsuarios
import org.example.models.Usuario
import org.example.models.UserTypeInfo
import org.example.models.EmailUpdateResponse // Añadir importación
import org.example.repositories.UsuarioRepository
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.transactions.transaction
import org.example.enums.Idioma
import org.jetbrains.exposed.sql.update
import org.example.database.UsuarioTable
import kotlinx.coroutines.runBlocking
import org.example.websocket.WebSocketManager
import org.example.services.FirebaseAdminService // Importación añadida
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

fun Route.usuarioRoutes() {
    val usuarioController = ControladorUsuarios(UsuarioRepository())
    route("/api/usuaris") {
        post("/crear") {
            try {
                val receivedText = call.receiveText()

                // Deserialitzar manualment el JSON
                val usuario = kotlinx.serialization.json.Json.decodeFromString<Usuario>(receivedText)
                println("Usuari deserialitzat: $usuario")

                val resultado = usuarioController.crearUsuario(
                    username = usuario.username,
                    nom = usuario.nom,
                    email = usuario.email,
                    idioma = usuario.idioma.toString(), // Convertir Idioma a String
                    isAdmin = usuario.isAdmin,
                )

                if (resultado != null) {
                    call.respond(HttpStatusCode.Created, "Usuari creat correctament")
                } else {
                    call.respond(HttpStatusCode.Conflict, "L'usuari ja existeix")
                }
            } catch (e: ExposedSQLException) {
                // Capturar errores específicos de la base de datos
                val errorMessage = when {
                    e.message?.contains("username") == true -> "El nom d'usuari ja està en ús."
                    e.message?.contains("email") == true -> "El correu electrònic ja està en ús."
                    else -> "Error en la base de dades: ${e.message}"
                }
                call.respond(HttpStatusCode.Conflict, errorMessage)
            } catch (e: Exception) {
                println("Error: ${e.message}")
                call.respond(HttpStatusCode.BadRequest, "Error en processar la petició")
            }
        }
        
        delete("/eliminar/{email}") {
            val email = call.parameters["email"]
            // Obtener el clientId de los headers o query params
            val clientId = call.request.queryParameters["clientId"]
            
            if (email != null) {
                // Pasar el clientId al controlador
                val resultado = usuarioController.eliminarUsuario(email, clientId)
                if (resultado) {
                    call.respond(HttpStatusCode.OK, "Usuari eliminat correctament")
                } else {
                    call.respond(HttpStatusCode.NotFound, "Usuari no trobat")
                }
            } else {
                call.respond(HttpStatusCode.BadRequest, "Cal proporcionar un email")
            }
        }
        
        post("/login") {
            try {
                val params = call.receive<Map<String, String>>()
                val email = params["email"]
                val username = params["username"] // Nuevo parámetro: username
                val clientId = params["clientId"] // Añadir clientId para identificar el dispositivo
                
                if (email != null) {
                    // Si tenemos un username, intentamos buscar primero por username
                    val usuario = if (username != null) {
                        println("🔍 Buscando usuario por username: $username")
                        usuarioController.obtenerUsuarioPorUsername(username)
                    } else {
                        // Como fallback, buscar por email
                        println("🔍 Buscando usuario por email: $email")
                        usuarioController.obtenerUsuarioPorEmail(email)
                    }
                    
                    if (usuario != null) {
                        // Actualizar el atributo sesionIniciada a true
                        usuario.sesionIniciada = true
                        
                        // Guardar el cambio en la base de datos
                        transaction {
                            usuarioController.actualizarSesion(usuario.email, true)
                        }

                        // Responder con éxito
                        val respuesta = mapOf("isAdmin" to usuario.isAdmin)
                        call.respond(HttpStatusCode.OK, respuesta)
                    } else {
                        // Responder que el login falló
                        call.respond(HttpStatusCode.Unauthorized, "Usuario no encontrado")
                    }
                } else {
                    // Responder que falta el parámetro email
                    call.respond(HttpStatusCode.BadRequest, "Falta el parámetro email")
                }
            } catch (e: Exception) {
                println("Error en el login: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, "Error procesando el login")
            }
        }
        
        post("/logout") {
            try {
                // Recibir el JSON como un mapa (o JsonObject)
                val requestBody = call.receive<Map<String, String>>()
                val email = requestBody["email"] // Extraer el valor del campo "email"

                if (!email.isNullOrBlank()) {
                    transaction {
                        usuarioController.actualizarSesion(email, false)
                    }
                } else {
                    call.respond(HttpStatusCode.BadRequest, "El campo 'email' está vacío o no es válido")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Error en el formato de la solicitud")
            }
        }
        
        put("/editar/{email}") {
            println("Ha llegado al backend")
            val currentEmail = call.parameters["email"]
            println("El correo actual es: $currentEmail")
            if (currentEmail != null) {
                try {
                    val updatedData = call.receive<Map<String, String>>()
                    val nuevoNom = updatedData["nom"]
                    val nuevoUsername = updatedData["username"]
                    val nuevoIdioma = updatedData["idioma"]
                    val nuevoCorreo = updatedData["correo"]

                    val resultado = usuarioController.modificarUsuario(currentEmail, nuevoNom, nuevoUsername, nuevoIdioma, nuevoCorreo)

                    if (resultado) {
                        call.respond(HttpStatusCode.OK, "Usuario actualizado correctamente")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Usuario no encontrado")
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Error al procesar la solicitud: ${e.message}")
                }
            } else {
                call.respond(HttpStatusCode.BadRequest, "Debe proporcionar un email válido")
            }
        }

        // Añadir un nuevo endpoint para obtener usuario por username
        get("/usuario-por-username/{username}") {
            val username = call.parameters["username"]

            if (username != null) {
                val usuario = usuarioController.obtenerUsuarioPorUsername(username)
                if (usuario != null) {
                    // Devolver los datos del usuario en lugar de solo un mensaje
                    call.respond(HttpStatusCode.OK, usuario)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Usuario no encontrado")
                }
            } else {
                call.respond(HttpStatusCode.BadRequest, "Debe proporcionar un username")
            }
        }

        // Endpoint para notificar eliminación de cuenta a otros dispositivos
        post("/notifications/account-deleted") {
            try {
                val request = call.receive<Map<String, String>>()
                val email = request["email"]
                val username = request["username"]
                val clientId = request["clientId"]
                
                if (email != null && username != null) {
                    println("🗑️ Notificación recibida para cuenta eliminada: $username ($email)")
                    
                    // Usar el WebSocketManager para enviar notificaciones a otros dispositivos
                    runBlocking {
                        WebSocketManager.instance.notifyAccountDeleted(
                            username = username,
                            email = email,
                            clientId = clientId
                        )
                    }
                    
                    call.respond(HttpStatusCode.OK, "Notificación enviada correctamente")
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Datos incompletos: se requiere email y username")
                }
            } catch (e: Exception) {
                println("❌ Error enviando notificación de cuenta eliminada: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, "Error: ${e.message}")
            }
        }

        // Endpoint para obtener tipo de usuario (admin o cliente) y nivel si es cliente
        get("/tipo-usuario/{username}") {
            val username = call.parameters["username"]
            
            if (username != null) {
                val tipoInfo = usuarioController.obtenerTipoYNivelUsuario(username)
                
                if (tipoInfo.tipo == "error") {
                    call.respond(HttpStatusCode.NotFound, tipoInfo)
                } else {
                    call.respond(HttpStatusCode.OK, tipoInfo)
                }
            } else {
                call.respond(
                    HttpStatusCode.BadRequest, 
                    UserTypeInfo(
                        tipo = "error", 
                        username = "", 
                        error = "Debe proporcionar un nombre de usuario"
                    )
                )
            }
        }

        // Endpoint unificado para actualizar todo el perfil incluyendo correo electrónico
        post("/updateFullProfile") {
            try {
                val request = call.receive<Map<String, String>>()
                val currentEmail = request["currentEmail"]
                val newEmail = request["newEmail"]
                val password = request["password"] // Para reautenticación
                val clientId = request["clientId"] // Para notificaciones
                val username = request["username"]
                val oldUsername = request["oldUsername"]
                val nom = request["nom"]
                val idioma = request["idioma"]
                val photoURL = request["photoURL"]
                
                if (currentEmail == null) {
                    call.respond(HttpStatusCode.BadRequest, EmailUpdateResponse(
                        success = false,
                        error = "El correo electrónico actual es obligatorio"
                    ))
                    return@post
                }
                
                // Preparamos un mapa con los datos actualizados (excluyendo los campos de control)
                val updatedData = mutableMapOf<String, String>()
                if (username != null) updatedData["username"] = username
                if (nom != null) updatedData["nom"] = nom
                if (idioma != null) updatedData["idioma"] = idioma
                if (photoURL != null) updatedData["photoURL"] = photoURL
                
                var emailChanged = false
                var customToken: String? = null
                
                // Si hay un cambio de correo, generamos primero el token y luego actualizamos el correo
                if (newEmail != null && currentEmail != newEmail) {
                    emailChanged = true
                    println("📱 Actualizando correo en perfil completo: $currentEmail → $newEmail")
                    
                    // Generar token personalizado y actualizar correo usando FirebaseAdminService
                    if (FirebaseAdminService.isInitialized()) {
                        val firebaseResult = FirebaseAdminService.updateEmailAndCreateCustomToken(currentEmail, newEmail)
                        customToken = firebaseResult["customToken"]?.toString()
                        
                        if (firebaseResult["success"] == true) {
                            println("✅ Correo actualizado en Firebase Auth y token personalizado generado")
                        } else {
                            println("❌ Error al actualizar correo en Firebase Auth: ${firebaseResult["error"]}")
                        }
                    } else {
                        println("⚠️ Firebase Admin SDK no inicializado, no se puede actualizar email en Firebase Auth")
                    }
                    
                    // Actualizar el correo en la base de datos
                    val dbResult = usuarioController.actualizarCorreoDirecto(currentEmail, newEmail, clientId)
                    
                    if (!dbResult) {
                        call.respond(HttpStatusCode.InternalServerError, EmailUpdateResponse(
                            success = false,
                            error = "No se pudo actualizar el correo en la base de datos"
                        ))
                        return@post
                    }
                }
                
                // Actualizamos el resto del perfil
                val profileEmail = if (emailChanged) newEmail!! else currentEmail
                val profileResult = usuarioController.modificarUsuario(
                    currentEmail = profileEmail,
                    nuevoNom = nom,
                    nuevoUsername = username, 
                    nuevoIdioma = idioma,
                    nuevoCorreo = null // El correo ya fue actualizado si era necesario
                )
                
                if (profileResult) {
                    // Notificar a otros dispositivos sobre la actualización del perfil (opcional)
                    if (clientId != null && oldUsername != null) {
                        try {
                            if (emailChanged) {
                                println("✅ Notificación de cambio de correo ya enviada previamente")
                            } else {
                                runBlocking {
                                    WebSocketManager.instance.notifyProfileUpdate(
                                        oldUsername,
                                        profileEmail,
                                        updatedData.keys.toList(),
                                        clientId
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            println("⚠️ Error enviando notificación de actualización de perfil: ${e.message}")
                        }
                    }
                    
                    // Respuesta exitosa con token si se cambió el correo
                    call.respond(HttpStatusCode.OK, EmailUpdateResponse(
                        success = true,
                        message = if (emailChanged) "Perfil actualizado y correo modificado correctamente" else "Perfil actualizado correctamente",
                        customToken = customToken
                    ))
                } else {
                    call.respond(HttpStatusCode.InternalServerError, EmailUpdateResponse(
                        success = false,
                        error = "Error al actualizar el perfil",
                        customToken = customToken // Devolvemos el token aunque haya fallado la actualización del perfil
                    ))
                }
            } catch (e: Exception) {
                println("❌ Error en actualización completa de perfil: ${e.message}")
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, EmailUpdateResponse(
                    success = false,
                    error = "Error: ${e.message}"
                ))
            }
        }
    }
    
    // Nuevo endpoint para verificar el cambio de correo electrónico con parámetros en la URL
    route("/api/usuaris") {
        // Nuevo endpoint que usa segmentos de ruta en lugar de parámetros de consulta
        get("/verify-email/{oldEmail}/{newEmail}") {
            try {
                // Obtener los parámetros de la ruta y decodificarlos correctamente
                val oldEmail = call.parameters["oldEmail"]?.let { 
                    java.net.URLDecoder.decode(it, "UTF-8")
                }
                val newEmail = call.parameters["newEmail"]?.let { 
                    java.net.URLDecoder.decode(it, "UTF-8")
                }
                
                if (oldEmail != null && newEmail != null) {
                    println("📧 Verificación de correo recibida (path): $oldEmail → $newEmail")
                    
                    // 1. Actualizar el correo en la base de datos
                    val dbUpdateResult = usuarioController.actualizarCorreoDirecto(oldEmail, newEmail)
                    
                    if (dbUpdateResult) {
                        // 2. Construir la URL para redirigir al usuario a una página de éxito en el frontend
                        val redirectUrl = "http://localhost:8000/#/email-verification-success?email=$newEmail"
                        call.respondRedirect(redirectUrl)
                    } else {
                        // Si falla la actualización en la base de datos, redirigir a una página de error
                        val errorUrl = "http://localhost:8000/#/email-verification-error?reason=database"
                        call.respondRedirect(errorUrl)
                    }
                } else {
                    // Parámetros incompletos (no debería ocurrir con segmentos de ruta)
                    val errorUrl = "http://localhost:8000/#/email-verification-error?reason=missing-params"
                    call.respondRedirect(errorUrl)
                }
            } catch (e: Exception) {
                println("❌ Error en la verificación del correo: ${e.message}")
                // Redirigir a una página de error con detalles
                val errorUrl = "http://localhost:8000/#/email-verification-error?reason=server-error"
                call.respondRedirect(errorUrl)
            }
        }
    }
}