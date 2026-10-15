package com.duoc.aquaflow.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val HOME = "home"
    const val HISTORIAL = "historial"
    const val USUARIOS = "usuarios"
    const val PERFIL = "perfil"
    const val APERTURA = "apertura"

    const val ARG_FAENA = "faenaId"
    const val FAENA = "faena/{$ARG_FAENA}"
    const val DPR24 = "faena/{$ARG_FAENA}/dpr24"
    const val AST = "faena/{$ARG_FAENA}/ast"
    const val SALUD = "faena/{$ARG_FAENA}/salud"
    const val DECISION = "faena/{$ARG_FAENA}/decision"
    const val REGISTRO = "faena/{$ARG_FAENA}/registro"

    fun faena(id: Long) = "faena/$id"
    fun dpr24(id: Long) = "faena/$id/dpr24"
    fun ast(id: Long) = "faena/$id/ast"
    fun salud(id: Long) = "faena/$id/salud"
    fun decision(id: Long) = "faena/$id/decision"
    fun registro(id: Long) = "faena/$id/registro"
}

/** Pestañas de la barra inferior. */
enum class Tab(val ruta: String, val etiqueta: String, val icono: ImageVector) {
    INICIO(Routes.HOME, "Inicio", Icons.Filled.Home),
    HISTORIAL(Routes.HISTORIAL, "Historial", Icons.Filled.History),
    USUARIOS(Routes.USUARIOS, "Usuarios", Icons.Filled.People),
    PERFIL(Routes.PERFIL, "Perfil", Icons.Filled.Person)
}
