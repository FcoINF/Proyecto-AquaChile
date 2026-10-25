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

/** La encuesta forma parte de una faena; no se inicia aislada desde una pestaña. */
fun tabsPara(rol: Rol): List<Tab> =
    listOf(Tab.INICIO, Tab.HISTORIAL)
