package org.example.controllers

import org.example.models.Notificacions
import org.example.repositories.NotificationRepository

class ControladorNotificacions(private val notificationRepository: NotificationRepository) {

    // Obtener todas las notificaciones de un usuario
    fun getNotifications(username: String): List<Notificacions> {
        return notificationRepository.getNotificationsForUser(username)
    }


    // Agregar una nueva notificación
    fun addNotification(username: String, type: String, message: String): Notificacions {
        return notificationRepository.addNotification(username, type, message)
    }

    // Eliminar una notificación
    fun deleteNotification(notificationId: Int): Boolean {
        return notificationRepository.deleteNotification(notificationId)
    }

    fun deleteNotificationsUser(usernameNoti: String): Boolean {
        return notificationRepository.deleteNotificationsUser(usernameNoti)
    }
}
