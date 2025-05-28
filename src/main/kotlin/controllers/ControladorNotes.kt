package org.example.controllers

import org.example.models.Nota
import org.example.repositories.NotaRepository

class ControladorNotes(private val notaRepository: NotaRepository) {

    fun afegirNota(nota: Nota): Boolean {
        return notaRepository.afegirNota(nota)
    }

    fun eliminarNota(id: Int): Boolean {
        return notaRepository.eliminarNota(id)
    }

    fun obtenirNotesPerUsuari(username: String): List<Nota> {
        return notaRepository.obtenirNotesPerUsuari(username)
    }

    fun editarNota(id: Int, novaNota: Nota): Boolean {
        return notaRepository.editarNota(id, novaNota)
    }

    fun obtenirNotaPerId(id: Int): Nota? {
        return notaRepository.obtenirNotaPerId(id)
    }
}