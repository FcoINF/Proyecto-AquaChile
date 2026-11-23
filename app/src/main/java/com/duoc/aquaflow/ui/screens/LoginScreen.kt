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
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.duoc.aquaflow.BuildConfig
import com.duoc.aquaflow.R
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import com.duoc.aquaflow.viewmodel.IngresoUiState
import com.duoc.aquaflow.viewmodel.SesionViewModel

private const val PREFS_LOGIN = "aquaflow_login"
private val PREF_RECORDAR = booleanPreferencesKey("recordar")
private val PREF_USUARIO = stringPreferencesKey("usuario")
private val Context.preferenciasIngreso by preferencesDataStore(
    name = PREFS_LOGIN,
    produceMigrations = { listOf(SharedPreferencesMigration(it, PREFS_LOGIN, setOf("recordar", "usuario"))) }
)

private val FondoLogin = Color(0xFFF8F9FF)
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
    val prefs = context.preferenciasIngreso
    val alcance = rememberCoroutineScope()
    var recordar by rememberSaveable { mutableStateOf(false) }
    val ui by sesion.ingreso.collectAsState()

    LaunchedEffect(prefs) {
        val guardadas = try { prefs.data.first() } catch (_: java.io.IOException) { return@LaunchedEffect }
        recordar = guardadas[PREF_RECORDAR] ?: false
        if (recordar && sesion.ingreso.value.nombre.isBlank()) {
            guardadas[PREF_USUARIO]?.let(sesion::cambiarNombre)
        }
    }

    LoginContent(
        ui = ui,
        recordar = recordar,
        accesoCloud = sesion.accesoCloud,
        onNombre = sesion::cambiarNombre,
        onEditarClave = sesion::limpiarError,
        onRol = sesion::seleccionarRol,
        onRecordar = { activo ->
            recordar = activo
            alcance.launch {
                try {
                    prefs.edit {
                        it[PREF_RECORDAR] = activo
                        if (!activo) it.remove(PREF_USUARIO)
                    }
                } catch (_: java.io.IOException) { }
            }
        },
        onIngresar = { clave ->
            alcance.launch {
                try {
                    prefs.edit {
                        it[PREF_RECORDAR] = recordar
                        if (recordar) it[PREF_USUARIO] = ui.nombre.trim() else it.remove(PREF_USUARIO)
                    }
                } catch (_: java.io.IOException) { }
                sesion.cambiarClave(clave)
                sesion.ingresar(onIngresoExitoso)
            }
        }
    )
}

