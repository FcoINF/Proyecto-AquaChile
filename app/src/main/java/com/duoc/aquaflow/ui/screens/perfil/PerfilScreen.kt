package com.duoc.aquaflow.ui.screens.perfil

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoc.aquaflow.BuildConfig
import com.duoc.aquaflow.domain.rules.Rut
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.ui.components.AquaScreen
import com.duoc.aquaflow.ui.components.ConfirmDialog
import com.duoc.aquaflow.ui.components.DangerButton
import com.duoc.aquaflow.ui.components.DatoFila
import com.duoc.aquaflow.ui.components.LocalAppContainer
import com.duoc.aquaflow.ui.components.MensajeSnackbar
import com.duoc.aquaflow.ui.components.ScreenColumn
import com.duoc.aquaflow.ui.components.SecondaryButton
import com.duoc.aquaflow.ui.components.SectionCard
import com.duoc.aquaflow.ui.navigation.MainBottomBar
import com.duoc.aquaflow.ui.navigation.Tab
import com.duoc.aquaflow.ui.theme.AquaOnSurfaceVariant
import com.duoc.aquaflow.viewmodel.PerfilViewModel
import com.duoc.aquaflow.viewmodel.vmFactory

@Composable
fun PerfilScreen(usuario: Usuario, onTab: (Tab) -> Unit) {
    val container = LocalAppContainer.current
    val vm: PerfilViewModel = viewModel(
        key = "perfil-${usuario.id}",
        factory = vmFactory {
            PerfilViewModel(container.authRepository, container.faenaRepository, container.syncGateway)
        }
    )
    val pendientes by vm.pendientesSync.collectAsStateWithLifecycle()
    val mensaje by vm.mensaje.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmarSalida by rememberSaveable { mutableStateOf(false) }
    MensajeSnackbar(mensaje, snackbar, vm::consumirMensaje)

    AquaScreen(
        title = usuario.nombre,
        subtitle = "Perfil",
        snackbarHostState = snackbar,
        bottomBar = { MainBottomBar(Tab.PERFIL, usuario.rol, onTab) }
    ) { padding ->
        ScreenColumn(padding) {
            SectionCard(title = "Mi cuenta") {
                DatoFila("Nombre", usuario.nombre)
                DatoFila("RUT", Rut.formatear(usuario.rut))
                DatoFila("Rol", usuario.rol.etiqueta)
            }
            SectionCard(title = "Datos y sincronización") {
                Text(
                    "La información se guarda en este dispositivo (modo local). Registros pendientes de sincronizar: $pendientes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AquaOnSurfaceVariant
                )
                SecondaryButton("Sincronizar ahora", onClick = vm::sincronizar)
            }
            SectionCard(title = "Aplicación") {
                DatoFila("Versión", BuildConfig.VERSION_NAME)
            }
            DangerButton(
                texto = "Cerrar sesión",
                onClick = { confirmarSalida = true },
                icono = Icons.AutoMirrored.Filled.Logout
            )
        }
    }

    if (confirmarSalida) {
        ConfirmDialog(
            titulo = "Cerrar sesión",
            texto = "Tus faenas y evidencias seguirán guardadas en este dispositivo.",
            textoConfirmar = "Cerrar sesión",
            onConfirmar = {
                confirmarSalida = false
                vm.cerrarSesion()
            },
            onCerrar = { confirmarSalida = false }
        )
    }
}
