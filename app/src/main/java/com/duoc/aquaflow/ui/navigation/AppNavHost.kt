package com.duoc.aquaflow.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.duoc.aquaflow.domain.rules.AccessPolicy
import com.duoc.aquaflow.domain.rules.Destino
import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.Faena
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.ui.screens.apertura.AperturaScreen
import com.duoc.aquaflow.ui.screens.ast.AstScreen
import com.duoc.aquaflow.ui.screens.decision.DecisionScreen
import com.duoc.aquaflow.ui.screens.dpr24.Dpr24Screen
import com.duoc.aquaflow.ui.screens.faena.FaenaScreen
import com.duoc.aquaflow.ui.screens.historial.HistorialScreen
import com.duoc.aquaflow.ui.screens.home.HomeScreen
import com.duoc.aquaflow.ui.screens.perfil.PerfilScreen
import com.duoc.aquaflow.ui.screens.registro.RegistroScreen
import com.duoc.aquaflow.ui.screens.salud.SaludScreen
import com.duoc.aquaflow.ui.screens.usuarios.UsuariosScreen

private val argumentoFaena = listOf(navArgument(Routes.ARG_FAENA) { type = NavType.LongType })

private fun NavBackStackEntry.faenaId(): Long = arguments?.getLong(Routes.ARG_FAENA) ?: -1L

/**
 * Grafo de navegación de la sesión iniciada. El acceso por rol se comprueba aquí en cada
 * destino (no basta con ocultar botones): un usuario sin permiso es devuelto al inicio.
 */
@Composable
fun AppNavHost(usuario: Usuario) {
    val nav = rememberNavController()

    fun irATab(tab: Tab) {
        nav.navigate(tab.ruta) {
            popUpTo(Routes.HOME) { inclusive = false }
            launchSingleTop = true
        }
    }

    fun abrirFaena(faena: Faena) {
        val destino = if (usuario.rol == Rol.ADMINISTRADOR && faena.estado == EstadoFaena.PENDIENTE_AUTORIZACION) {
            Routes.decision(faena.id)
        } else {
            Routes.faena(faena.id)
        }
        nav.navigate(destino)
    }

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                usuario = usuario,
                onNuevaFaena = { nav.navigate(Routes.APERTURA) },
                onAbrirFaena = ::abrirFaena,
                onTab = ::irATab
            )
        }
        composable(Routes.HISTORIAL) {
            HistorialScreen(usuario = usuario, onAbrirFaena = ::abrirFaena, onTab = ::irATab)
        }
        composable(Routes.PERFIL) {
            PerfilScreen(usuario = usuario, onTab = ::irATab)
        }
        composable(Routes.USUARIOS) {
            Protegido(usuario, Destino.USUARIOS, nav) {
                UsuariosScreen(usuario = usuario, onTab = ::irATab)
            }
        }
        composable(Routes.APERTURA) {
            Protegido(usuario, Destino.APERTURA, nav) {
                AperturaScreen(
                    usuario = usuario,
                    onVolver = { nav.popBackStack() },
                    onCreada = { id ->
                        nav.navigate(Routes.faena(id)) {
                            popUpTo(Routes.APERTURA) { inclusive = true }
                        }
                    }
                )
            }
        }
        composable(Routes.FAENA, arguments = argumentoFaena) { entrada ->
            FaenaScreen(
                usuario = usuario,
                faenaId = entrada.faenaId(),
                onVolver = { nav.popBackStack() },
                onNavegar = { ruta -> nav.navigate(ruta) }
            )
        }
        composable(Routes.DPR24, arguments = argumentoFaena) { entrada ->
            Dpr24Screen(usuario = usuario, faenaId = entrada.faenaId(), onVolver = { nav.popBackStack() })
        }
        composable(Routes.AST, arguments = argumentoFaena) { entrada ->
            AstScreen(usuario = usuario, faenaId = entrada.faenaId(), onVolver = { nav.popBackStack() })
        }
        composable(Routes.SALUD, arguments = argumentoFaena) { entrada ->
            SaludScreen(usuario = usuario, faenaId = entrada.faenaId(), onVolver = { nav.popBackStack() })
        }
        composable(Routes.DECISION, arguments = argumentoFaena) { entrada ->
            DecisionScreen(
                usuario = usuario,
                faenaId = entrada.faenaId(),
                onVolver = { nav.popBackStack() },
                onNavegar = { ruta -> nav.navigate(ruta) }
            )
        }
        composable(Routes.REGISTRO, arguments = argumentoFaena) { entrada ->
            RegistroScreen(usuario = usuario, faenaId = entrada.faenaId(), onVolver = { nav.popBackStack() })
        }
    }
}

/** Muestra [contenido] solo si el rol del usuario puede acceder al [destino]; si no, vuelve al inicio. */
@Composable
private fun Protegido(
    usuario: Usuario,
    destino: Destino,
    nav: NavController,
    contenido: @Composable () -> Unit
) {
    val permitido = AccessPolicy.puedeAccederA(usuario.rol, destino)
    if (permitido) {
        contenido()
    } else {
        LaunchedEffect(Unit) {
            nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
        }
    }
}