@Composable
private fun LoginContent(
    ui: IngresoUiState,
    recordar: Boolean,
    accesoCloud: Boolean = false,
    onNombre: (String) -> Unit,
    onEditarClave: () -> Unit,
    onRol: (Rol) -> Unit,
    onRecordar: (Boolean) -> Unit,
    onIngresar: (String) -> Unit
) {
    val focus = LocalFocusManager.current
    val clave = remember { TextFieldState() }
    LaunchedEffect(clave, onEditarClave) {
        snapshotFlow { clave.text }.drop(1).collect { onEditarClave() }
    }
    val enviar = {
        val claveAlEnviar = clave.text.toString()
        focus.clearFocus()
        onIngresar(claveAlEnviar)
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
                    accesoCloud = accesoCloud,
                    onNombre = onNombre,
                    clave = clave,
                    onRecordar = onRecordar,
                    onIngresar = enviar,
                    modifier = Modifier
                        .padding(horizontal = 38.dp)
                        .fillMaxWidth()
                )
                Spacer(Modifier.height(26.dp))
                TarjetaAcceso(
                    accesoCloud = accesoCloud,
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
            .background(Color(0xFF192838)),
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
    accesoCloud: Boolean,
    onNombre: (String) -> Unit,
    clave: TextFieldState,
    onRecordar: (Boolean) -> Unit,
    onIngresar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focus = LocalFocusManager.current
    var mostrarClave by remember { mutableStateOf(false) }
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
        MarcoCredencial("USUARIO DEL OPERADOR") { campo, estilo ->
            BasicTextField(
                value = ui.nombre,
                onValueChange = onNombre,
                modifier = campo,
                enabled = !ui.ocupado,
                textStyle = estilo,
                singleLine = true,
                cursorBrush = SolidColor(AzulAccion),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focus.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                ),
                decorationBox = { editor ->
                    ContenidoCredencial(
                        painterResource(R.drawable.ic_id_card), "Rol", ui.nombre.isEmpty(), estilo, editor
                    )
                }
            )
        }
        Spacer(Modifier.height(18.dp))
        MarcoCredencial(if (accesoCloud) "CONTRASEÑA" else "PIN DE ACCESO") { campo, estilo ->
            BasicSecureTextField(
                state = clave,
                modifier = campo,
                enabled = !ui.ocupado,
                textStyle = estilo,
                cursorBrush = SolidColor(AzulAccion),
                textObfuscationMode = if (mostrarClave) TextObfuscationMode.Visible else TextObfuscationMode.Hidden,
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    keyboardType = if (accesoCloud) KeyboardType.Password else KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                onKeyboardAction = { onIngresar() },
                decorator = { editor ->
                    ContenidoCredencial(
                        painterResource(R.drawable.ic_lock), "Contraseña", clave.text.isEmpty(), estilo, editor
                    ) {
                        IconButton(
                            onClick = { mostrarClave = !mostrarClave },
                            enabled = !ui.ocupado,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (mostrarClave) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (mostrarClave) "Ocultar contraseña" else "Mostrar contraseña",
                                tint = IconoGris
                            )
                        }
                    }
                }
            )
        }
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
            enabled = !ui.ocupado,
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
                text = if (ui.ocupado) "CONECTANDO" else "INGRESAR",
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
private fun MarcoCredencial(
    etiqueta: String,
    contenido: @Composable (Modifier, TextStyle) -> Unit
) {
    val forma = RoundedCornerShape(8.dp)
    val estiloTexto = MaterialTheme.typography.bodyLarge.copy(
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Bold,
        color = TextoPrincipal
    )
    Column {
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
        contenido(
            Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(forma)
                .background(FondoCampo)
                .border(1.dp, BordeCampo, forma)
                .padding(horizontal = 12.dp)
                .semantics { contentDescription = etiqueta },
            estiloTexto
        )
    }
}

@Composable
private fun ContenidoCredencial(
    icono: Painter,
    placeholder: String,
    vacio: Boolean,
    estilo: TextStyle,
    editor: @Composable () -> Unit,
    accion: @Composable () -> Unit = {}
) {
    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, tint = IconoGris, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(24.dp))
        Box(Modifier.weight(1f)) {
            editor()
            if (vacio) {
                Text(
                    text = placeholder,
                    style = estilo.copy(
                        color = TextoSecundario,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.sp
                    )
                )
            }
        }
        accion()
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
private fun TarjetaAcceso(accesoCloud: Boolean, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(14.dp)
    val claveDePrueba = BuildConfig.LOGIN_HINT_PASSWORD
    Column(
        modifier = modifier
            .shadow(4.dp, forma)
            .clip(forma)
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (accesoCloud) "USUARIOS POR PERFIL" else "ACCESO DEMO",
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 14.sp,
                lineHeight = 15.sp,
                letterSpacing = 0.3.sp,
                color = AzulAccion
            )
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = when {
                !accesoCloud -> "SUPERVISOR: SUPERVISOR\nADMINISTRADOR: JEFE\nPIN: 1234"
                claveDePrueba.isNotEmpty() -> "supervisor = $claveDePrueba\njefe = $claveDePrueba"
                else -> "Supervisor de Buceo · supervisor\nJefe de Centro / Prevención · jefe"
            },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                lineHeight = 15.sp,
                letterSpacing = 0.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipal
            )
        )
        if (accesoCloud && claveDePrueba.isEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Usa la contraseña asignada a tu cuenta.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall.copy(color = TextoSecundario)
            )
        }
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
            onEditarClave = {},
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
            onEditarClave = {},
            onRol = {},
            onRecordar = {},
            onIngresar = {}
        )
    }
}
