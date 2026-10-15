package com.duoc.aquaflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duoc.aquaflow.di.AppContainer
import com.duoc.aquaflow.model.SessionState
import com.duoc.aquaflow.ui.components.LoadingBox
import com.duoc.aquaflow.ui.components.LocalAppContainer
import com.duoc.aquaflow.ui.navigation.AppNavHost
import com.duoc.aquaflow.ui.screens.login.LoginScreen
import com.duoc.aquaflow.ui.theme.AquaFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as AquaFlowApplication).container
        setContent {
            AquaFlowTheme {
                CompositionLocalProvider(LocalAppContainer provides container) {
                    AquaFlowRoot(container)
                }
            }
        }
    }
}

/**
 * Raíz de la UI. Sin sesión solo se muestra el login: el grafo de navegación autenticado ni siquiera
 * existe, por lo que no se puede llegar a una pantalla protegida sin iniciar sesión.
 */
@Composable
private fun AquaFlowRoot(container: AppContainer) {
    val session by container.authRepository.session.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { container.authRepository.restoreSession() }

    when (val actual = session) {
        SessionState.Loading -> LoadingBox(Modifier.fillMaxSize())
        SessionState.LoggedOut -> LoginScreen()
        is SessionState.LoggedIn -> AppNavHost(usuario = actual.usuario)
    }
}
