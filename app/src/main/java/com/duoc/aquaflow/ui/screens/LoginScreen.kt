package com.duoc.aquaflow.ui.screens

import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import com.duoc.aquaflow.ui.theme.AquaNaranja
import com.duoc.aquaflow.viewmodel.IngresoUiState
import com.duoc.aquaflow.viewmodel.SesionViewModel

private val FondoIngreso = Color(0xFF263944)
private val PanelIngreso = Color(0xFF242424)
private val GrisPerfil = Color(0xFF898989)

@Composable
fun LoginScreen(sesion: SesionViewModel, onIngresoExitoso: () -> Unit) {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val statusAnterior = controller?.isAppearanceLightStatusBars
        val navegacionAnterior = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            if (statusAnterior != null) controller?.isAppearanceLightStatusBars = statusAnterior
            if (navegacionAnterior != null) controller?.isAppearanceLightNavigationBars = navegacionAnterior
        }
    }
    val ui by sesion.ingreso.collectAsState()
    LoginContent(ui, sesion::cambiarNombre, sesion::cambiarClave, sesion::seleccionarRol) {
        if (sesion.ingresar()) onIngresoExitoso()
    }
}

@Composable
private fun LoginContent(
    ui: IngresoUiState,
    onNombre: (String) -> Unit,
    onClave: (String) -> Unit,
    onRol: (Rol) -> Unit,
    onIngresar: () -> Unit
) {
    val focus = LocalFocusManager.current
    val enviar = { focus.clearFocus(); onIngresar() }
    Column(
        modifier = Modifier.fillMaxSize().background(FondoIngreso)
            .safeDrawingPadding().imePadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MarcaAquaChile()
        Spacer(Modifier.height(56.dp))
        Surface(
            modifier = Modifier.widthIn(max = 380.dp).fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            color = PanelIngreso,
            contentColor = Color.White,
            shadowElevation = 5.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Perfil Operativo de Turno", style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50))
                        .background(GrisPerfil).selectableGroup()
                ) {
                    listOf(Rol.JEFE, Rol.SUPERVISOR).forEach { rol ->
                        Box(
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (ui.rol == rol) AquaNaranja else GrisPerfil)
                                .selectable(selected = ui.rol == rol, role = Role.RadioButton,
                                    onClick = { onRol(rol) }).padding(horizontal = 4.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(rol.texto.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center, color = Color.White)
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
                EtiquetaIngreso("Usuario / identificación")
                OutlinedTextField(
                    value = ui.nombre, onValueChange = onNombre,
                    placeholder = { Text(if (ui.rol == Rol.JEFE) "jefe" else "supervisor") },
                    leadingIcon = { Icon(Icons.Outlined.Badge, contentDescription = null) },
                    singleLine = true, shape = RoundedCornerShape(12.dp),
                    colors = coloresCampoIngreso(), modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = {
                        focus.moveFocus(androidx.compose.ui.focus.FocusDirection.Down)
                    })
                )
                Spacer(Modifier.height(20.dp))
                EtiquetaIngreso("Contraseña")
                OutlinedTextField(
                    value = ui.clave, onValueChange = onClave,
                    placeholder = { Text("••••••••••") },
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(), singleLine = true,
                    shape = RoundedCornerShape(12.dp), colors = coloresCampoIngreso(),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { enviar() })
                )
                ui.error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = Color(0xFFFFB4AB), style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite })
                }
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = enviar, modifier = Modifier.fillMaxWidth(0.9f).heightIn(min = 52.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = AquaNaranja, contentColor = Color.White)
                ) { Text("INICIAR SESIÓN", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("AquaFlow · Acceso de demostración\nSupervisor: supervisor · Administrador: jefe\nContraseña: 1234",
            color = Color(0xFFCED9DF), style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center)
    }
}

@Composable
private fun EtiquetaIngreso(texto: String) {
    Text(texto, modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 6.dp),
        style = MaterialTheme.typography.labelMedium)
}

@Composable
private fun coloresCampoIngreso() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
    focusedTextColor = PanelIngreso, unfocusedTextColor = PanelIngreso,
    focusedBorderColor = AquaNaranja, unfocusedBorderColor = Color(0xFFD7DDE1),
    focusedLeadingIconColor = Color(0xFF666666), unfocusedLeadingIconColor = Color(0xFF666666),
    focusedPlaceholderColor = Color(0xFF747474), unfocusedPlaceholderColor = Color(0xFF747474),
    cursorColor = PanelIngreso
)

@Composable
private fun MarcaAquaChile() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("AquaChile", color = Color.White, fontFamily = FontFamily.Serif,
            fontSize = 30.sp, letterSpacing = 1.sp)
        // Símbolo geométrico inspirado en la referencia; no requiere conexión.
        Canvas(Modifier.size(46.dp, 48.dp)) {
            for (columna in 0..4) {
                val cantidad = 5 - columna
                val pasoY = size.height / 5
                val x = columna * size.width / 5
                for (fila in 0 until cantidad) {
                    val y = (fila + columna / 2f) * pasoY
                    val path = Path().apply {
                        moveTo(x, y)
                        lineTo(x + size.width / 5 - 2.dp.toPx(), y + pasoY / 2)
                        lineTo(x, y + pasoY - 2.dp.toPx())
                        close()
                    }
                    drawPath(path, Color.White)
                }
            }
        }
    }
}

@Preview(name = "Login compacto", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun LoginScreenPreview() {
    AquaFlowTheme { LoginContent(IngresoUiState(), {}, {}, {}, {}) }
}

@Preview(name = "Login supervisor", showBackground = true, widthDp = 320, heightDp = 640)
@Composable
private fun LoginSupervisorPreview() {
    AquaFlowTheme { LoginContent(IngresoUiState(rol = Rol.SUPERVISOR), {}, {}, {}, {}) }
}
