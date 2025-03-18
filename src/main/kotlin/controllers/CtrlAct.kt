package org.example.controllers

import org.example.models.Activitat
import org.example.models.Localitzacio
import java.time.LocalDateTime

class ActivitatController {
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
            creador = creador,
            participants = mutableListOf(creador)
        )
        novaActivitat.afegirActivitat()
        activitats.add(novaActivitat) //solo si no hay problemas con la base de datos
    }

    fun modificarActivitat(id: Int, nom: String, descripcio: String, ubicacio: Localitzacio, dataInici: LocalDateTime, dataFi: LocalDateTime) {
        activitats.find{ it.id == id }?.modificarActivitat(nom, descripcio, ubicacio, dataInici, dataFi)
    }

    fun eliminarActivitat(id: Int): Boolean {
        activitats.find{ it.id == id }?.eliminarActivitat()
        return activitats.removeIf { it.id == id } //solo si no hay problemas con la base de datos
    }
}