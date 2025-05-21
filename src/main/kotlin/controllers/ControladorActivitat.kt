package org.example.controllers

import kotlinx.datetime.LocalDateTime
import org.example.database.ActivitatFavoritaTable
import org.example.models.Activitat
import org.example.models.ParticipantsActivitats
import org.example.repositories.ParticipantsActivitatsRepository
import org.example.models.Localitzacio
import repositories.ActivitatRepository
import repositories.ActivitatFavoritaRepository
import kotlin.reflect.jvm.internal.impl.descriptors.Visibilities.Local

import kotlinx.datetime.TimeZone
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDate
import kotlinx.datetime.toLocalDateTime
import org.example.services.PerspectiveService
import kotlinx.coroutines.runBlocking
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class ControladorActivitat(
    private val ActivitatRepository: ActivitatRepository,
    private val ParticipantsActivitatsRepository: ParticipantsActivitatsRepository,
    private val ActivitatFavoritaRepository: ActivitatFavoritaRepository,
    private val perspectiveService: PerspectiveService = PerspectiveService()
) {
    // Secondary constructor for test compatibility without passing PerspectiveService
    constructor(
        ActivitatRepository: ActivitatRepository,
        ParticipantsActivitatsRepository: ParticipantsActivitatsRepository,
        ActivitatFavoritaRepository: ActivitatFavoritaRepository
    ) : this(
        ActivitatRepository,
        ParticipantsActivitatsRepository,
        ActivitatFavoritaRepository,
        PerspectiveService()
    )

    private val activitats = mutableListOf<Activitat>()

    fun afegirActivitat(nom: String, descripcio: String, ubicacio: Localitzacio, dataInici: LocalDateTime, dataFi: LocalDateTime, creador: String) {
        // Batch validate title and description via perspective service
        val results = runBlocking { perspectiveService.analyzeMessages(listOf(nom, descripcio)) }
        if (results.any { it }) throw IllegalArgumentException("Títol o descripció bloquejats per ser inapropiats")
        val novaActivitat = Activitat(
            id = 0,
            nom = nom,
            descripcio = descripcio,
            ubicacio = ubicacio,
            dataInici = dataInici,
            dataFi = dataFi,
            creador = creador
        )

        val activitatId = ActivitatRepository.afegirActivitat(novaActivitat)

        if (activitatId == null) {
            throw IllegalStateException("Error al afegir l'activitat a la base de dades.")
        }

        print("Activitat afegida amb ID: $activitatId") // Depuració

        novaActivitat.id = activitatId
        activitats.add(novaActivitat)

        // Añadir el creador como primer participante
        val participant = ParticipantsActivitats(
            id_act = activitatId,
            us_participant = creador
        )
        val afegit = ParticipantsActivitatsRepository.afegirParticipant(participant)

        println(afegit)

        if (afegit) {
            println("El creador ${creador} ha sido añadido como participante.")
        } else {
            println("Error al añadir el creador como participante.")
        }
    }

    fun modificarActivitat(id: Int, nom: String, descripcio: String, ubicacio: Localitzacio, dataInici: LocalDateTime, dataFi: LocalDateTime): Boolean {
        // Batch validate content before modification
        val results = runBlocking { perspectiveService.analyzeMessages(listOf(nom, descripcio)) }
        if (results.any { it }) throw IllegalArgumentException("Títol o descripció bloquejats per ser inapropiats")
        return ActivitatRepository.modificarActivitat(id, nom, descripcio, ubicacio, dataInici, dataFi)
    }

    fun eliminarActivitat(id: Int): Boolean {
        activitats.find{ it.id == id }?.eliminarActivitat()
        return activitats.removeIf { it.id == id } //solo si no hay problemas con la base de datos
    }

    fun obtenirActivitatPerId(id: Int): Activitat {
        return ActivitatRepository.getActivitatPerId(id)
    }

    fun obtenirTotesActivitats():  List<Activitat> {
        val acti = ActivitatRepository.obtenirActivitats()
        for (activitat in acti) {
            println("${activitat.nom} - ${activitat.descripcio} - ${activitat.ubicacio.latitud} -${activitat.ubicacio.longitud}  - ${activitat.dataInici} - ${activitat.dataFi} - ${activitat.creador}")
        }
        return acti
    }

    fun eliminarActividad(id: Int): Boolean {
        return ActivitatRepository.eliminarActividad(id)
    }

    fun afegirActivitatFavorita(idActivitat: Int, username: String, dataAfegida: LocalDateTime): Boolean {
        return ActivitatFavoritaRepository.afegirActivitatFavorita(idActivitat, username, dataAfegida)

    }

    fun eliminarActivitatFavorita(idActivitat: Int, username: String): Boolean {
        return ActivitatFavoritaRepository.eliminarActivitatFavorita(idActivitat, username)
    }

    fun comprovarActivitatFavorita(idActivitat: Int, username: String): Boolean {
        return ActivitatFavoritaRepository.comprovarActivitatFavorita(idActivitat, username)
    }

    fun obtenirActivitatsFavoritesPerUsuari(username: String): List<Activitat> {
        return ActivitatFavoritaRepository.obtenirActivitatsFavoritesPerUsuari(username)
    }

    fun eliminarParticipant(idActivitat: Int, username: String): Boolean {
        return ParticipantsActivitatsRepository.eliminarParticipant(idActivitat, username)
    }

    fun eliminarParticipantsPerActivitat(idActivitat: Int): Boolean {
        return ParticipantsActivitatsRepository.eliminarParticipantsPerActivitat(idActivitat)
    }

    fun esCreador(idActivitat: Int, username: String): Boolean {
        return ParticipantsActivitatsRepository.esCreador(idActivitat, username)
    }

    fun obtenirParticipantsDeActivitat(idActivitat: Int): List<String> {
        return ParticipantsActivitatsRepository.obtenirParticipantsPerActivitat(idActivitat)
    }

    /**
     * Obtiene actividades excluyendo aquellas creadas por usuarios en la lista de bloqueados
     */
    fun obtenirActivitatsExcluintUsuaris(usuarisBloqueados: List<String>): List<Activitat> {
        return ActivitatRepository.obtenirActivitatsExcluintUsuaris(usuarisBloqueados)
    }

    /**
     * Obtiene actividades filtrando en una única consulta SQL las que pertenecen a usuarios bloqueados
     */
    fun obtenirActivitatsPerUsuariSenseBloquejos(username: String): List<Activitat> {
        return ActivitatRepository.obtenirActivitatsPerUsuariSenseBloquejos(username)
    }

    fun obtenirActivitatsStartingToday(): List<Activitat> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return ActivitatRepository.obtenirActivitatsStartingToday(today)
    }

    fun obtenirActivitatsPerParticipant(username: String): List<Activitat> {
        return ParticipantsActivitatsRepository.obtenirActivitatsPerParticipant(username)
    }

    fun obtenirActivitatsRecomanades(localitzacio: Localitzacio): List<Activitat> {
        val activitats = ActivitatRepository.obtenirActivitats()
        if (activitats.isEmpty()) {
            throw IllegalStateException("No hi han activitats al sistema.")
        }

        // Helper to calculate distance in km using Haversine formula
        fun distanceKm(loc1: Localitzacio, loc2: Localitzacio): Double {
            val R = 6371.0 // Earth radius in km
            val dLat = Math.toRadians((loc2.latitud - loc1.latitud).toDouble())
            val dLon = Math.toRadians((loc2.longitud - loc1.longitud).toDouble())
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(loc1.latitud.toDouble())) * cos(Math.toRadians(loc2.latitud.toDouble())) *
                    sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return R * c
        }

        // Find the closest activity
        val closest = activitats.minByOrNull { distanceKm(localitzacio, it.ubicacio) }
            ?: throw IllegalStateException("No hi han activitats al sistema.")

        val minDistance = distanceKm(localitzacio, closest.ubicacio)
        if (minDistance > 50.0) {
            throw NoSuchElementException("Les activitats més properes es troben a més de 50 km.")
        }

        val searchRadius = 5.0 + minDistance
        return activitats.filter { distanceKm(localitzacio, it.ubicacio) <= searchRadius }
    }
}
