package com.duoc.aquaflow.navigation

import android.net.Uri
import com.duoc.aquaflow.model.Rol
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.duoc.aquaflow.data.local.AquaFlowDatabase
import com.duoc.aquaflow.data.remote.FaenasRemotas
import com.duoc.aquaflow.data.remote.SincronizacionFaenas
import com.duoc.aquaflow.data.local.EstadoEnvio
import kotlinx.coroutines.flow.flowOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.duoc.aquaflow.data.repository.PreChequeoRepository
import com.duoc.aquaflow.model.ResumenFaena
import com.duoc.aquaflow.model.CENTROS_MARITIMOS
import com.duoc.aquaflow.ui.navigation.Tab
import com.duoc.aquaflow.ui.components.deseleccionarAlTocarODeslizar
import com.duoc.aquaflow.ui.navigation.tabsPara
import com.duoc.aquaflow.ui.screens.AperturaFaenaScreen
import com.duoc.aquaflow.ui.screens.AstScreen
import com.duoc.aquaflow.ui.screens.BorradoresScreen
import com.duoc.aquaflow.ui.screens.ChecklistScreen
import com.duoc.aquaflow.ui.screens.DashboardScreen
import com.duoc.aquaflow.ui.screens.EncuestaSaludScreen
import com.duoc.aquaflow.ui.screens.LoginScreen
import com.duoc.aquaflow.ui.screens.ResumenFaenaScreen
import com.duoc.aquaflow.ui.screens.TareasScreen
import com.duoc.aquaflow.ui.screens.historial.DetalleAperturaScreen
import com.duoc.aquaflow.ui.screens.historial.AperturasRecibidasScreen
import com.duoc.aquaflow.ui.screens.historial.DetalleFaenaScreen
import com.duoc.aquaflow.ui.screens.historial.HistorialScreen
import com.duoc.aquaflow.viewmodel.AstViewModel
import com.duoc.aquaflow.viewmodel.AperturasRecibidasViewModel
import com.duoc.aquaflow.viewmodel.DashboardViewModel
import com.duoc.aquaflow.viewmodel.DetalleAperturaViewModel
import com.duoc.aquaflow.viewmodel.EncuestaSaludViewModel
import com.duoc.aquaflow.viewmodel.FaenaViewModel
import com.duoc.aquaflow.viewmodel.HistorialViewModel
import com.duoc.aquaflow.viewmodel.ResumenFaenaViewModel
import com.duoc.aquaflow.viewmodel.MeteorologiaViewModel
import com.duoc.aquaflow.viewmodel.ChecklistViewModel
import com.duoc.aquaflow.viewmodel.TareasViewModel
import com.duoc.aquaflow.viewmodel.PronosticoUbicacionViewModel
import com.duoc.aquaflow.data.remote.MeteorologiaApi
import com.duoc.aquaflow.data.remote.PronosticoApi
import com.duoc.aquaflow.data.repository.MeteorologiaRepository
import com.duoc.aquaflow.data.repository.ResultadoMeteorologia
import com.duoc.aquaflow.viewmodel.SesionViewModel

private const val MODO_DEMO_SIN_PERSISTENCIA = false

object Ruta {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val BORRADORES = "borradores"
    const val HISTORIAL = "historial"
    const val APERTURAS_RECIBIDAS = "aperturas-recibidas"
    const val CIERRES_RECIBIDOS = "cierres-recibidos"
    const val DETALLE_APERTURA = "historial/{aperturaId}"
    const val FAENA = "faena?aperturaId={aperturaId}"
    const val APERTURA = "faena/apertura"
    const val CHECKLIST = "faena/checklist"
    const val SALUD = "faena/salud"
    const val AST = "faena/ast"
    const val TAREAS = "faena/tareas"
    const val RESUMEN = "faena/resumen"
}

