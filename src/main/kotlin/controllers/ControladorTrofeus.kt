package org.example.controllers

import org.example.models.Trofeu
import org.example.repositories.TrofeuRepository
import org.example.repositories.TrofeuUsuariInfo

class ControladorTrofeus(private val trofeuRepository: TrofeuRepository) {
    fun obtenirTrofeusPerUsuari(usuari: String): List<TrofeuUsuariInfo> {
        return trofeuRepository.obtenirTrofeusPerUsuari(usuari)
    }
}