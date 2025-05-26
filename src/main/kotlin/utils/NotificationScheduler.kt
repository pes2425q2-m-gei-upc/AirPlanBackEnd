package org.example.utils

import kotlinx.coroutines.*
import kotlinx.datetime.*
import org.example.database.ActivitatTable
import org.example.database.ParticipantsActivitatsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.example.repositories.NotificationRepository
import org.example.websocket.WebSocketManager
import org.example.repositories.NotaRepository

val notifiedActivities = mutableSetOf<Int>()
val notifiedNotes = mutableSetOf<Int>()
val notificationRepository = NotificationRepository()
val notaRepository = NotaRepository()
val webSocketManager = WebSocketManager.instance

object NotificationScheduler {
    @OptIn(DelicateCoroutinesApi::class)
    fun start() {
        GlobalScope.launch {
            while (true) {
                checkAndNotifyUpcomingActivities()
                checkAndNotifyUpcomingNotes()
                delay(60 * 1000L) // cada minuto
            }
        }
    }

    suspend fun checkAndNotifyUpcomingActivities() {
        withContext(Dispatchers.IO) {
            val nowInstant = Clock.System.now()
            val now = nowInstant.toLocalDateTime(TimeZone.currentSystemDefault())
            val in30MinutesInstant = nowInstant.plus(30, DateTimeUnit.MINUTE)
            val in30Minutes = in30MinutesInstant.toLocalDateTime(TimeZone.currentSystemDefault())

            println("Verificando actividades entre $now y $in30Minutes")

            val activitiesToNotify = transaction {
                val query = ActivitatTable.select {
                    ActivitatTable.dataInici.between(now, in30Minutes)
                }.toList()

                println("Actividades encontradas en el rango: ${query.size}")
                query.forEach { activity ->
                    println("  - ID: ${activity[ActivitatTable.id_activitat]}, Nombre: ${activity[ActivitatTable.nom]}, Inicio: ${activity[ActivitatTable.dataInici]}")
                }

                query
                    .filter { activity ->
                        !notifiedActivities.contains(activity[ActivitatTable.id_activitat])
                    }
                    .map { activity ->
                        val activityId = activity[ActivitatTable.id_activitat]
                        val activityName = activity[ActivitatTable.nom]
                        val activityStartTime = activity[ActivitatTable.dataInici]

                        val participants = ParticipantsActivitatsTable.select {
                            ParticipantsActivitatsTable.id_activitat eq activityId
                        }.map { it[ParticipantsActivitatsTable.username_participant] }

                        println("Seleccionada para notificación: ID $activityId, Nombre $activityName, Inicio $activityStartTime, Participantes: ${participants.size}")

                        Triple(activityId, activityName, activityStartTime to participants)
                    }
            }

            println("Actividades a notificar: ${activitiesToNotify.size}")

            activitiesToNotify.forEach { (activityId, activityName, activityStartTimeWithParticipants) ->
                val (activityStartTime, participants) = activityStartTimeWithParticipants
                val minutesRemaining = java.time.Duration.between(
                    now.toJavaLocalDateTime(),
                    activityStartTime.toJavaLocalDateTime()
                ).toMinutes()


                val message = "$activityName,$minutesRemaining"
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

    private suspend fun checkAndNotifyUpcomingNotes() {
        withContext(Dispatchers.IO) {
            println("Verificando notas próximas...")

            val notesToNotify = notaRepository.obtenirNotesProperes(30)
                .filter { nota -> !notifiedNotes.contains(nota.id) }

            println("Notas a notificar: ${notesToNotify.size}")

            notesToNotify.forEach { nota ->
                // Calcular minutos restantes
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val reminderDateTime = nota.fechaCreacion.atTime(nota.horaRecordatorio)
                val nowDateTime = now.date.atTime(now.time)

                val minutesRemaining = if (reminderDateTime > nowDateTime) {
                    val duration = reminderDateTime.toInstant(TimeZone.currentSystemDefault()) -
                            nowDateTime.toInstant(TimeZone.currentSystemDefault())
                    duration.inWholeMinutes
                } else 0

                val message = if (minutesRemaining > 0) {
                    "Recordatorio en $minutesRemaining minutos: ${nota.comentario}"
                } else {
                    "Recordatorio: ${nota.comentario}"
                }

                webSocketManager.notifyRealTimeEvent(
                    username = nota.username,
                    message = message,
                    type = "NOTE_REMINDER"
                )
                nota.id?.let { notifiedNotes.add(it) }
            }
        }
    }
}