package com.duoc.aquaflow.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.ui.navigation.Tab
import com.duoc.aquaflow.ui.navigation.tabsPara
import com.duoc.aquaflow.ui.screens.DashboardScreen
import com.duoc.aquaflow.ui.screens.EncuestaSaludScreen
import com.duoc.aquaflow.ui.screens.LoginScreen
import com.duoc.aquaflow.ui.screens.historial.HistorialScreen
import com.duoc.aquaflow.viewmodel.SesionViewModel
import com.duoc.aquaflow.ui.screens.AstScreen
import com.duoc.aquaflow.viewmodel.AstViewModel

object Ruta {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val HISTORIAL = "historial"
    const val SALUD = "faena/salud"

    const val AST = "faena/ast"
}

@Composable
fun AquaFlowNavHost() {
    val nav = rememberNavController()
    val sesion: SesionViewModel = viewModel()
    val usuario by sesion.usuario.collectAsState()

    // Si Android restaura las rutas sin una sesión, volvemos al ingreso.
    LaunchedEffect(usuario) {
        if (usuario == null) {
            nav.navigate(Ruta.LOGIN) {
                popUpTo(nav.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    fun irATab(tab: Tab) {
        nav.navigate(tab.ruta) {
            popUpTo(Ruta.DASHBOARD) { inclusive = false }
            launchSingleTop = true
        }
    }

    NavHost(navController = nav, startDestination = Ruta.LOGIN) {
        composable(Ruta.LOGIN) {
            LoginScreen(
                sesion = sesion,
                onIngresoExitoso = {
                    nav.navigate(Ruta.DASHBOARD) {
                        popUpTo(Ruta.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Ruta.DASHBOARD) {
            usuario?.let { actual ->
                DashboardScreen(
                    usuario = u,
                    tabs = tabsPara(u.rol),
                    ultimaEncuesta = ultimaEncuesta,
                    onNuevaEncuesta = { nav.navigate(Ruta.SALUD) },
                    onTab = ::irATab,
                    onSalir = {
                        sesion.salir()
                        ultimaEncuesta = null
                        nav.navigate(Ruta.LOGIN) {
                            popUpTo(nav.graph.id) { inclusive = true }  // borra todo el historial
                        }
                    },
                    onSalir = sesion::salir
                )
            }
        }
        navigation(startDestination = Ruta.APERTURA, route = Ruta.FAENA) {
            composable(Ruta.APERTURA) { entrada ->
                val padre = remember(entrada) { nav.getBackStackEntry(Ruta.FAENA) }
                val faena: FaenaViewModel = viewModel(viewModelStoreOwner = padre)
                val ui by faena.ui.collectAsState()
                val actual = usuario

        composable(Ruta.HISTORIAL) {
            usuario?.let { u ->
                HistorialScreen(usuario = u, tabs = tabsPara(u.rol), onTab = ::irATab)
            }
        }

        composable(Ruta.SALUD) {
            EncuestaSaludScreen(
                vm = viewModel(),   // uno nuevo por visita
                buzo = "Buzo por asignar",
                tabs = usuario?.let { tabsPara(it.rol) } ?: Tab.entries.toList(),
                onGuardar = {
                    ultimaEncuesta = it
                    irATab(Tab.INICIO)
                },
                onVolver = { irATab(Tab.INICIO) },
                onTab = ::irATab
            )
        }
    }
}