@Composable
fun AquaFlowNavHost(backendUrl: String = "", authUrl: String = "") {
    val nav = rememberNavController()
    val context = LocalContext.current.applicationContext
    val repository = remember(context) {
        PreChequeoRepository(AquaFlowDatabase.obtener(context).preChequeoDao())
    }
    val fabricaMeteorologia = remember(context) {
        viewModelFactory {
            initializer {
                val red = context.getSystemService(android.net.ConnectivityManager::class.java)
                MeteorologiaViewModel(MeteorologiaRepository(MeteorologiaApi.crear(), hayRed = {
                    red?.getNetworkCapabilities(red.activeNetwork)
                        ?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                }))
            }
        }
    }
    val sincronizacion = remember(context, backendUrl) {
        backendUrl.takeIf { it.isNotBlank() }?.let { SincronizacionFaenas(context, it) }
    }
    val sesionFactory = remember(backendUrl, authUrl, sincronizacion) {
        viewModelFactory {
            initializer {
                SesionViewModel(
                    remoto = backendUrl.takeIf { it.isNotBlank() }?.let(::FaenasRemotas),
                    sincronizacion = sincronizacion,
                    autenticacionRemota = authUrl.takeIf { it.isNotBlank() }?.let(::FaenasRemotas)
                )
            }
        }
    }
    val sesion: SesionViewModel = viewModel(factory = sesionFactory)
    val servidor = sesion.remoto
    val faenaFactory = remember(repository, servidor) {
        viewModelFactory {
            initializer {
                FaenaViewModel(
                    borradores = repository,
                    registro = repository,
                    persistenciaHabilitada = !MODO_DEMO_SIN_PERSISTENCIA
                )
            }
        }
    }
    val usuario by sesion.usuario.collectAsState()
    val supervisor = usuario?.takeIf { it.rol == Rol.SUPERVISOR }?.nombre
    val envios by remember(sincronizacion, supervisor) {
        if (supervisor == null || sincronizacion == null) flowOf(emptyList())
        else sincronizacion.observar(supervisor)
    }.collectAsState(initial = emptyList())
    val alcance = rememberCoroutineScope()

    // Si Android restaura las rutas sin una sesión, volvemos al ingreso.
    LaunchedEffect(usuario) {
        if (usuario == null && nav.currentDestination?.route != Ruta.LOGIN) {
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

    val entradaActual by nav.currentBackStackEntryAsState()
    val modifierNav = if (entradaActual?.destination?.route?.let { it != Ruta.LOGIN } == true) {
        Modifier.deseleccionarAlTocarODeslizar()
    } else {
        Modifier
    }
    NavHost(navController = nav, startDestination = Ruta.LOGIN, modifier = modifierNav) {
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
                val factory = remember(repository, actual) {
                    viewModelFactory { initializer { DashboardViewModel(repository, actual) } }
                }
                val vm: DashboardViewModel = viewModel(factory = factory)
                val state by vm.state.collectAsState()
                val fabricaPronostico = remember(context) {
                    viewModelFactory { initializer { PronosticoUbicacionViewModel(context, PronosticoApi.crear()) } }
                }
                val pronostico: PronosticoUbicacionViewModel = viewModel(factory = fabricaPronostico)

                DashboardScreen(
                    usuario = actual,
                    pronostico = pronostico,
                    tabs = tabsPara(actual.rol),
                    faenas = state.faenas,
                    cargando = state.cargando,
                    error = state.error,
                    onReintentar = vm::reintentar,
                    onVerFaena = { id -> nav.navigate("historial/${Uri.encode(id)}") },
                    onNuevaFaena = { if (actual.rol == Rol.SUPERVISOR) nav.navigate("faena") },
                    onAbrirBorradores = { if (actual.rol == Rol.SUPERVISOR) nav.navigate(Ruta.BORRADORES) },
                    onAperturasRecibidas = servidor?.let { { nav.navigate(Ruta.APERTURAS_RECIBIDAS) } },
                    envios = envios,
                    onReintentarEnvio = { id -> alcance.launch { sincronizacion?.reintentar(actual.nombre, id) } },
                    onTab = ::irATab,
                    onSalir = {
                        sesion.salir()
                        nav.navigate(Ruta.LOGIN) {
                            popUpTo(nav.graph.id) { inclusive = true }
                        }
                    }
                )
            }
        }

        listOf(Ruta.APERTURAS_RECIBIDAS, Ruta.CIERRES_RECIBIDOS).forEach { ruta ->
            composable(ruta) {
                if (usuario != null && servidor != null) {
                    val factory = remember(servidor) {
                        viewModelFactory { initializer { AperturasRecibidasViewModel(servidor) } }
                    }
                    val vm: AperturasRecibidasViewModel = viewModel(factory = factory)
                    val state by vm.state.collectAsState()
                    AperturasRecibidasScreen(
                        state, vm::actualizar,
                        soloCierres = ruta == Ruta.CIERRES_RECIBIDOS,
                        propias = usuario?.rol == Rol.SUPERVISOR,
                        onVolver = { nav.popBackStack() },
                        onDetalle = { nav.navigate("historial/${Uri.encode(it)}") }
                    )
                }
            }
        }

        composable(Ruta.BORRADORES) {
            usuario?.let { actual ->
                val factory = remember(repository, actual) {
                    viewModelFactory { initializer { DashboardViewModel(repository, actual) } }
                }
                val vm: DashboardViewModel = viewModel(factory = factory)
                val state by vm.state.collectAsState()

                BorradoresScreen(
                    faenas = state.faenas,
                    cargando = state.cargando,
                    error = state.error,
                    onReintentar = vm::reintentar,
                    onEditarBorrador = { id ->
                        if (actual.rol == Rol.SUPERVISOR) {
                            if (servidor == null) nav.navigate("faena?aperturaId=${Uri.encode(id)}")
                            else nav.navigate("historial/${Uri.encode(id)}")
                        }
                    },
                    onVolver = { nav.popBackStack() }
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
                    nombreUsuario = actual.nombre, rol = actual.rol,
                    onTexto = vm::onTexto, onCategoria = vm::onCategoria, onFecha = vm::onFecha,
                    onLimpiar = vm::limpiar, onReintentar = vm::reintentar,
                    onDetalle = { nav.navigate("historial/${Uri.encode(it)}") },
                    onCierresRecibidos = if (actual.rol == Rol.JEFE && servidor != null) {
                        { nav.navigate(Ruta.CIERRES_RECIBIDOS) }
                    } else null,
                    onSalir = {
                        sesion.salir()
                        nav.navigate(Ruta.LOGIN) {
                            popUpTo(nav.graph.id) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(Ruta.DETALLE_APERTURA,
            arguments = listOf(navArgument("aperturaId") { type = NavType.StringType })
        ) { entrada ->
            usuario?.let { actual ->
                val id = requireNotNull(entrada.arguments?.getString("aperturaId"))
                val factory = remember(repository, actual, id, servidor) {
                    viewModelFactory {
                        initializer { DetalleAperturaViewModel(repository, actual, id, repository, repository, servidor, context) }
                    }
                }
                val vm: DetalleAperturaViewModel = viewModel(factory = factory)
                val state by vm.state.collectAsState()
                val faenaGuardada = state.faena
                val envio = envios.firstOrNull { it.faena == id }
                LaunchedEffect(envio?.estado) {
                    if (envio?.estado == EstadoEnvio.SINCRONIZADO.name) vm.reintentar()
                }
                if (faenaGuardada != null) {
                    val apertura = faenaGuardada.resumen.apertura
                    val centro = CENTROS_MARITIMOS.firstOrNull { it.id == apertura.centroId }
                        ?: CENTROS_MARITIMOS.firstOrNull { it.nombre == apertura.centroCultivo }
                    val climaVm: MeteorologiaViewModel = viewModel(key = "clima-detalle-$id", factory = fabricaMeteorologia)
                    val climaDetalle by climaVm.ui.collectAsState()
                    DetalleFaenaScreen(
                        vm = viewModel(),
                        faena = faenaGuardada,
                        rol = actual.rol,
                        nombreUsuario = actual.nombre,
                        onDecidir = vm::decidir,
                        onAuditarAutorizacion = vm::auditarAutorizacion,
                        onVolver = { nav.popBackStack() },
                        onFinalizar = { nav.popBackStack() },
                        climaActual = climaDetalle,
                        onConsultarClima = centro?.let { ubicacion ->
                            {
                                if (climaDetalle.centro == ubicacion) climaVm.actualizar()
                                else climaVm.seleccionar(ubicacion.id)
                            }
                        },
                        onCerrar = vm::cerrar,
                        cierrePublicado = state.cierrePublicado,
                        onIniciar = vm::iniciar,
                        ocupado = state.guardando,
                        errorExterno = state.errorDecision ?: state.error,
                        envio = envio,
                        expedienteRemoto = state.expedienteRemoto,
                        onActualizar = servidor?.let { vm::reintentar },
                        onRetirar = if (actual.rol == Rol.SUPERVISOR) servidor?.let { vm::retirar } else null,
                        onEditarExpediente = if (actual.rol == Rol.SUPERVISOR && servidor != null) {
                            if (envio?.estado == EstadoEnvio.PENDIENTE.name) null
                            else { { vm.prepararEdicion(repository, envio?.estado == EstadoEnvio.SINCRONIZADO.name) {
                                nav.navigate("faena?aperturaId=${Uri.encode(id)}")
                            } } }
                        } else null,
                        onRevisarAlerta = if (actual.rol == Rol.JEFE) servidor?.let { vm::revisarAlerta } else null
                    )
                } else {
                    DetalleAperturaScreen(state, onVolver = { nav.popBackStack() }, onReintentar = vm::reintentar)
                }
            }
        }

        navigation(
            startDestination = Ruta.APERTURA,
            route = Ruta.FAENA,
            arguments = listOf(navArgument("aperturaId") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) {
            composable(Ruta.APERTURA) { entrada ->
                val graphEntry = remember(entrada) { nav.getBackStackEntry(Ruta.FAENA) }
                val aperturaId = graphEntry.arguments?.getString("aperturaId")
                val faena: FaenaViewModel = viewModel(viewModelStoreOwner = graphEntry, factory = faenaFactory)
                val ui by faena.ui.collectAsState()
                val actual = usuario

                LaunchedEffect(actual, aperturaId) {
                    actual?.let {
                        if (aperturaId != null) faena.iniciar(it, aperturaId)
                        else {
                            faena.iniciar(it)
                        }
                    }
                }

                val clima: MeteorologiaViewModel = viewModel(viewModelStoreOwner = graphEntry, factory = fabricaMeteorologia)
                val meteorologia by clima.ui.collectAsState()
                LaunchedEffect(ui.apertura.centroId) { clima.seleccionar(ui.apertura.centroId) }
                LaunchedEffect(ui.apertura.id, meteorologia) {
                    val condiciones = (meteorologia.resultado as? ResultadoMeteorologia.Disponible)?.condiciones
                    val centro = meteorologia.centro
                    if (condiciones != null && centro != null) {
                        faena.registrarMeteorologia(ui.apertura.id, centro.id, condiciones)
                    }
                }
                AperturaFaenaScreen(
                    ui = ui,
                    meteorologia = meteorologia,
                    onActualizarMeteorologia = clima::actualizar,
                    onEditarImc = faena::editarImc,
                    onCambiar = faena::actualizar,
                    onGuardar = {
                        faena.guardarApertura {
                            nav.popBackStack(Ruta.DASHBOARD, inclusive = false)
                        }
                    },
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
                val graphEntry = remember(entrada) { nav.getBackStackEntry(Ruta.FAENA) }
                val faena: FaenaViewModel = viewModel(viewModelStoreOwner = graphEntry, factory = faenaFactory)
                val ui by faena.ui.collectAsState()
                if (ui.apertura.id.isBlank()) return@composable
                val checklistVm: ChecklistViewModel = viewModel(
                    viewModelStoreOwner = graphEntry, key = "checklist-${ui.apertura.id}-${ui.revisionFormularios}"
                )
                ChecklistScreen(
                    ocupado = ui.ocupado, errorExterno = ui.error,
                    faenaId = ui.apertura.id,
                    checklistInicial = ui.checklist,
                    vm = checklistVm,
                    centroCultivo = ui.apertura.centroCultivo,
                    supervisor = ui.apertura.supervisorACargo,
                    mensaje = ui.mensaje,
                    onBloqueo = { faena.registrarBloqueo("Continuar desde checklist", it) },
                    onGuardar = { checklist ->
                        faena.registrarChecklist(checklist) { nav.navigate(Ruta.SALUD) }
                    },
                    onGuardarBorrador = { checklist ->
                        faena.guardarBorradorChecklist(checklist) {
                            nav.popBackStack(Ruta.DASHBOARD, inclusive = false)
                        }
                    },
                    onVolver = { nav.popBackStack() }
                )
            }

            composable(Ruta.SALUD) { entrada ->
                val graphEntry = remember(entrada) { nav.getBackStackEntry(Ruta.FAENA) }
                val faena: FaenaViewModel = viewModel(viewModelStoreOwner = graphEntry, factory = faenaFactory)
                val ui by faena.ui.collectAsState()
                if (ui.apertura.id.isBlank()) return@composable
                val saludVm: EncuestaSaludViewModel = viewModel(
                    viewModelStoreOwner = graphEntry, key = "salud-${ui.apertura.id}-${ui.revisionFormularios}"
                )
                LaunchedEffect(saludVm, ui.encuesta) {
                    ui.encuesta?.let(saludVm::cargar)
                }

                EncuestaSaludScreen(
                    vm = saludVm,
                    ocupado = ui.ocupado, errorExterno = ui.error,
                    buzo = ui.apertura.buzoAsignado.ifBlank { "Buzo por asignar" },
                    centroCultivo = ui.apertura.centroCultivo,
                    onGuardar = { encuesta ->
                        faena.registrarEncuesta(encuesta) { nav.navigate(Ruta.AST) }
                    },
                    onGuardarBorrador = { encuesta ->
                        faena.guardarBorradorEncuesta(encuesta) {
                            nav.popBackStack(Ruta.DASHBOARD, inclusive = false)
                        }
                    },
                    onVolver = { nav.popBackStack() }
                )
            }

            composable(Ruta.AST) { entrada ->
                val graphEntry = remember(entrada) { nav.getBackStackEntry(Ruta.FAENA) }
                val faena: FaenaViewModel = viewModel(viewModelStoreOwner = graphEntry, factory = faenaFactory)
                val ui by faena.ui.collectAsState()

                if (ui.apertura.id.isBlank()) return@composable
                val astVm: AstViewModel = viewModel(
                    viewModelStoreOwner = graphEntry, key = "ast-${ui.apertura.id}-${ui.revisionFormularios}"
                )
                LaunchedEffect(astVm, ui.ast) { ui.ast?.let(astVm::cargar) }
                AstScreen(
                    vm = astVm,
                    ocupado = ui.ocupado, errorExterno = ui.error,
                    mensaje = ui.mensaje,
                    onBloqueo = { faena.registrarBloqueo("Continuar desde AST", it) },
                    centro = ui.apertura.centroCultivo.ifBlank { "Centro por definir" },
                    fecha = ui.apertura.fecha.ifBlank { "Sin fecha" },
                    onGuardar = { ast ->
                        faena.registrarAst(ast) { nav.navigate(Ruta.TAREAS) }
                    },
                    onGuardarBorrador = { ast ->
                        faena.guardarBorradorAst(ast) {
                            nav.popBackStack(Ruta.DASHBOARD, inclusive = false)
                        }
                    },
                    onVolver = { nav.popBackStack() }
                )
            }

            composable(Ruta.TAREAS) { entrada ->
                val graphEntry = remember(entrada) { nav.getBackStackEntry(Ruta.FAENA) }
                val faena: FaenaViewModel = viewModel(viewModelStoreOwner = graphEntry, factory = faenaFactory)
                val ui by faena.ui.collectAsState()
                if (ui.apertura.id.isBlank()) return@composable
                val tareasVm: TareasViewModel = viewModel(
                    viewModelStoreOwner = graphEntry, key = "tareas-${ui.apertura.id}-${ui.revisionFormularios}"
                )
                TareasScreen(
                    id = ui.apertura.id,
                    inicial = ui.tareas,
                    vm = tareasVm,
                    ocupado = ui.ocupado,
                    errorExterno = ui.error,
                    onGuardar = { tareas ->
                        faena.registrarTareas(tareas) { nav.navigate(Ruta.RESUMEN) }
                    },
                    onBorrador = { tareas ->
                        faena.guardarBorradorTareas(tareas) {
                            nav.popBackStack(Ruta.DASHBOARD, inclusive = false)
                        }
                    },
                    onVolver = { nav.popBackStack() }
                )
            }

            composable(Ruta.RESUMEN) { entrada ->
                val graphEntry = remember(entrada) { nav.getBackStackEntry(Ruta.FAENA) }
                val faena: FaenaViewModel = viewModel(viewModelStoreOwner = graphEntry, factory = faenaFactory)
                val ui by faena.ui.collectAsState()
                val actual = usuario

                if (actual != null) {
                    val resumen = ResumenFaena(ui.apertura, ui.checklist, ui.ast, ui.encuesta, ui.tareas)
                    val vm: ResumenFaenaViewModel = viewModel()
                    ResumenFaenaScreen(
                        vm = vm,
                        resumen = resumen,
                        decision = ui.decision,
                        rol = actual.rol,
                        nombreUsuario = actual.nombre,
                        modoDemo = MODO_DEMO_SIN_PERSISTENCIA,
                        onDecidir = {},
                        onVolver = { nav.popBackStack() },
                        textoFinalizar = if (sincronizacion == null) "Ir al inicio" else "Guardar para sincronizar",
                        onFinalizar = {
                            val irAlInicio = {
                                nav.navigate(Ruta.DASHBOARD) {
                                    popUpTo(Ruta.DASHBOARD) { inclusive = true }
                                }
                            }
                            if (sincronizacion == null) irAlInicio()
                            else vm.enviar({ sincronizacion.encolar(actual.nombre, resumen) }, irAlInicio)
                        }
                    )
                }
            }
        }
    }
}
