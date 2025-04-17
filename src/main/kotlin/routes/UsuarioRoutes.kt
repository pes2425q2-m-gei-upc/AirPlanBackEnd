package org.example.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import org.example.controllers.ControladorUsuarios
import org.example.models.Usuario
import org.example.repositories.UsuarioRepository
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.transactions.transaction
import org.example.enums.Idioma
import org.jetbrains.exposed.sql.update
import org.example.database.UsuarioTable  // Corregida la importación

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
                    idioma = usuario.idioma,
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
            if (email != null) {
                val resultado = usuarioController.eliminarUsuario(email)
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
            println("pepe")
            // Obtener los parámetros directamente desde la solicitud JSON
            val params = call.receive<Map<String, String>>()  // Recibe los datos como un mapa
            val email = params["email"]
            val contrasena = params["contrasena"]
            // Verificamos si el email y la contraseña son correctos
            val usuario = usuarioController.login(email, contrasena)
            if (usuario != null) {
                // Actualizamos el atributo sesionIniciada
                usuario.sesionIniciada = true
                // Guardamos el usuario actualizado en la base de datos
                transaction {
                usuarioController.actualizarSesion(usuario.email, true)
                }

                var respuesta = mapOf("isAdmin" to usuario.isAdmin)
                // Respondemos que el login fue exitoso
                call.respond(HttpStatusCode.OK, respuesta)
            } else {
                // Respondemos que el login falló
                call.respond(HttpStatusCode.Unauthorized, "Email o contraseña incorrectos")
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
        // Endpoint para guardar un correo pendiente de verificación
        post("/pendingEmail") {
            try {
                val request = call.receive<Map<String, String>>()
                val currentEmail = request["currentEmail"]
                val pendingEmail = request["pendingEmail"]

                if (currentEmail != null && pendingEmail != null) {
                    val resultado = usuarioController.guardarCorreoPendiente(currentEmail, pendingEmail)
                    if (resultado) {
                        call.respond(HttpStatusCode.OK, "Correo pendiente guardado correctamente")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Usuario no encontrado")
                    }
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Datos incompletos")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error: ${e.message}")
            }
        }

        // Endpoint para confirmar un cambio de correo
        post("/confirmEmail") {
            try {
                val request = call.receive<Map<String, String>>()
                val currentEmail = request["currentEmail"]
                val oldEmail = request["oldEmail"]  // Nuevo: recibimos el parámetro oldEmail si está disponible

                if (currentEmail != null) {
                    val resultado = usuarioController.confirmarCambioCorreo(currentEmail, oldEmail)
                    if (resultado) {
                        call.respond(HttpStatusCode.OK, "Correo actualizado correctamente")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "No hay correo pendiente o usuario no encontrado")
                    }
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Datos incompletos")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error: ${e.message}")
            }
        }

        // Endpoint para cancelar un cambio de correo pendiente
        post("/cancelEmail") {
            try {
                val request = call.receive<Map<String, String>>()
                val currentEmail = request["currentEmail"]

                if (currentEmail != null) {
                    val resultado = usuarioController.cancelarCambioCorreo(currentEmail)
                    if (resultado) {
                        call.respond(HttpStatusCode.OK, "Cambio de correo cancelado correctamente")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "No hay correo pendiente o usuario no encontrado")
                    }
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Datos incompletos")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error: ${e.message}")
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
                
                if (newEmail != null) {
                    println("📱 Firebase notificó cambio de correo: ${oldEmail ?: "desconocido"} → $newEmail")
                    
                    // Primero, intentar el enfoque estándar (si el pendingEmail coincide con el nuevo email)
                    var resultado = usuarioController.confirmarCambioCorreo(newEmail)
                    
                    // Si no funcionó y tenemos el email antiguo, intentar buscar directamente por el email antiguo
                    if (!resultado && oldEmail != null) {
                        // Buscar un usuario con el email antiguo y actualizar su email
                        resultado = transaction {
                            val filasActualizadas = UsuarioTable.update({ UsuarioTable.email eq oldEmail }) {
                                it[UsuarioTable.email] = newEmail
                                it[UsuarioTable.pendingEmail] = null
                            }
                            filasActualizadas > 0
                        }
                    }
                    
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

                if (oldEmail != null && newEmail != null) {
                    println("📧 Actualización directa de correo: $oldEmail → $newEmail")
                    
                    // Actualizar el correo directamente en la base de datos
                    val resultado = transaction {
                        val filasActualizadas = UsuarioTable.update({ UsuarioTable.email eq oldEmail }) {
                            it[UsuarioTable.email] = newEmail
                            it[UsuarioTable.pendingEmail] = null  // Limpiar cualquier pendiente
                        }
                        filasActualizadas > 0
                    }
                    
                    if (resultado) {
                        call.respond(HttpStatusCode.OK, "Correo actualizado correctamente")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Usuario no encontrado")
                    }
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Datos incompletos")
                }
            } catch (e: Exception) {
                println("Error en actualización directa de correo: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, "Error: ${e.message}")
            }
        }
    }
}