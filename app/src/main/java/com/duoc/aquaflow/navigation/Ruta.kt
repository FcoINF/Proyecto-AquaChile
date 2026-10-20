package com.duoc.aquaflow.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.ui.screens.DashboardScreen
import com.duoc.aquaflow.ui.screens.EncuestaSaludScreen
import com.duoc.aquaflow.ui.screens.LoginScreen
import com.duoc.aquaflow.viewmodel.SesionViewModel

object Ruta {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val SALUD = "faena/salud"
}

@Composable
fun AquaFlowNavHost() {
    val nav = rememberNavController()
    val sesion: SesionViewModel = viewModel()
    val usuario by sesion.usuario.collectAsState()

    // Provisorio: cuando exista el FaenaViewModel, esto se mueve allá
    var ultimaEncuesta by remember { mutableStateOf<EncuestaSalud?>(null) }

    NavHost(navController = nav, startDestination = Ruta.LOGIN) {

        composable(Ruta.LOGIN) {
            LoginScreen(
                sesion = sesion,
                onIngresoExitoso = {
                    nav.navigate(Ruta.DASHBOARD) {
                        popUpTo(Ruta.LOGIN) { inclusive = true }  // "atrás" no vuelve al login
                    }
                }
            )
        }

        composable(Ruta.DASHBOARD) {
            usuario?.let { u ->
                DashboardScreen(
                    usuario = u,
                    ultimaEncuesta = ultimaEncuesta,
                    onNuevaEncuesta = { nav.navigate(Ruta.SALUD) },
                    onSalir = {
                        sesion.salir()
                        ultimaEncuesta = null
                        nav.navigate(Ruta.LOGIN) {
                            popUpTo(nav.graph.id) { inclusive = true }  // borra todo el historial
                        }
                    }
                )
            }
        }

        composable(Ruta.SALUD) {
            EncuestaSaludScreen(
                vm = viewModel(),   // uno nuevo por visita
                buzo = "Buzo por asignar",
                onGuardar = {
                    ultimaEncuesta = it
                    nav.popBackStack()
                },
                onVolver = { nav.popBackStack() }
            )
        }
    }
}