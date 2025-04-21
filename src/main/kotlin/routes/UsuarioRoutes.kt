package org.example.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import org.example.controllers.ControladorUsuarios
import org.example.models.Usuario
import org.example.models.UserTypeInfo // Added import for UserTypeInfo
import org.example.repositories.UsuarioRepository
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.transactions.transaction
import org.example.enums.Idioma
import org.jetbrains.exposed.sql.update
import org.example.database.UsuarioTable
import kotlinx.coroutines.runBlocking
import org.example.websocket.WebSocketManager

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
        get("/usuarios/{email}") {
            val email = call.parameters["email"]

            if (email != null) {
                val usuario = usuarioController.obtenerUsuarioPorEmail(email)
                if (usuario != null) {
                    // Devolver los datos del usuario en lugar de solo un mensaje
                    call.respond(HttpStatusCode.OK, usuario)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Usuario no encontrado")
                }
            } else {
                call.respond(HttpStatusCode.BadRequest, "Debe proporcionar un email")
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
                        // Comprobar si el email de Firebase coincide con el de la base de datos
                        val firebaseEmail = email // El email que llega desde Firebase
                        val databaseEmail = usuario.email // Email en la base de datos
                        
                        if (firebaseEmail != databaseEmail) {
                            println("⚠️ Correo en Firebase diferente al de la base de datos durante login")
                            println("   - Firebase: $firebaseEmail")
                            println("   - Base de datos: $databaseEmail")
                            if (clientId != null) {
                                println("🆔 Cliente ID: $clientId (este dispositivo no recibirá la notificación)")
                            }
                            
                            // Actualizar el correo en la base de datos pasando el clientId
                            val emailActualizado = usuarioController.actualizarCorreoDirecto(databaseEmail, firebaseEmail, clientId)
                            if (emailActualizado) {
                                println("✅ Correo actualizado correctamente durante el login")
                                
                                // Enviar notificación a otros dispositivos - no es necesario hacerlo explícitamente aquí
                                // ya que actualizarCorreoDirecto ya incluye el envío de notificaciones WebSocket
                                
                                // Actualizar el objeto usuario con el nuevo email
                                usuario.email = firebaseEmail
                            } else {
                                println("❌ Error actualizando correo durante el login")
                            }
                        }
                        
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
        
        // Endpoint para Firebase Cloud Functions
        post("/firebaseEmailUpdate") {
            try {
                val request = call.receive<Map<String, String>>()
                val newEmail = request["currentEmail"]
                val oldEmail = request["oldEmail"]
                
                if (newEmail != null && oldEmail != null) {
                    println("📱 Firebase notificó cambio de correo: $oldEmail → $newEmail")
                    
                    // Actualizar directamente el correo electrónico
                    val resultado = usuarioController.actualizarCorreoDirecto(oldEmail, newEmail)
                    
                    if (resultado) {
                        call.respond(HttpStatusCode.OK, "Correo actualizado correctamente por Firebase")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "No se pudo actualizar el correo")
                    }
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Datos incompletos")
                }
            } catch (e: Exception) {
                println("Error en endpoint firebaseEmailUpdate: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, "Error: ${e.message}")
            }
        }

        // Endpoint para actualización directa del correo electrónico
        post("/directUpdateEmail") {
            try {
                val request = call.receive<Map<String, String>>()
                val oldEmail = request["oldEmail"]
                val newEmail = request["newEmail"]
                val clientId = request["clientId"] // Capturar el clientId enviado desde el frontend
                
                if (oldEmail != null && newEmail != null) {
                    println("📱 Actualizando directamente el correo: $oldEmail → $newEmail")
                    if (clientId != null) {
                        println("🆔 Cliente ID: $clientId (este dispositivo no recibirá la notificación)")
                    }
                    
                    val resultado = usuarioController.actualizarCorreoDirecto(oldEmail, newEmail, clientId)
                    
                    if (resultado) {
                        call.respond(HttpStatusCode.OK, "Correo actualizado correctamente en la base de datos")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "No se pudo actualizar el correo")
                    }
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Datos incompletos")
                }
            } catch (e: Exception) {
                println("Error actualizando correo directamente: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, "Error: ${e.message}")
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
    }
}