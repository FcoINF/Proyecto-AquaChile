package com.duoc.aquaflow

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.ui.screens.LoginScreen
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import com.duoc.aquaflow.viewmodel.SesionViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AquaFlowTheme {
                val sesionViewModel: SesionViewModel = viewModel()
                val usuarioActivo by sesionViewModel.usuario.collectAsState()

                // Si no hay usuario logueado, muestra LoginScreen
                if (usuarioActivo == null) {
                    LoginScreen(
                        sesion = sesionViewModel,
                        onIngresoExitoso = { /* El cambio en usuarioActivo redibuja la pantalla automáticamente */ }
                    )
                } else {
                    // Verifica si el rol es SUPERVISOR
                    val puedeCrear = usuarioActivo?.rol == Rol.SUPERVISOR

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        floatingActionButton = {
                            // Solo el SUPERVISOR ve el botón para registrar; el JEFE solo consulta
                            if (puedeCrear) {
                                ExtendedFloatingActionButton(onClick = { /* Ir a NuevoPreChequeo */ }) {
                                    Text("Nuevo pre-chequeo")
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
                            Text("Bienvenido: ${usuarioActivo?.nombre}")
                            Text("Rol activo: ${usuarioActivo?.rol}")
                            Text(
                                if (puedeCrear) "Permisos: Registro y consulta de pre-chequeos"
                                else "Permisos: Solo consulta de historial y detalles"
                            )
                            Button(onClick = { sesionViewModel.salir() }) {
                                Text("Cerrar sesión")
                            }
                        }
                    }
                }
            }
        }
    }
}