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
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.ui.screens.AperturaFaenaScreen
import com.duoc.aquaflow.ui.screens.DashboardScreen
import com.duoc.aquaflow.ui.screens.EncuestaSaludScreen
import com.duoc.aquaflow.ui.screens.LoginScreen
import com.duoc.aquaflow.viewmodel.EncuestaSaludViewModel
import com.duoc.aquaflow.viewmodel.FaenaViewModel
import com.duoc.aquaflow.viewmodel.SesionViewModel
import com.duoc.aquaflow.ui.screens.AstScreen
import com.duoc.aquaflow.viewmodel.AstViewModel

object Ruta {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val FAENA = "faena"
    const val APERTURA = "faena/apertura"
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
                    usuario = actual,
                    onNuevaFaena = {
                        if (actual.rol == Rol.SUPERVISOR) {
                            nav.navigate(Ruta.FAENA) { launchSingleTop = true }
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

                LaunchedEffect(actual) {
                    if (actual != null && actual.rol == Rol.SUPERVISOR) {
                        faena.iniciar(actual)
                    } else if (actual != null) {
                        nav.popBackStack(Ruta.DASHBOARD, false)
                    }
                }
                if (actual?.rol == Rol.SUPERVISOR && ui.apertura.id.isNotEmpty()) {
                    AperturaFaenaScreen(
                        ui = ui,
                        onCambiar = faena::actualizar,
                        onContinuar = {
                            if (faena.confirmarApertura()) {
                                nav.navigate(Ruta.AST) { launchSingleTop = true }
                            }
                        },
                        onSalir = { nav.popBackStack(Ruta.DASHBOARD, false) }
                    )
                }
            }
            composable(Ruta.AST) { entrada ->
                val padre = remember(entrada) { nav.getBackStackEntry(Ruta.FAENA) }
                val faena: FaenaViewModel = viewModel(viewModelStoreOwner = padre)
                val ui by faena.ui.collectAsState()
                val ast: AstViewModel = viewModel()
                val actual = usuario

                LaunchedEffect(actual, ui.aperturaConfirmada) {
                    if (actual != null && actual.rol != Rol.SUPERVISOR) {
                        nav.popBackStack(Ruta.DASHBOARD, false)
                    } else if (actual != null && !ui.aperturaConfirmada) {
                        nav.popBackStack(Ruta.APERTURA, false)
                    }
                }
                LaunchedEffect(entrada) {
                    ui.ast?.let(ast::cargar)
                }
                if (actual?.rol == Rol.SUPERVISOR && ui.aperturaConfirmada) {
                    AstScreen(
                        vm = ast,
                        centro = ui.apertura.centroCultivo,
                        fecha = "${ui.apertura.fecha} ${ui.apertura.hora}",
                        onGuardar = {
                            if (faena.registrarAst(it)) {
                                nav.navigate(Ruta.SALUD) { launchSingleTop = true }
                            }
                        },
                        onVolver = { nav.popBackStack() }
                    )
                }
            }
            composable(Ruta.SALUD) { entrada ->
                val padre = remember(entrada) { nav.getBackStackEntry(Ruta.FAENA) }
                val faena: FaenaViewModel = viewModel(viewModelStoreOwner = padre)
                val ui by faena.ui.collectAsState()
                val salud: EncuestaSaludViewModel = viewModel()
                val actual = usuario

                LaunchedEffect(actual, ui.aperturaConfirmada) {
                    if (actual != null && actual.rol != Rol.SUPERVISOR) {
                        nav.popBackStack(Ruta.DASHBOARD, false)
                    } else if (actual != null && !ui.aperturaConfirmada) {
                        nav.popBackStack(Ruta.APERTURA, false)
                    }
                }
                LaunchedEffect(entrada) {
                    ui.encuesta?.let(salud::cargar)
                }
                if (actual?.rol == Rol.SUPERVISOR && ui.aperturaConfirmada) {
                    EncuestaSaludScreen(
                        vm = salud,
                        buzo = ui.apertura.buzoAsignado,
                        onGuardar = {
                            if (faena.registrarEncuesta(it)) {
                                nav.popBackStack(Ruta.APERTURA, false)
                            }
                        },
                        onVolver = { nav.popBackStack() }
                    )
                }
            }
        }
    }
}
