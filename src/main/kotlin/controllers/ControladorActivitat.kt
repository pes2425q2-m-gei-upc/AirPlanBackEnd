package org.example.controllers

import kotlinx.datetime.LocalDateTime
import org.example.database.ActivitatFavoritaTable
import org.example.models.Activitat
import org.example.models.Localitzacio
import java.sql.Timestamp
import repositories.ActivitatRepository
import repositories.ActivitatFavoritaRepository
import kotlin.reflect.jvm.internal.impl.descriptors.Visibilities.Local

class ControladorActivitat(
    private val ActivitatRepository: ActivitatRepository,
    private val ActivitatFavoritaRepository: ActivitatFavoritaRepository
) {
    private val activitats = mutableListOf<Activitat>()

    fun obtenirActivitats(): List<Activitat> {
        return activitats
    }

    fun afegirActivitat(nom: String, descripcio: String, ubicacio: Localitzacio, dataInici: LocalDateTime, dataFi: LocalDateTime, creador: String) {
        val novaActivitat = Activitat(
            id = 0,
            nom = nom,
            descripcio = descripcio,
            ubicacio = ubicacio,
            dataInici = dataInici,
            dataFi = dataFi,
            creador = creador
        )
        ActivitatRepository.afegirActivitat(novaActivitat)
        activitats.add(novaActivitat) //solo si no hay problemas con la base de datos
        //TODO: canviar id despres de afegir a la base de dades
    }

    fun modificarActivitat(id: Int, nom: String, descripcio: String, ubicacio: Localitzacio, dataInici: LocalDateTime, dataFi: LocalDateTime): Boolean {
        return ActivitatRepository.modificarActivitat(id,nom, descripcio, ubicacio, dataInici, dataFi)
    }

    fun eliminarActivitat(id: Int): Boolean {
        activitats.find{ it.id == id }?.eliminarActivitat()
        return activitats.removeIf { it.id == id } //solo si no hay problemas con la base de datos
    }

    fun obtenirActivitatPerId(id: Int): Activitat {
        return ActivitatRepository.getActivitatPerId(id)
    }

    fun obtenirTotesActivitats():  List<Activitat> {
        var acti = ActivitatRepository.obtenirActivitats()
        for (activitat in acti) {
            println("${activitat.nom} - ${activitat.descripcio} - ${activitat.ubicacio.latitud} -${activitat.ubicacio.longitud}  - ${activitat.dataInici} - ${activitat.dataFi} - ${activitat.creador}")
        }
        return acti
    }

    fun eliminarActividad(id: Int): Boolean {
        return ActivitatRepository.eliminarActividad(id)
    }

    fun afegirActivitatFavorita(idActivitat: Int, username: String, dataAfegida: LocalDateTime): Boolean {
        val activitat = activitats.find { it.id == idActivitat }
        return if (activitat != null) {
            ActivitatFavoritaRepository.afegirActivitatFavorita(idActivitat, username, dataAfegida)
            true
        } else {
            false // Activity not found
        }
    }

    fun eliminarActivitatFavorita(idActivitat: Int, username: String): Boolean {
        return ActivitatFavoritaRepository.eliminarActivitatFavorita(idActivitat, username)
    }
}
