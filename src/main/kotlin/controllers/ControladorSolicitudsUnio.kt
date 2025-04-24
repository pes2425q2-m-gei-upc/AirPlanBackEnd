package org.example.controllers

import org.example.models.Activitat
import repositories.SolicitudRepository

class ControladorSolicitudsUnio(private val solicitudRepository: SolicitudRepository) {

    fun enviarSolicitud(usernameAnfitrio: String, usernameSolicitant: String, idActivitat: Int): Boolean {
        return solicitudRepository.enviarSolicitud(usernameAnfitrio, usernameSolicitant, idActivitat)
    }

    fun eliminarSolicitud(usernameSolicitant: String, idActivitat: Int): Boolean {
        return solicitudRepository.eliminarSolicitud(usernameSolicitant, idActivitat)
    }

    fun obtenirSolicitudesPerUsuari(usernameSolicitant: String): List<Activitat> {
        return solicitudRepository.obtenirSolicitudesPerUsuari(usernameSolicitant)
    }

    fun activitatJaSolicitada(usernameAnfitrio: String, usernameSolicitant: String, idActivitat: Int): Boolean {
        return solicitudRepository.existeixSolicitud(usernameAnfitrio, usernameSolicitant, idActivitat)
    }
}