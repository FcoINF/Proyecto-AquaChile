package com.duoc.aquaflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.ui.components.SemaforoSalud
import com.duoc.aquaflow.ui.screens.EncuestaSaludScreen
import com.duoc.aquaflow.ui.screens.LoginScreen
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import com.duoc.aquaflow.viewmodel.SesionViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AquaFlowTheme { AquaFlowApp() }
        }
    }
}

// Decide qué pantalla se ve: login, inicio o encuesta de salud.
// Sin librería de navegación: con tres estados basta una variable.
@Composable
private fun AquaFlowApp() {
    val sesion: SesionViewModel = viewModel()
    val usuario by sesion.usuario.collectAsState()

    var mostrarEncuesta by rememberSaveable { mutableStateOf(false) }
    // Cambia en cada encuesta guardada para que la siguiente parta con un ViewModel limpio
    var intento by rememberSaveable { mutableIntStateOf(0) }
    var ultimaEncuesta by remember { mutableStateOf<EncuestaSalud?>(null) }

    val u = usuario
    if (u == null) {
        // Si no hay usuario logueado, muestra LoginScreen
        LoginScreen(
            sesion = sesion,
            onIngresoExitoso = { /* El cambio en usuario redibuja la pantalla automáticamente */ }
        )
    } else if (mostrarEncuesta) {
        // El botón "atrás" del teléfono vuelve al inicio en vez de cerrar la app
        BackHandler { mostrarEncuesta = false }
        EncuestaSaludScreen(
            vm = viewModel(key = "encuesta-$intento"),
            buzo = "Buzo por asignar",   // vendrá de la apertura de faena
            onGuardar = {
                ultimaEncuesta = it
                intento++
                mostrarEncuesta = false
            },
            onVolver = { mostrarEncuesta = false }
        )
    } else {
        // Verifica si el rol es SUPERVISOR
        val puedeCrear = u.rol == Rol.SUPERVISOR

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            floatingActionButton = {
                // Solo el SUPERVISOR ve el botón para registrar; el JEFE solo consulta
                if (puedeCrear) {
                    ExtendedFloatingActionButton(onClick = { mostrarEncuesta = true }) {
                        Text("Encuesta de salud")
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Bienvenido: ${u.nombre}")
                Text("Rol activo: ${u.rol}")
                Text(
                    if (puedeCrear) "Permisos: Registro y consulta de pre-chequeos"
                    else "Permisos: Solo consulta de historial y detalles"
                )

                // Resultado de la última encuesta guardada en esta sesión
                ultimaEncuesta?.let { e ->
                    Text("Última encuesta: ${e.sistolica}/${e.diastolica} mmHg · Sat. ${e.saturacion}%")
                    Text("Alertas en la encuesta: ${e.alertasEncuesta.size}")
                    SemaforoSalud(e.nivel)
                }

                Button(onClick = {
                    sesion.salir()
                    mostrarEncuesta = false
                    ultimaEncuesta = null
                }) {
                    Text("Cerrar sesión")
                }
            }
        }
    }
}