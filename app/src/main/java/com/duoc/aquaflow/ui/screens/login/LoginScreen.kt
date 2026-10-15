package com.duoc.aquaflow.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoc.aquaflow.BuildConfig
import com.duoc.aquaflow.data.seed.SeedData
import com.duoc.aquaflow.domain.rules.Rut
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.ui.components.AlertBanner
import com.duoc.aquaflow.ui.components.AquaScreen
import com.duoc.aquaflow.ui.components.AquaTextField
import com.duoc.aquaflow.ui.components.LocalAppContainer
import com.duoc.aquaflow.ui.components.OpcionCard
import com.duoc.aquaflow.ui.components.PrimaryButton
import com.duoc.aquaflow.ui.components.ScreenColumn
import com.duoc.aquaflow.ui.components.SectionCard
import com.duoc.aquaflow.ui.components.Tone
import com.duoc.aquaflow.ui.theme.AquaOnSurfaceVariant
import com.duoc.aquaflow.viewmodel.LoginViewModel
import com.duoc.aquaflow.viewmodel.vmFactory

@Composable
fun LoginScreen() {
    val container = LocalAppContainer.current
    val vm: LoginViewModel = viewModel(factory = vmFactory { LoginViewModel(container.authRepository) })
    val state by vm.state.collectAsStateWithLifecycle()

    AquaScreen(title = "Ingreso a faena", subtitle = "AquaChile · Operaciones marítimas") { padding ->
        ScreenColumn(padding) {
            SectionCard(title = "Selecciona tu rol operativo") {
                Rol.values().forEach { rol ->
                    OpcionCard(
                        titulo = rol.etiqueta,
                        detalle = rol.descripcion,
                        seleccionado = state.rol == rol,
                        onClick = { vm.onRolChange(rol) }
                    )
                }
            }
            SectionCard(title = "Credenciales en terreno") {
                AquaTextField(
                    valor = state.rut,
                    onValueChange = vm::onRutChange,
                    etiqueta = "RUT del operador",
                    error = state.errorRut,
                    ayuda = "Ej.: 12.345.678-5"
                )
                AquaTextField(
                    valor = state.pin,
                    onValueChange = vm::onPinChange,
                    etiqueta = "PIN rápido (4 dígitos)",
                    error = state.errorPin,
                    keyboardType = KeyboardType.NumberPassword,
                    visualTransformation = PasswordVisualTransformation()
                )
            }
            state.error?.let { AlertBanner("No se pudo ingresar", listOf(it), Tone.DANGER) }
            PrimaryButton(
                texto = "Ingresar a pre-chequeo",
                onClick = vm::ingresar,
                enabled = state.rut.isNotBlank() && state.pin.length == 4,
                cargando = state.cargando
            )
            if (BuildConfig.DEBUG) DatosDemostracion()
        }
    }
}

/** Ayuda solo para builds de depuración: usuarios ficticios sembrados en la base de datos. */
@Composable
private fun DatosDemostracion() {
    SectionCard(title = "Datos de demostración (solo debug)") {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SeedData.usuarios.forEach { demo ->
                Text(
                    text = "${demo.rol.etiqueta}\n${Rut.formatear(demo.rut)} · PIN ${demo.pin}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AquaOnSurfaceVariant
                )
            }
        }
    }
}
