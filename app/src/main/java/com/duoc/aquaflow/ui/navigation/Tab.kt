package com.duoc.aquaflow.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.navigation.Ruta

/** Pestañas de la barra inferior. */
enum class Tab(val ruta: String, val etiqueta: String, val icono: ImageVector) {
    INICIO(Ruta.DASHBOARD, "Inicio", Icons.Filled.Home),
    HISTORIAL(Ruta.HISTORIAL, "Historial", Icons.AutoMirrored.Filled.List),
    ENCUESTA(Ruta.SALUD, "Encuesta", Icons.Filled.Favorite)
}

/** El jefe solo consulta: no ve la pestaña de encuesta. */
fun tabsPara(rol: Rol): List<Tab> =
    if (rol == Rol.SUPERVISOR) Tab.entries.toList() else listOf(Tab.INICIO, Tab.HISTORIAL)
