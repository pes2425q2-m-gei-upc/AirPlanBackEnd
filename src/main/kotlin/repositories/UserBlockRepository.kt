package org.example.repositories

import org.example.database.UserBlockTable
import org.example.models.UserBlock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import repositories.ActivitatFavoritaRepository
import repositories.SolicitudRepository
import org.example.database.ActivitatTable

open class UserBlockRepository {
    
    private val activitatFavoritaRepository = ActivitatFavoritaRepository()
    private val solicitudRepository = SolicitudRepository()
    
    // Block a user
    open fun blockUser(blockerUsername: String, blockedUsername: String): Boolean {
        return try {
            println("📝 Iniciando transacción para bloquear usuario: $blockerUsername -> $blockedUsername")
            transaction {
                // Don't create duplicate blocks
                val exists = isUserBlocked(blockerUsername, blockedUsername)
                if (!exists) {
                    println("👥 Verificando que ambos usuarios existen...")
                    // Uncomment these lines to debug
                    /*
                    val blockerExists = UsuarioTable.select { UsuarioTable.username eq blockerUsername }.count() > 0
                    val blockedExists = UsuarioTable.select { UsuarioTable.username eq blockedUsername }.count() > 0
                    
                    println("✓ Usuario que bloquea ($blockerUsername) existe: $blockerExists")
                    println("✓ Usuario bloqueado ($blockedUsername) existe: $blockedExists")
                    
                    if (!blockerExists || !blockedExists) {
                        println("⚠️ Uno o ambos usuarios no existen en la base de datos")
                        return@transaction false
                    }
                    */
                    
                    // Verificar si el usuario a bloquear ya me tiene bloqueado
                    val alreadyBlocked = isUserBlocked(blockedUsername, blockerUsername)
                    
                    println("📋 Insertando registro de bloqueo...")
                    UserBlockTable.insert {
                        it[UserBlockTable.blockerUsername] = blockerUsername
                        it[UserBlockTable.blockedUsername] = blockedUsername
                    }
                    println("✅ Bloqueo insertado correctamente")
                    
                    // Si no me ha bloqueado previamente, eliminar actividades favoritas y solicitudes entre ambos
                    if (!alreadyBlocked) {
                        println("🗑️ Eliminando actividades favoritas entre usuarios...")
                        removeFavoriteActivitiesBetweenUsers(blockerUsername, blockedUsername)
                        
                        println("🗑️ Eliminando solicitudes/invitaciones entre usuarios...")
                        solicitudRepository.eliminarTodasSolicitudesEntreUsuarios(blockerUsername, blockedUsername)
                    }
                    
                    true
                } else {
                    println("ℹ️ El bloqueo ya existía, no se necesita crear uno nuevo")
                    true
                }
            }
        } catch (e: Exception) {
            println("❌ Error en la operación de bloqueo: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    // Función para eliminar actividades favoritas entre dos usuarios
    private fun removeFavoriteActivitiesBetweenUsers(user1: String, user2: String) {
        try {
            // Obtener las actividades creadas por user2
            val user2Activities = transaction {
                ActivitatTable.select {
                    ActivitatTable.username_creador eq user2
                }.map { row ->
                    row[ActivitatTable.id_activitat]
                }
            }
            
            // Eliminar de favoritos las actividades de user2 para user1
            for (activityId in user2Activities) {
                activitatFavoritaRepository.eliminarActivitatFavorita(activityId, user1)
            }
            
            // Obtener las actividades creadas por user1
            val user1Activities = transaction {
                ActivitatTable.select {
                    ActivitatTable.username_creador eq user1
                }.map { row ->
                    row[ActivitatTable.id_activitat]
                }
            }
            
            // Eliminar de favoritos las actividades de user1 para user2
            for (activityId in user1Activities) {
                activitatFavoritaRepository.eliminarActivitatFavorita(activityId, user2)
            }
            
            println("✅ Actividades favoritas eliminadas correctamente entre $user1 y $user2")
        } catch (e: Exception) {
            println("❌ Error al eliminar actividades favoritas: ${e.message}")
            e.printStackTrace()
        }
    }
    
    // Unblock a user
    open fun unblockUser(blockerUsername: String, blockedUsername: String): Boolean {
        return try {
            println("📝 Iniciando transacción para desbloquear usuario: $blockerUsername -> $blockedUsername")
            transaction {
                val deletedRows = UserBlockTable.deleteWhere {
                    (UserBlockTable.blockerUsername eq blockerUsername) and
                    (UserBlockTable.blockedUsername eq blockedUsername)
                }
                if (deletedRows > 0) {
                    println("✅ Desbloqueo realizado correctamente")
                    true
                } else {
                    println("ℹ️ No se encontró un bloqueo existente para eliminar")
                    false
                }
            }
        } catch (e: Exception) {
            println("❌ Error en la operación de desbloqueo: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    // Check if a user is blocked by another user
    open fun isUserBlocked(blockerUsername: String, blockedUsername: String): Boolean {
        return transaction {
            UserBlockTable.select {
                (UserBlockTable.blockerUsername eq blockerUsername) and
                (UserBlockTable.blockedUsername eq blockedUsername)
            }.count() > 0
        }
    }
    
    // Check if either user has blocked the other (for chat verification)
    fun isEitherUserBlocked(user1: String, user2: String): Boolean {
        return transaction {
            UserBlockTable.select {
                ((UserBlockTable.blockerUsername eq user1) and (UserBlockTable.blockedUsername eq user2)) or
                ((UserBlockTable.blockerUsername eq user2) and (UserBlockTable.blockedUsername eq user1))
            }.count() > 0
        }
    }
    
    // Get all users blocked by a specific user
    open fun getBlockedUsers(blockerUsername: String): List<UserBlock> {
        return transaction {
            UserBlockTable
                .select { UserBlockTable.blockerUsername eq blockerUsername }
                .map { row ->
                    UserBlock(
                        blockerUsername = row[UserBlockTable.blockerUsername],
                        blockedUsername = row[UserBlockTable.blockedUsername]
                    )
                }
        }
    }
    
    // Get who has blocked this user
    fun getBlockersOfUser(blockedUsername: String): List<UserBlock> {
        return transaction {
            UserBlockTable
                .select { UserBlockTable.blockedUsername eq blockedUsername }
                .map { row ->
                    UserBlock(
                        blockerUsername = row[UserBlockTable.blockerUsername],
                        blockedUsername = row[UserBlockTable.blockedUsername]
                    )
                }
        }
    }
}