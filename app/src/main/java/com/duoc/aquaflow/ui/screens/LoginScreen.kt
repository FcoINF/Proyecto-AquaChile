package com.duoc.aquaflow.ui.screens

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.duoc.aquaflow.R
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import com.duoc.aquaflow.viewmodel.IngresoUiState
import com.duoc.aquaflow.viewmodel.SesionViewModel

private const val PREFS_LOGIN = "aquaflow_login"
private const val PREF_RECORDAR = "recordar"
private const val PREF_USUARIO = "usuario"

private val FondoLogin = Color(0xFFF8F9FF)
private val AzulMarino = Color(0xFF192838)
private val AzulAccion = Color(0xFF0755B8)
private val AzulTile = Color(0xFF90BDF9)
private val AzulTileSuave = Color(0xFFE5EEFF)
private val TextoPrincipal = Color(0xFF0A0F17)
private val TextoSecundario = Color(0xFF6B7280)
private val TextoEtiqueta = Color(0xFF334155)
private val IconoGris = Color(0xFF64748B)
private val BordeCampo = Color(0xFFE2E8F0)
private val FondoCampo = Color(0xFFF8FAFC)
private val BordeTarjeta = Color(0xFFEEF2F7)
private val BordeDivisor = Color(0xFFE2E6EE)
private val PuntoGris = Color(0xFFC9CFD8)
private val RojoError = Color(0xFFB3261E)

private data class OpcionRol(
    val rol: Rol,
    val titulo: String,
    val subtitulo: String,
    val icono: Int
)

private val OPCIONES_ROL = listOf(
    OpcionRol(
        rol = Rol.SUPERVISOR,
        titulo = "Supervisor de Buceo",
        subtitulo = "Responsable AST, Cuadrilla, etc.",
        icono = R.drawable.ic_sports_score
    ),
    OpcionRol(
        rol = Rol.JEFE,
        titulo = "Jefe de Centro / Prevención",
        subtitulo = "Auditoría Portón, trazabilidad, etc.",
        icono = R.drawable.ic_shield_person
    )
)

@Composable
fun LoginScreen(sesion: SesionViewModel, onIngresoExitoso: () -> Unit) {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val statusAnterior = controller?.isAppearanceLightStatusBars
        val navegacionAnterior = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = true
        controller?.isAppearanceLightNavigationBars = true
        onDispose {
            if (statusAnterior != null) controller?.isAppearanceLightStatusBars = statusAnterior
            if (navegacionAnterior != null) controller?.isAppearanceLightNavigationBars = navegacionAnterior
        }
    }

    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences(PREFS_LOGIN, Context.MODE_PRIVATE)
    }
    var recordar by rememberSaveable {
        mutableStateOf(prefs.getBoolean(PREF_RECORDAR, false))
    }
    val ui by sesion.ingreso.collectAsState()

    LaunchedEffect(Unit) {
        if (recordar && ui.nombre.isBlank()) {
            prefs.getString(PREF_USUARIO, null)?.let(sesion::cambiarNombre)
        }
    }

    LoginContent(
        ui = ui,
        recordar = recordar,
        onNombre = sesion::cambiarNombre,
        onClave = sesion::cambiarClave,
        onRol = sesion::seleccionarRol,
        onRecordar = { activo ->
            recordar = activo
            prefs.edit().putBoolean(PREF_RECORDAR, activo).apply()
            // Solo se recuerda el usuario; el PIN nunca se guarda en el dispositivo.
            if (!activo) prefs.edit().remove(PREF_USUARIO).apply()
        },
        onIngresar = {
            if (recordar) {
                prefs.edit().putString(PREF_USUARIO, ui.nombre.trim()).apply()
            } else {
                prefs.edit().remove(PREF_USUARIO).apply()
            }
            if (sesion.ingresar()) onIngresoExitoso()
        }
    )
}

