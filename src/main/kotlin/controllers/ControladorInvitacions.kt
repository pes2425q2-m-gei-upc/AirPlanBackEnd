package org.example.controllers

import org.example.models.Activitat
import org.example.models.Cliente
import org.example.models.Invitacio

class ControladorInvitacions {
    private val invitacions = mutableListOf<Invitacio>()

    // Crear una nueva invitación
    fun crearInvitacio(activitat: Activitat, anfitrio: Cliente, destinatario: Cliente): Boolean {
        if (anfitrio.username != activitat.creador) {
            println("Solo el creador de la actividad puede enviar invitaciones.")
            return false
        }
        val invitacio = Invitacio(activitat, anfitrio, destinatario)
        invitacions.add(invitacio)
        println("Invitación creada para ${destinatario.username}.")
        return true
    }

    // Aceptar una invitación
    fun acceptarInvitacio(invitacio: Invitacio): Boolean {
        if (invitacions.remove(invitacio)) {
            invitacio.activitat.participants.add(invitacio.destinatario.username)
            println("${invitacio.destinatario.username} ha sido añadido a la actividad.")
            return true
        }
        println("La invitación no existe.")
        return false
    }

    // Rechazar una invitación
    fun rebutjarInvitacio(invitacio: Invitacio): Boolean {
        if (invitacions.remove(invitacio)) {
            println("La invitación para ${invitacio.destinatario.username} ha sido rechazada.")
            return true
        }
        println("La invitación no existe.")
        return false
    }

    // Listar todas las invitaciones
    fun listarInvitacions(): List<Invitacio> {
        return invitacions
    }
}