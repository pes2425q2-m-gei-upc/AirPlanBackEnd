package org.example.controllers

import kotlinx.datetime.LocalDateTime
import org.example.models.Activitat
import org.example.models.Localitzacio
import java.sql.Timestamp
import repositories.ActivitatRepository
import kotlin.reflect.jvm.internal.impl.descriptors.Visibilities.Local

class ControladorActivitat (private val ActivitatRepository: ActivitatRepository) {
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

    fun modificarActivitat(id: Int, nom: String, descripcio: String, ubicacio: Localitzacio, dataInici: Timestamp, dataFi: Timestamp) {
        activitats.find{ it.id == id }?.modificarActivitat(nom, descripcio, ubicacio, dataInici, dataFi)
    }

    fun eliminarActivitat(id: Int): Boolean {
        activitats.find{ it.id == id }?.eliminarActivitat()
        return activitats.removeIf { it.id == id } //solo si no hay problemas con la base de datos
    }

    fun obtenirActivitatPerId(id: Int): Activitat {
        return activitats.find{ it.id == id }!!
    }

    fun obtenirTotesActivitats():  List<Activitat> {
        var acti = ActivitatRepository.obtenirActivitats()
        for (activitat in acti) {
            println("${activitat.nom} - ${activitat.descripcio} - ${activitat.ubicacio.latitud} -${activitat.ubicacio.longitud}  - ${activitat.dataInici} - ${activitat.dataFi} - ${activitat.creador}")
        }
        return acti
    }
}