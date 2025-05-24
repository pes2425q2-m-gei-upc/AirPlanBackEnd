package org.example.utils

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import java.io.FileInputStream

class FCMManager {
    init {
        val serviceAccount = this.javaClass.classLoader
            .getResourceAsStream("firebase-service-account.json")
            ?: throw IllegalStateException("No se pudo encontrar el archivo de configuración de Firebase")

        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build()

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
        }
    }

    fun sendNotification(token: String, title: String, body: String) {
        val message = Message.builder()
            .setToken(token)
            .setNotification(Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build())
            .build()

        FirebaseMessaging.getInstance().send(message)
    }
}

