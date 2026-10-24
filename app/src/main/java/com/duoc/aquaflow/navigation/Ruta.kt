package com.duoc.aquaflow.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.ResumenFaena
import com.duoc.aquaflow.ui.navigation.Tab
import com.duoc.aquaflow.ui.navigation.tabsPara
import com.duoc.aquaflow.ui.screens.AperturaFaenaScreen
import com.duoc.aquaflow.ui.screens.AstScreen
import com.duoc.aquaflow.ui.screens.DashboardScreen
import com.duoc.aquaflow.ui.screens.EncuestaSaludScreen
import com.duoc.aquaflow.ui.screens.LoginScreen
import com.duoc.aquaflow.ui.screens.ResumenFaenaScreen
import com.duoc.aquaflow.ui.screens.historial.HistorialScreen
import com.duoc.aquaflow.viewmodel.FaenaViewModel
import com.duoc.aquaflow.viewmodel.SesionViewModel

object Ruta {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val HISTORIAL = "historial"
    const val FAENA = "faena"
    const val APERTURA = "faena/apertura"
    const val SALUD = "faena/salud"
    const val AST = "faena/ast"
    const val RESUMEN = "faena/resumen"
}

@Composable
fun AquaFlowNavHost() {
    val nav = rememberNavController()
    val sesion: SesionViewModel = viewModel()
    val usuario by sesion.usuario.collectAsState()
    var ultimaEncuesta by remember { mutableStateOf<EncuestaSalud?>(null) }

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
                    usuario = actual,
                    tabs = tabsPara(actual.rol),
                    ultimaEncuesta = ultimaEncuesta,
                    onNuevaFaena = { nav.navigate(Ruta.APERTURA) },
                    onTab = ::irATab,
                    onSalir = {
                        sesion.salir()
                        ultimaEncuesta = null
                        nav.navigate(Ruta.LOGIN) {
                            popUpTo(nav.graph.id) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(Ruta.HISTORIAL) {
            usuario?.let { actual ->
                HistorialScreen(usuario = actual, tabs = tabsPara(actual.rol), onTab = ::irATab)
            }
        }

        navigation(startDestination = Ruta.APERTURA, route = Ruta.FAENA) {
            composable(Ruta.APERTURA) { entrada ->
                val padre = remember(entrada) { nav.getBackStackEntry(Ruta.FAENA) }
                val faena: FaenaViewModel = viewModel(viewModelStoreOwner = padre)
                val ui by faena.ui.collectAsState()
                val actual = usuario

                LaunchedEffect(actual) {
                    actual?.let { faena.iniciar(it) }
                }

                AperturaFaenaScreen(
                    ui = ui,
                    onCambiar = faena::actualizar,
                    onContinuar = {
                        if (faena.confirmarApertura()) {
                            nav.navigate(Ruta.SALUD)
                        }
                    },
                    onSalir = {
                        nav.popBackStack(Ruta.DASHBOARD, inclusive = false)
                    }
                )
            }

            composable(Ruta.SALUD) { entrada ->
                val padre = remember(entrada) { nav.getBackStackEntry(Ruta.FAENA) }
                val faena: FaenaViewModel = viewModel(viewModelStoreOwner = padre)
                val ui by faena.ui.collectAsState()

                EncuestaSaludScreen(
                    vm = viewModel(),
                    buzo = ui.apertura.buzoAsignado.ifBlank { "Buzo por asignar" },
                    tabs = usuario?.let { tabsPara(it.rol) } ?: Tab.entries.toList(),
                    onGuardar = { encuesta ->
                        faena.registrarEncuesta(encuesta)
                        ultimaEncuesta = encuesta
                        nav.navigate(Ruta.AST)
                    },
                    onVolver = { nav.popBackStack() },
                    onTab = ::irATab
                )
            }

            composable(Ruta.AST) { entrada ->
                val padre = remember(entrada) { nav.getBackStackEntry(Ruta.FAENA) }
                val faena: FaenaViewModel = viewModel(viewModelStoreOwner = padre)
                val ui by faena.ui.collectAsState()

                AstScreen(
                    vm = viewModel(),
                    centro = ui.apertura.centroCultivo.ifBlank { "Centro por definir" },
                    fecha = ui.apertura.fecha.ifBlank { "Sin fecha" },
                    onGuardar = { ast ->
                        faena.registrarAst(ast)
                        nav.navigate(Ruta.RESUMEN)
                    },
                    onVolver = { nav.popBackStack() }
                )
            }

            composable(Ruta.RESUMEN) { entrada ->
                val padre = remember(entrada) { nav.getBackStackEntry(Ruta.FAENA) }
                val faena: FaenaViewModel = viewModel(viewModelStoreOwner = padre)
                val ui by faena.ui.collectAsState()
                val actual = usuario

                if (actual != null) {
                    ResumenFaenaScreen(
                        vm = viewModel(),
                        resumen = ResumenFaena(
                            apertura = ui.apertura,
                            checklist = null,
                            ast = ui.ast,
                            encuesta = ui.encuesta
                        ),
                        decision = ui.decision,
                        rol = actual.rol,
                        nombreUsuario = actual.nombre,
                        onDecidir = { decision ->
                            faena.registrarDecision(decision)
                        },
                        onVolver = { nav.popBackStack() },
                        onFinalizar = {
                            nav.navigate(Ruta.DASHBOARD) {
                                popUpTo(Ruta.DASHBOARD) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}
