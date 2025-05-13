package org.example.utils

import kotlinx.coroutines.*
import kotlinx.datetime.toJavaLocalDateTime
import org.example.database.ActivitatTable
import org.example.database.ParticipantsActivitatsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import org.example.repositories.NotificationRepository
import org.example.websocket.WebSocketManager
import java.sql.Timestamp
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

val notifiedActivities = mutableSetOf<Int>()
val notificationRepository = NotificationRepository()
val webSocketManager = WebSocketManager.instance

object NotificationScheduler {
    @OptIn(DelicateCoroutinesApi::class)
    fun start() {
        GlobalScope.launch {
            while (true) {
                // Aquí llamamos a la función suspendida
                checkAndNotifyUpcomingActivities()
                delay(60 * 1000L) // cada minuto
            }
        }
    }

    suspend fun checkAndNotifyUpcomingActivities() {
        withContext(Dispatchers.IO) {
            val now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)

            val activitiesToNotify = transaction {
                val in30Minutes = now.plusMinutes(30)
                ActivitatTable.select {
                    ActivitatTable.dataInici.between(now, in30Minutes)
                }.toList()
                    .filter { activity ->
                        !notifiedActivities.contains(activity[ActivitatTable.id_activitat])
                    }
                    .map { activity ->
                        val activityId = activity[ActivitatTable.id_activitat]
                        val activityName = activity[ActivitatTable.nom]
                        val activityStartTime = activity[ActivitatTable.dataInici].toJavaLocalDateTime()

                        val participants = ParticipantsActivitatsTable.select {
                            ParticipantsActivitatsTable.id_activitat eq activityId
                        }.map { it[ParticipantsActivitatsTable.username_participant] }

                        Triple(activityId, activityName, activityStartTime to participants)
                    }
            }

            activitiesToNotify.forEach { (activityId, activityName, activityStartTimeWithParticipants) ->
                val (activityStartTime, participants) = activityStartTimeWithParticipants
                val minutesRemaining = java.time.Duration.between(now, activityStartTime).toMinutes()

                println("Actividad: '$activityName' a punto de empezar en $minutesRemaining minutos")

                val message = "La actividad '$activityName' empieza en $minutesRemaining minutos."
                participants.forEach { username ->
                    webSocketManager.notifyRealTimeEvent(
                        username = username,
                        message = message,
                        type = "ACTIVITY_REMINDER"
                    )
                }

                notifiedActivities.add(activityId)
            }
        }
    }

}
