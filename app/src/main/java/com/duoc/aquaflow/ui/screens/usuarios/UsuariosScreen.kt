package com.duoc.aquaflow.ui.screens.usuarios

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoc.aquaflow.domain.rules.Rut
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.ui.components.AquaScreen
import com.duoc.aquaflow.ui.components.AquaTextField
import com.duoc.aquaflow.ui.components.LocalAppContainer
import com.duoc.aquaflow.ui.components.MensajeSnackbar
import com.duoc.aquaflow.ui.components.OpcionCard
import com.duoc.aquaflow.ui.components.ScreenColumn
import com.duoc.aquaflow.ui.components.SectionCard
import com.duoc.aquaflow.ui.components.StatusChip
import com.duoc.aquaflow.ui.components.Tone
import com.duoc.aquaflow.ui.navigation.MainBottomBar
import com.duoc.aquaflow.ui.navigation.Tab
import com.duoc.aquaflow.ui.theme.AquaNavy
import com.duoc.aquaflow.ui.theme.AquaOnSurfaceVariant
import com.duoc.aquaflow.ui.theme.AquaOrange
import com.duoc.aquaflow.viewmodel.UsuariosViewModel
import com.duoc.aquaflow.viewmodel.vmFactory

/** Administración de usuarios (solo administrador): alta con RUT y PIN, activación y desactivación. */
@Composable
fun UsuariosScreen(usuario: Usuario, onTab: (Tab) -> Unit) {
    val container = LocalAppContainer.current
    val vm: UsuariosViewModel = viewModel(
        key = "usuarios-${usuario.id}",
        factory = vmFactory { UsuariosViewModel(usuario, container.usuarioRepository) }
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    MensajeSnackbar(state.mensaje, snackbar, vm::consumirMensaje)

    AquaScreen(
        title = "Usuarios",
        subtitle = "Administración",
        snackbarHostState = snackbar,
        bottomBar = { MainBottomBar(Tab.USUARIOS, usuario.rol, onTab) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = vm::abrirDialogo,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nuevo usuario") },
                containerColor = AquaOrange,
                contentColor = AquaNavy
            )
        }
    ) { padding ->
        ScreenColumn(padding) {
            state.usuarios.forEach { item ->
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.nombre, style = MaterialTheme.typography.titleMedium)
                            Text(Rut.formatear(item.rut), style = MaterialTheme.typography.bodyMedium, color = AquaOnSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatusChip(item.rol.etiqueta, tono = Tone.INFO)
                                StatusChip(if (item.activo) "Activo" else "Inactivo", tono = if (item.activo) Tone.SUCCESS else Tone.NEUTRAL)
                            }
                        }
                        Switch(
                            checked = item.activo,
                            onCheckedChange = { vm.cambiarActivo(item, it) },
                            enabled = item.id != usuario.id
                        )
                    }
                }
            }
            Spacer(Modifier.height(72.dp))
        }
    }

    if (state.dialogoAbierto) {
        val form = state.form
        AlertDialog(
            onDismissRequest = { if (!state.guardando) vm.cerrarDialogo() },
            title = { Text("Nuevo usuario") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AquaTextField(form.nombre, vm::onNombre, "Nombre completo", error = state.errores["nombre"])
                    AquaTextField(form.rut, vm::onRut, "RUT", error = state.errores["rut"], ayuda = "Ej.: 12.345.678-5")
                    Text("Rol", style = MaterialTheme.typography.labelLarge)
                    Rol.values().forEach { rol ->
                        OpcionCard(titulo = rol.etiqueta, seleccionado = form.rol == rol, onClick = { vm.onRol(rol) })
                    }
                    AquaTextField(
                        form.pin, vm::onPin, "PIN (4 dígitos)",
                        error = state.errores["pin"],
                        keyboardType = KeyboardType.NumberPassword,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    AquaTextField(
                        form.pinConfirmacion, vm::onPinConfirmacion, "Repite el PIN",
                        error = state.errores["pinConfirmacion"],
                        keyboardType = KeyboardType.NumberPassword,
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = vm::crear, enabled = !state.guardando) { Text("Crear usuario") }
            },
            dismissButton = {
                TextButton(onClick = vm::cerrarDialogo, enabled = !state.guardando) { Text("Cancelar") }
            }
        )
    }
}
