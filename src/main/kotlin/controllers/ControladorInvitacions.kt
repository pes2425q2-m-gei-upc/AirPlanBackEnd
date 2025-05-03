package org.example.controllers

import org.example.models.Invitacio
import org.example.models.Activitat
import org.example.models.ParticipantsActivitats
import org.example.repositories.InvitacioRepository
import org.example.repositories.ParticipantsActivitatsRepository
import org.example.repositories.UsuarioRepository

class ControladorInvitacions(
    private val participantsActivitatsRepository: ParticipantsActivitatsRepository,
    private val invitacionsRepository: InvitacioRepository,
    private val usuarioRepository: UsuarioRepository
) {
    // Crear una nueva invitación
    fun crearInvitacio(idAct: Int, usAnfitrio: String, usDestinatari: String): Boolean {
        if (!usuarioRepository.existeUsuario(usDestinatari)) {
            println("El usuario destinatario no existe.")
            return false
        }

        // Verify that the host is the creator of the activity
        /*if (!participantsActivitatsRepository.esCreador(idAct, usAnfitrio)) {
            println("Solo el creador de la actividad puede enviar invitaciones.")
            return false
        }*/

        val invitacio = Invitacio(id_act = idAct, us_anfitrio = usAnfitrio, us_destinatari = usDestinatari)
        invitacionsRepository.afegirInvitacio(invitacio)
        println("Invitación creada para $usDestinatari con anfitrion $usAnfitrio.")
        return true
    }

    // Aceptar una invitación
    fun acceptarInvitacio(invitacio: Invitacio): Boolean {
        if (invitacionsRepository.eliminarInvitacio(invitacio)) {
            val participant = ParticipantsActivitats(id_act = invitacio.id_act, us_participant = invitacio.us_destinatari)
            participantsActivitatsRepository.afegirParticipant(participant)
            println("${invitacio.us_destinatari} ha sido añadido a la actividad.")
            return true
        }
        println("La invitación no existe.")
        return false
    }

    // Rechazar una invitación
    fun rebutjarInvitacio(invitacio: Invitacio): Boolean {
        if (invitacionsRepository.eliminarInvitacio(invitacio)) {
            println("La invitación para ${invitacio.us_destinatari} ha sido rechazada.")
            return true
        }
        println("La invitación no existe.")
        return false
    }

    // Listar todas las invitaciones
    fun listarInvitacions(): List<Invitacio> {
        return invitacionsRepository.obtenirTotesInvitacions()
    }

    fun obtenirActivitatsAmbInvitacionsPerUsuari(username: String): List<Activitat> {
        return invitacionsRepository.obtenirActivitatsAmbInvitacionsPerUsuari(username)
    }
}