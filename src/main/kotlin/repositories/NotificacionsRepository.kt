package org.example.repositories

import org.example.database.NotificacionsTable
import org.example.models.Notificacions
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.datetime.toKotlinLocalDateTime
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class NotificationRepository {
    private fun ResultRow.toNotificacio(): Notificacions {
        return Notificacions(
            id = this[NotificacionsTable.id_notificacion], // Se asume que la columna tiene el nombre id_notificacion
            username = this[NotificacionsTable.username],
            type = this[NotificacionsTable.type],
            message = this[NotificacionsTable.message],
            isRead = this[NotificacionsTable.isRead],
            createdAt = this[NotificacionsTable.created_at].toKotlinLocalDateTime() // Convertir el datetime a LocalDateTime
        )
    }

    // Agregar una nueva notificación a la base de datos
    fun addNotification(username: String, type: String, message: String): Notificacions {
        return transaction {
            val insertStatement = NotificacionsTable.insert {
                it[this.username] = username
                it[this.type] = type
                it[this.message] = message
                it[this.isRead] = false
                // No necesitas setear created_at porque tiene defaultExpression(CurrentTimestamp())
            }

            val insertedId = insertStatement[NotificacionsTable.id_notificacion]

            // Recuperamos el registro insertado completo (incluido created_at)
            val row = NotificacionsTable
                .select { NotificacionsTable.id_notificacion eq insertedId }
                .single()

            Notificacions(
                id = row[NotificacionsTable.id_notificacion],
                username = row[NotificacionsTable.username],
                type = row[NotificacionsTable.type],
                message = row[NotificacionsTable.message],
                isRead = row[NotificacionsTable.isRead],
                createdAt = row[NotificacionsTable.created_at].toKotlinLocalDateTime()
            )
        }
    }

    // Obtener todas las notificaciones de un usuario
    fun getNotificationsForUser(username: String): List<Notificacions> {
        return transaction {
            NotificacionsTable
                .select { NotificacionsTable.username eq username }
                .map { it.toNotificacio()}
        }
    }

    // Eliminar una notificación por su ID
    fun deleteNotification(notificationId: Int): Boolean {
        return transaction {
            val rowCount = NotificacionsTable
                .deleteWhere { id_notificacion eq notificationId }
            rowCount > 0  // Devuelve true si al menos una fila fue eliminada
        }
    }

    // Eliminar las notificaciones de un usuario
    fun deleteNotificationsUser(usernameNoti: String): Boolean {
        return transaction {
            val rowCount = NotificacionsTable
                .deleteWhere { username eq usernameNoti }
            rowCount > 0  // Devuelve true si al menos una fila fue eliminada
        }
    }
}
