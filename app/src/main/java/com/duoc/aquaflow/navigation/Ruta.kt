package com.duoc.aquaflow.navigation

import android.net.Uri
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.duoc.aquaflow.ui.screens.historial.DetalleAperturaScreen
import com.duoc.aquaflow.viewmodel.HistorialViewModel
import com.duoc.aquaflow.viewmodel.DetalleAperturaViewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.duoc.aquaflow.data.local.AquaFlowDatabase
import com.duoc.aquaflow.data.repository.PreChequeoRepository
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
import com.duoc.aquaflow.ui.screens.ChecklistScreen
import com.duoc.aquaflow.ui.screens.DashboardScreen
import com.duoc.aquaflow.ui.screens.EncuestaSaludScreen
import com.duoc.aquaflow.ui.screens.LoginScreen
import com.duoc.aquaflow.ui.screens.ResumenFaenaScreen
import com.duoc.aquaflow.ui.screens.historial.HistorialScreen
import com.duoc.aquaflow.viewmodel.EncuestaSaludViewModel
import com.duoc.aquaflow.viewmodel.FaenaViewModel
import com.duoc.aquaflow.viewmodel.SesionViewModel

object Ruta {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val HISTORIAL = "historial"
    const val DETALLE_APERTURA = "historial/{aperturaId}"
    const val FAENA = "faena"
    const val APERTURA = "faena/apertura"
    const val CHECKLIST = "faena/checklist"
    const val SALUD = "faena/salud"
    const val AST = "faena/ast"
    const val RESUMEN = "faena/resumen"
}

@Composable
fun AquaFlowNavHost() {
    val nav = rememberNavController()
    val context = LocalContext.current.applicationContext
    val repository = remember(context) {
        PreChequeoRepository(AquaFlowDatabase.obtener(context).preChequeoDao())
    }
    val faenaFactory = remember(context) {
        viewModelFactory {
            initializer {
                FaenaViewModel(repository)
            }
        }
    }
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
                    onNuevaFaena = { nav.navigate(Ruta.FAENA) },
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
                val factory = remember(repository, actual) {
                    viewModelFactory { initializer { HistorialViewModel(repository, actual) } }
                }
                val vm: HistorialViewModel = viewModel(factory = factory)
                val state by vm.state.collectAsState()
                HistorialScreen(
                    state = state, tabs = tabsPara(actual.rol), onTab = ::irATab,
                    onTexto = vm::onTexto, onEstado = vm::onEstado,
                    onLimpiar = vm::limpiar, onReintentar = vm::reintentar,
                    onDetalle = { nav.navigate("historial/${Uri.encode(it)}") }
                )
            }
        }

        composable(Ruta.DETALLE_APERTURA,
            arguments = listOf(navArgument("aperturaId") { type = NavType.StringType })
        ) { entrada ->
            usuario?.let { actual ->
                val id = requireNotNull(entrada.arguments?.getString("aperturaId"))
                val factory = remember(repository, actual, id) {
                    viewModelFactory { initializer { DetalleAperturaViewModel(repository, actual, id) } }
                }
                val vm: DetalleAperturaViewModel = viewModel(factory = factory)
                val state by vm.state.collectAsState()
                DetalleAperturaScreen(state, onVolver = { nav.popBackStack() }, onReintentar = vm::reintentar)
            }
        }

        navigation(startDestination = Ruta.APERTURA, route = Ruta.FAENA) {
            composable(Ruta.APERTURA) { entrada ->
                val padre = remember(entrada) { nav.getBackStackEntry(Ruta.FAENA) }
                val faena: FaenaViewModel = viewModel(viewModelStoreOwner = padre, factory = faenaFactory)
                val ui by faena.ui.collectAsState()
                val actual = usuario

                LaunchedEffect(actual) {
                    actual?.let { faena.iniciar(it) }
                }

                AperturaFaenaScreen(
                    ui = ui,
                    onCambiar = faena::actualizar,
                    onGuardar = { faena.guardarApertura() },
                    onRecuperar = faena::recuperarApertura,
                    onContinuar = {
                        faena.guardarApertura(continuar = true) {
                            nav.navigate(Ruta.CHECKLIST)
                        }
                    },
                    onSalir = {
                        nav.popBackStack(Ruta.DASHBOARD, inclusive = false)
                    }
                )
            }

            composable(Ruta.CHECKLIST) { entrada ->
                val padre = remember(entrada) { nav.getBackStackEntry(Ruta.FAENA) }
                val faena: FaenaViewModel = viewModel(viewModelStoreOwner = padre, factory = faenaFactory)
                val ui by faena.ui.collectAsState()

                ChecklistScreen(
                    faenaId = ui.apertura.id,
                    checklistInicial = ui.checklist,
                    onGuardar = { checklist ->
                        faena.registrarChecklist(checklist)
                        nav.navigate(Ruta.SALUD)
                    },
                    onVolver = { nav.popBackStack() }
                )
            }

            composable(Ruta.SALUD) { entrada ->
                val padre = remember(entrada) {
                    try {
                        nav.getBackStackEntry(Ruta.FAENA)
                    } catch (_: Exception) {
                        null
                    }
                }
                val faena: FaenaViewModel? = padre?.let {
                    viewModel(viewModelStoreOwner = it, factory = faenaFactory)
                }
                val ui = faena?.ui?.collectAsState()?.value

                val saludVm: EncuestaSaludViewModel = viewModel(key = "EncuestaSaludViewModel")
                val encuestaActual = ui?.encuesta ?: ultimaEncuesta
                LaunchedEffect(encuestaActual) {
                    encuestaActual?.let { saludVm.cargar(it) }
                }

                EncuestaSaludScreen(
                    vm = saludVm,
                    buzo = ui?.apertura?.buzoAsignado?.ifBlank { "Buzo por asignar" } ?: "Buzo por asignar",
                    tabs = usuario?.let { tabsPara(it.rol) } ?: Tab.entries.toList(),
                    onGuardar = { encuesta ->
                        faena?.registrarEncuesta(encuesta)
                        ultimaEncuesta = encuesta
                        if (faena != null) {
                            nav.navigate(Ruta.AST)
                        } else {
                            nav.popBackStack()
                        }
                    },
                    onVolver = { nav.popBackStack() },
                    onTab = ::irATab
                )
            }

            composable(Ruta.AST) { entrada ->
                val padre = remember(entrada) { nav.getBackStackEntry(Ruta.FAENA) }
                val faena: FaenaViewModel = viewModel(viewModelStoreOwner = padre, factory = faenaFactory)
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
                val faena: FaenaViewModel = viewModel(viewModelStoreOwner = padre, factory = faenaFactory)
                val ui by faena.ui.collectAsState()
                val actual = usuario

                if (actual != null) {
                    ResumenFaenaScreen(
                        vm = viewModel(),
                        resumen = ResumenFaena(
                            apertura = ui.apertura,
                            checklist = ui.checklist,
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