@Composable
private fun LoginContent(
    ui: IngresoUiState,
    recordar: Boolean,
    onNombre: (String) -> Unit,
    onClave: (String) -> Unit,
    onRol: (Rol) -> Unit,
    onRecordar: (Boolean) -> Unit,
    onIngresar: () -> Unit
) {
    val focus = LocalFocusManager.current
    val enviar = {
        focus.clearFocus()
        onIngresar()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoLogin)
            .safeDrawingPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(top = 40.dp, bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MarcaAquaChile(
                    modifier = Modifier
                        .padding(horizontal = 30.dp)
                        .fillMaxWidth()
                        .height(110.dp)
                )
                Spacer(Modifier.height(66.dp))
                SelectorRol(
                    seleccionado = ui.rol,
                    onRol = onRol,
                    modifier = Modifier
                        .padding(horizontal = 38.dp)
                        .fillMaxWidth()
                )
                Spacer(Modifier.height(32.dp))
                TarjetaCredenciales(
                    ui = ui,
                    recordar = recordar,
                    onNombre = onNombre,
                    onClave = onClave,
                    onRecordar = onRecordar,
                    onIngresar = enviar,
                    modifier = Modifier
                        .padding(horizontal = 38.dp)
                        .fillMaxWidth()
                )
                Spacer(Modifier.height(26.dp))
                TarjetaDemo(
                    modifier = Modifier
                        .padding(horizontal = 38.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun MarcaAquaChile(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AzulMarino),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.logo_aquachile),
            contentDescription = "AquaChile",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .height(66.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun SelectorRol(
    seleccionado: Rol,
    onRol: (Rol) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OPCIONES_ROL.forEach { opcion ->
            TarjetaRol(
                opcion = opcion,
                seleccionado = seleccionado == opcion.rol,
                onClick = { onRol(opcion.rol) }
            )
        }
    }
}

@Composable
private fun TarjetaRol(
    opcion: OpcionRol,
    seleccionado: Boolean,
    onClick: () -> Unit
) {
    val forma = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(if (seleccionado) 4.dp else 2.dp, forma)
            .clip(forma)
            .background(Color.White)
            .border(
                width = if (seleccionado) 2.dp else 1.dp,
                color = if (seleccionado) AzulAccion else BordeTarjeta,
                shape = forma
            )
            .selectable(
                selected = seleccionado,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (seleccionado) AzulTile else AzulTileSuave),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(opcion.icono),
                contentDescription = null,
                tint = if (seleccionado) AzulAccion else Color(0xFF475569),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = opcion.titulo,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoPrincipal
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = opcion.subtitulo,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = TextoSecundario
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .then(
                    if (seleccionado) {
                        Modifier.background(AzulAccion)
                    } else {
                        Modifier.border(1.5.dp, PuntoGris, CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (seleccionado) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
private fun TarjetaCredenciales(
    ui: IngresoUiState,
    recordar: Boolean,
    onNombre: (String) -> Unit,
    onClave: (String) -> Unit,
    onRecordar: (Boolean) -> Unit,
    onIngresar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focus = LocalFocusManager.current
    val forma = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .shadow(4.dp, forma)
            .clip(forma)
            .background(Color.White)
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "INGRESO DE CREDENCIALES",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.4.sp,
                color = TextoPrincipal
            )
        )
        Spacer(Modifier.height(11.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(BordeDivisor)
        )
        Spacer(Modifier.height(9.dp))
        CampoCredencial(
            valor = ui.nombre,
            onValorChange = onNombre,
            etiqueta = "RUT DEL OPERADOR",
            placeholder = "11.111.111-K",
            icono = painterResource(R.drawable.ic_id_card),
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next,
            onImeAction = { focus.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
        )
        Spacer(Modifier.height(18.dp))
        CampoCredencial(
            valor = ui.clave,
            onValorChange = onClave,
            etiqueta = "PIN DE ACCESO",
            placeholder = "*****",
            icono = painterResource(R.drawable.ic_lock),
            esPin = true,
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done,
            onImeAction = onIngresar
        )
        ui.error?.let { mensaje ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = mensaje,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = RojoError
                )
            )
        }
        Spacer(Modifier.height(12.dp))
        FilaRecordar(
            recordar = recordar,
            onRecordar = onRecordar
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onIngresar,
            modifier = Modifier
                .width(168.dp)
                .height(40.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = AzulAccion,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "INGRESAR",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 11.sp,
                    letterSpacing = 0.6.sp
                ),
                maxLines = 1
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_arrow_forward),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun CampoCredencial(
    valor: String,
    onValorChange: (String) -> Unit,
    etiqueta: String,
    placeholder: String,
    icono: Painter,
    modifier: Modifier = Modifier,
    esPin: Boolean = false,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    onImeAction: () -> Unit
) {
    val forma = RoundedCornerShape(8.dp)
    val estiloTexto = MaterialTheme.typography.bodyLarge.copy(
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Bold,
        color = TextoPrincipal
    )
    Column(modifier = modifier) {
        Text(
            text = etiqueta,
            modifier = Modifier.padding(start = 2.dp, bottom = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.7.sp,
                color = TextoEtiqueta
            )
        )
        BasicTextField(
            value = valor,
            onValueChange = onValorChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(forma)
                .background(FondoCampo)
                .border(1.dp, BordeCampo, forma)
                .padding(horizontal = 12.dp)
                .semantics { contentDescription = etiqueta },
            textStyle = estiloTexto,
            singleLine = true,
            cursorBrush = SolidColor(AzulAccion),
            visualTransformation = if (esPin) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onNext = { onImeAction() },
                onDone = { onImeAction() }
            ),
            decorationBox = { campoInterno ->
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = icono,
                        contentDescription = null,
                        tint = IconoGris,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.width(24.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (valor.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = estiloTexto.copy(
                                    letterSpacing = if (esPin) (-0.5).sp else 0.sp
                                )
                            )
                        }
                        campoInterno()
                    }
                }
            }
        )
    }
}

@Composable
private fun FilaRecordar(
    recordar: Boolean,
    onRecordar: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .toggleable(
                value = recordar,
                onValueChange = onRecordar,
                role = Role.Checkbox
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(19.dp)
                .clip(CircleShape)
                .then(
                    if (recordar) {
                        Modifier.background(AzulAccion)
                    } else {
                        Modifier.border(1.5.dp, PuntoGris, CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (recordar) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Recordar credenciales",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = TextoSecundario
            )
        )
    }
}

@Composable
private fun TarjetaDemo(modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .shadow(4.dp, forma)
            .clip(forma)
            .background(Color.White)
            .padding(vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ACCESO DEMO",
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 14.sp,
                lineHeight = 15.sp,
                letterSpacing = 0.3.sp,
                color = AzulAccion
            )
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "SUPERVISOR: SUPERVISOR\nADMINISTRADOR: JEFE\nPIN: 1234",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                lineHeight = 15.sp,
                letterSpacing = 0.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipal
            )
        )
    }
}

@Preview(name = "Login compacto", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun LoginScreenPreview() {
    AquaFlowTheme {
        LoginContent(
            ui = IngresoUiState(rol = Rol.SUPERVISOR),
            recordar = false,
            onNombre = {},
            onClave = {},
            onRol = {},
            onRecordar = {},
            onIngresar = {}
        )
    }
}

@Preview(name = "Login administrador", showBackground = true, widthDp = 320, heightDp = 640)
@Composable
private fun LoginAdministradorPreview() {
    AquaFlowTheme {
        LoginContent(
            ui = IngresoUiState(rol = Rol.JEFE),
            recordar = true,
            onNombre = {},
            onClave = {},
            onRol = {},
            onRecordar = {},
            onIngresar = {}
        )
    }
}
