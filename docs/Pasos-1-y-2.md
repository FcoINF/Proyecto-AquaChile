# AquaFlow: pasos 1 y 2 — código completo
Base: carpeta actual del proyecto, más avanzada que app.zip. Estos archivos conservan las integraciones existentes de AST, resumen e historial. No son un parche independiente para el ZIP antiguo; para esa versión usa el paquete completo de fuentes adjunto.
Paso 1: tema fijo naranja/azul/blanco, Inter local y nombre AquaFlow. JEFE conserva su identificador interno y muestra Administrador. Fuentes: app/src/main/res/font/inter_regular.ttf e inter_bold.ttf.
Paso 2: apertura con StateFlow, validaciones en Reglas y ViewModel compartido por el grafo faena. El flujo actual es Apertura → Salud → AST → Resumen. El checklist aún no está conectado.
Corrección de compatibilidad: se eliminó el NivelSalud duplicado de Enums.kt; la definición válida está en EncuestaSalud.kt.

## app/src/main/java/com/duoc/aquaflow/ui/theme/Color.kt

```kotlin
package com.duoc.aquaflow.ui.theme

import androidx.compose.ui.graphics.Color

val AquaNaranja = Color(0xFFF95716)
val AquaAzul = Color(0xFF0B1723)
val AquaBlanco = Color(0xFFFFFFFF)
val AquaNaranjaSuave = Color(0xFFFFDBCC)
val AquaGrisSuave = Color(0xFFF1F4F6)

```

## app/src/main/java/com/duoc/aquaflow/ui/theme/Theme.kt

```kotlin
package com.duoc.aquaflow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta fija del MVP: no depende del fondo de pantalla del teléfono.
private val AquaFlowColors = lightColorScheme(
    primary = AquaNaranja,
    onPrimary = AquaAzul,
    primaryContainer = AquaNaranjaSuave,
    onPrimaryContainer = AquaAzul,
    secondary = AquaAzul,
    onSecondary = AquaBlanco,
    secondaryContainer = AquaGrisSuave,
    onSecondaryContainer = AquaAzul,
    tertiary = AquaAzul,
    onTertiary = AquaBlanco,
    tertiaryContainer = AquaGrisSuave,
    onTertiaryContainer = AquaAzul,
    background = AquaBlanco,
    onBackground = AquaAzul,
    surface = AquaBlanco,
    onSurface = AquaAzul,
    surfaceVariant = AquaGrisSuave,
    onSurfaceVariant = Color(0xFF43515C),
    surfaceTint = AquaNaranja,
    outline = Color(0xFF697680),
    outlineVariant = Color(0xFFCAD1D6),
    error = Color(0xFFBA1A1A),
    onError = AquaBlanco,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = AquaAzul,
    inverseOnSurface = AquaBlanco,
    inversePrimary = AquaNaranjaSuave
)

@Composable
fun AquaFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AquaFlowColors, typography = Typography) {
        Surface(color = MaterialTheme.colorScheme.background, content = content)
    }
}

```

## app/src/main/java/com/duoc/aquaflow/ui/theme/Type.kt

```kotlin
package com.duoc.aquaflow.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.duoc.aquaflow.R

// Fuentes empaquetadas: disponibles también sin conexión.
private val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_bold, FontWeight.Bold)
)
private val base = Typography()

val Typography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
    displayMedium = base.displayMedium.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
    displaySmall = base.displaySmall.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
    headlineLarge = base.headlineLarge.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
    titleSmall = base.titleSmall.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
    bodyLarge = base.bodyLarge.copy(fontFamily = Inter),
    bodyMedium = base.bodyMedium.copy(fontFamily = Inter),
    bodySmall = base.bodySmall.copy(fontFamily = Inter),
    labelLarge = base.labelLarge.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
    labelMedium = base.labelMedium.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
    labelSmall = base.labelSmall.copy(fontFamily = Inter, fontWeight = FontWeight.Bold)
)

```

## app/src/main/java/com/duoc/aquaflow/model/Usuario.kt

```kotlin
package com.duoc.aquaflow.model

// Conservamos JEFE como identificador hasta que el equipo acuerde el nombre interno.
enum class Rol(val texto: String) {
    SUPERVISOR("Supervisor"),
    JEFE("Administrador")
}

data class Usuario(
    val nombre: String,
    val clave: String,
    val rol: Rol
)

```

## app/src/main/java/com/duoc/aquaflow/ui/screens/LoginScreen.kt

```kotlin
package com.duoc.aquaflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.viewmodel.SesionViewModel

@Composable
fun LoginScreen(sesion: SesionViewModel, onIngresoExitoso: () -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = "AquaFlow",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Demo: supervisor / jefe (Administrador). Clave: 1234",
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = nombre,
            onValueChange = {
                nombre = it
                error = false
            },
            label = { Text("Usuario") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = clave,
            onValueChange = {
                clave = it
                error = false
            },
            label = { Text("Clave") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (error) {
            Text(
                text = "Usuario o clave incorrectos",
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = {
                if (sesion.ingresar(nombre, clave)) {
                    onIngresoExitoso()
                } else {
                    error = true
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ingresar")
        }
    }
}

```

## app/src/main/java/com/duoc/aquaflow/ui/screens/DashboardScreen.kt

```kotlin
package com.duoc.aquaflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.ui.navigation.MainBottomBar
import com.duoc.aquaflow.ui.navigation.Tab

@Composable
fun DashboardScreen(
    usuario: Usuario,
    tabs: List<Tab>,
    ultimaEncuesta: EncuestaSalud?,
    onNuevaFaena: () -> Unit,
    onTab: (Tab) -> Unit,
    onSalir: () -> Unit
) {
    val puedeCrear = usuario.rol == Rol.SUPERVISOR
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { MainBottomBar(Tab.INICIO, tabs, onTab) },
        floatingActionButton = {
            if (puedeCrear) {
                ExtendedFloatingActionButton(onClick = onNuevaFaena) { Text("Nueva faena") }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("AquaFlow", style = MaterialTheme.typography.headlineMedium)
            Text("Bienvenido: ${usuario.nombre}")
            Text("Rol activo: ${usuario.rol.texto}")
            Text(
                if (puedeCrear) "Inicia una faena para registrar su apertura."
                else "La revisión y el historial se incorporarán en una próxima etapa."
            )
            Button(onClick = onSalir) { Text("Cerrar sesión") }
        }
    }
}

```

## app/src/main/java/com/duoc/aquaflow/model/Enums.kt

```kotlin
package com.duoc.aquaflow.model

enum class Estado(val texto:String){
    PENDIENTE("Pendiente"),
    CUMPLE("Cumple"),
    NO_CUMPLE("No cumple"),
    NO_APLICA("No aplica"),
    OBSERVADO("Observado")
}

enum class Resultado(val texto: String){
    CUMPLE("Cumple"),
    OBSERVADO("Observado"),
    REQUIERE_REVISIONES("Requiere revisión")
}


```

## app/src/main/java/com/duoc/aquaflow/model/EncuestaSalud.kt

```kotlin
package com.duoc.aquaflow.model

// Nivel de los parámetros de salud (ficticios): alimenta el semáforo.
enum class NivelSalud(val texto: String) {
    NORMAL("Normal"),
    ATENCION("Atención"),
    CRITICO("Crítico")
}

// Encuesta preventiva: responder "Sí" a cualquiera genera una alerta.
val PREGUNTAS_SALUD = listOf(
    "¿Durmió menos de 6 horas?",
    "¿Presenta resfrío o dolor de oídos?",
    "¿Consumió alcohol en las últimas 24 horas?"
)

// Encuesta ya validada. Los nombres de campo coinciden con los que la guía agrega a PreChequeo
// (sistolica, diastolica, saturacion, alertasEncuesta), así luego se copian sin traducir.
data class EncuestaSalud(
    val sistolica: Int,
    val diastolica: Int,
    val saturacion: Int,
    val alertasEncuesta: List<String>,
    val nivel: NivelSalud
)
```

## app/src/main/java/com/duoc/aquaflow/model/PreChequeo.kt

```kotlin
package com.duoc.aquaflow.model

data class PreChequeo(
    val id: String = "",
    val centroCultivo: String = "",
    val fecha: String = "",
    val hora: String = "",
    val buzoAsignado: String = "",
    val supervisorACargo: String = ""
)

```

## app/src/main/java/com/duoc/aquaflow/domain/Reglas.kt

```kotlin
package com.duoc.aquaflow.domain

import com.duoc.aquaflow.model.NivelSalud
import com.duoc.aquaflow.model.PreChequeo
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale

// Reglas de negocio puras (sin Android): fáciles de leer y de probar.
// Los umbrales son ficticios, solo para el ejercicio académico.
object Reglas {

    // Rangos aceptados al digitar: fuera de ellos se asume error de tipeo
    val RANGO_SISTOLICA = 70..250
    val RANGO_DIASTOLICA = 40..150
    val RANGO_SATURACION = 50..100

    // El primer `when` que se cumpla gana, por eso CRITICO va antes que ATENCION
    fun nivelSalud(sistolica: Int, diastolica: Int, saturacion: Int, alertas: Int) = when {
        saturacion < 92 || sistolica >= 160 || diastolica >= 100 -> NivelSalud.CRITICO
        saturacion < 95 || sistolica >= 140 || diastolica >= 90 || alertas > 0 -> NivelSalud.ATENCION
        else -> NivelSalud.NORMAL
    }

    fun enRango(sistolica: Int, diastolica: Int, saturacion: Int) =
        sistolica in RANGO_SISTOLICA && diastolica in RANGO_DIASTOLICA && saturacion in RANGO_SATURACION


    // Devuelve el primer problema de la apertura; null significa válida.
    fun errorApertura(apertura: PreChequeo): String? = when {
        apertura.centroCultivo.isBlank() -> "Ingresa el centro de cultivo"
        !fechaValida(apertura.fecha) -> "Ingresa una fecha válida con formato dd/MM/aaaa"
        !Regex("([01][0-9]|2[0-3]):[0-5][0-9]").matches(apertura.hora) ->
            "Ingresa una hora válida con formato HH:mm"
        apertura.buzoAsignado.isBlank() -> "Ingresa el nombre del buzo"
        apertura.supervisorACargo.isBlank() -> "Falta el supervisor a cargo"
        else -> null
    }

    private fun fechaValida(texto: String): Boolean {
        if (!Regex("[0-9]{2}/[0-9]{2}/[0-9]{4}").matches(texto)) return false
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).apply {
            isLenient = false
        }
        val posicion = ParsePosition(0)
        return formato.parse(texto, posicion) != null && posicion.index == texto.length
    }
}

```

## app/src/main/java/com/duoc/aquaflow/viewmodel/FaenaViewModel.kt

```kotlin
package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.ViewModel
import com.duoc.aquaflow.domain.Reglas
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.FormularioAst
import com.duoc.aquaflow.model.PreChequeo
import com.duoc.aquaflow.model.RegistroDecision
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.Usuario
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class FaenaUiState(
    val apertura: PreChequeo = PreChequeo(),
    val aperturaConfirmada: Boolean = false,
    val ast: FormularioAst? = null,
    val encuesta: EncuestaSalud? = null,
    val decision: RegistroDecision? = null,
    val error: String? = null,
)

// Borrador compartido por las pantallas del grafo "faena".
class FaenaViewModel : ViewModel() {
    private val _ui = MutableStateFlow(FaenaUiState())
    val ui: StateFlow<FaenaUiState> = _ui.asStateFlow()

    fun iniciar(usuario: Usuario) {
        if (usuario.rol != Rol.SUPERVISOR || _ui.value.apertura.id.isNotEmpty()) return
        val ahora = Date()
        _ui.value = FaenaUiState(
            apertura = PreChequeo(
                id = UUID.randomUUID().toString(),
                fecha = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).format(ahora),
                hora = SimpleDateFormat("HH:mm", Locale.ROOT).format(ahora),
                supervisorACargo = usuario.nombre
            )
        )
    }

    fun actualizar(apertura: PreChequeo) = _ui.update { actual ->
        // La identidad y el supervisor provienen de la sesión, no del formulario.
        val nueva = apertura.copy(
            id = actual.apertura.id,
            supervisorACargo = actual.apertura.supervisorACargo
        )
        if (nueva == actual.apertura) actual else actual.copy(
            apertura = nueva,
            aperturaConfirmada = false,
            encuesta = null,
            ast = null,
            decision = null,
            error = null
        )
    }

    fun confirmarApertura(): Boolean {
        val actual = _ui.value.apertura
        val limpia = actual.copy(
            centroCultivo = actual.centroCultivo.trim(),
            fecha = actual.fecha.trim(),
            hora = actual.hora.trim(),
            buzoAsignado = actual.buzoAsignado.trim(),
            supervisorACargo = actual.supervisorACargo.trim()
        )
        val error = Reglas.errorApertura(limpia)
        _ui.update {
            it.copy(apertura = limpia, aperturaConfirmada = error == null, error = error)
        }
        return error == null
    }

    fun registrarEncuesta(encuesta: EncuestaSalud): Boolean {
        if (!_ui.value.aperturaConfirmada) return false
        _ui.update { it.copy(encuesta = encuesta, decision = null) }
        return true
    }

    fun registrarAst(ast: FormularioAst): Boolean {
        if (!_ui.value.aperturaConfirmada) return false
        _ui.update { it.copy(ast = ast, decision = null) }
        return true
    }

    fun registrarDecision(decision: RegistroDecision): Boolean {
        if (!_ui.value.aperturaConfirmada) return false
        _ui.update { it.copy(decision = decision) }
        return true
    }
}
```

## app/src/main/java/com/duoc/aquaflow/ui/screens/AperturaFaenaScreen.kt

```kotlin
package com.duoc.aquaflow.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.model.PreChequeo
import com.duoc.aquaflow.ui.components.SemaforoSalud
import com.duoc.aquaflow.viewmodel.FaenaUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AperturaFaenaScreen(
    ui: FaenaUiState,
    onCambiar: (PreChequeo) -> Unit,
    onContinuar: () -> Unit,
    onSalir: () -> Unit
) {
    var confirmarSalida by rememberSaveable { mutableStateOf(false) }
    BackHandler { confirmarSalida = true }
    val apertura = ui.apertura

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("¿Descartar esta faena?") },
            text = { Text("El borrador todavía no se guarda en el dispositivo.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarSalida = false
                    onSalir()
                }) { Text("Descartar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarSalida = false }) { Text("Seguir editando") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Apertura de faena") },
                navigationIcon = {
                    TextButton(onClick = { confirmarSalida = true }) { Text("Volver") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).imePadding()
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Datos de la faena", style = MaterialTheme.typography.titleLarge)
            Text("Todos los campos son obligatorios.")
            OutlinedTextField(
                value = apertura.centroCultivo,
                onValueChange = { onCambiar(apertura.copy(centroCultivo = it)) },
                label = { Text("Centro de cultivo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = apertura.fecha,
                onValueChange = { onCambiar(apertura.copy(fecha = it)) },
                label = { Text("Fecha (dd/MM/aaaa)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = apertura.hora,
                onValueChange = { onCambiar(apertura.copy(hora = it)) },
                label = { Text("Hora (HH:mm)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = apertura.buzoAsignado,
                onValueChange = { onCambiar(apertura.copy(buzoAsignado = it)) },
                label = { Text("Buzo asignado") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = apertura.supervisorACargo,
                onValueChange = {},
                readOnly = true,
                label = { Text("Supervisor a cargo") },
                supportingText = { Text("Corresponde al usuario que inició sesión.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            ui.encuesta?.let {
                Text("Encuesta registrada en este borrador.")
                SemaforoSalud(it.nivel)
                Text("Si modificas la apertura, tendrás que completar la encuesta otra vez.")
            }
            Text(
                "Avance académico: por ahora se conecta con Salud. " +
                    "Checklist y AST se integrarán en los siguientes pasos.",
                style = MaterialTheme.typography.bodySmall
            )
            Button(onClick = onContinuar, modifier = Modifier.fillMaxWidth()) {
                Text(if (ui.encuesta == null) "Continuar a Salud" else "Revisar encuesta")
            }
        }
    }
}

```

## app/src/main/java/com/duoc/aquaflow/navigation/Ruta.kt

```kotlin
package com.duoc.aquaflow.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.ResumenFaena
import com.duoc.aquaflow.ui.navigation.Tab
import com.duoc.aquaflow.ui.navigation.tabsPara
import com.duoc.aquaflow.ui.screens.AperturaFaenaScreen
import com.duoc.aquaflow.ui.screens.AstScreen
import com.duoc.aquaflow.ui.screens.DashboardScreen
import com.duoc.aquaflow.ui.screens.EncuestaSaludScreen
import com.duoc.aquaflow.ui.screens.LoginScreen
import com.duoc.aquaflow.ui.screens.ResumenFaenaScreen
import com.duoc.aquaflow.ui.screens.historial.HistorialScreen
import com.duoc.aquaflow.viewmodel.FaenaViewModel
import com.duoc.aquaflow.viewmodel.SesionViewModel

object Ruta {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val HISTORIAL = "historial"
    const val FAENA = "faena"
    const val APERTURA = "faena/apertura"
    const val SALUD = "faena/salud"
    const val AST = "faena/ast"
    const val RESUMEN = "faena/resumen"
}

@Composable
fun AquaFlowNavHost() {
    val nav = rememberNavController()
    val sesion: SesionViewModel = viewModel()
    val usuario by sesion.usuario.collectAsState()
    var ultimaEncuesta by remember { mutableStateOf<EncuestaSalud?>(null) }

    // Si Android restaura las rutas sin una sesión, volvemos al ingreso.
    LaunchedEffect(usuario) {
        if (usuario == null) {
            nav.navigate(Ruta.LOGIN) {
                popUpTo(nav.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    fun irATab(tab: Tab) {
        nav.navigate(tab.ruta) {
            popUpTo(Ruta.DASHBOARD) { inclusive = false }
            launchSingleTop = true
        }
    }

    NavHost(navController = nav, startDestination = Ruta.LOGIN) {
        composable(Ruta.LOGIN) {
            LoginScreen(
                sesion = sesion,
                onIngresoExitoso = {
                    nav.navigate(Ruta.DASHBOARD) {
                        popUpTo(Ruta.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Ruta.DASHBOARD) {
            usuario?.let { actual ->
                DashboardScreen(
                    usuario = actual,
                    tabs = tabsPara(actual.rol),
                    ultimaEncuesta = ultimaEncuesta,
                    onNuevaFaena = { nav.navigate(Ruta.FAENA) },
                    onTab = ::irATab,
                    onSalir = {
                        sesion.salir()
                        ultimaEncuesta = null
                        nav.navigate(Ruta.LOGIN) {
                            popUpTo(nav.graph.id) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(Ruta.HISTORIAL) {
            usuario?.let { actual ->
                HistorialScreen(usuario = actual, tabs = tabsPara(actual.rol), onTab = ::irATab)
            }
        }

        navigation(startDestination = Ruta.APERTURA, route = Ruta.FAENA) {
            composable(Ruta.APERTURA) { entrada ->
                val padre = remember(entrada) { nav.getBackStackEntry(Ruta.FAENA) }
                val faena: FaenaViewModel = viewModel(viewModelStoreOwner = padre)
                val ui by faena.ui.collectAsState()
                val actual = usuario

                LaunchedEffect(actual) {
                    actual?.let { faena.iniciar(it) }
                }

                AperturaFaenaScreen(
                    ui = ui,
                    onCambiar = faena::actualizar,
                    onContinuar = {
                        if (faena.confirmarApertura()) {
                            nav.navigate(Ruta.SALUD)
                        }
                    },
                    onSalir = {
                        nav.popBackStack(Ruta.DASHBOARD, inclusive = false)
                    }
                )
            }

            composable(Ruta.SALUD) { entrada ->
                val padre = remember(entrada) { nav.getBackStackEntry(Ruta.FAENA) }
                val faena: FaenaViewModel = viewModel(viewModelStoreOwner = padre)
                val ui by faena.ui.collectAsState()

                EncuestaSaludScreen(
                    vm = viewModel(),
                    buzo = ui.apertura.buzoAsignado.ifBlank { "Buzo por asignar" },
                    tabs = usuario?.let { tabsPara(it.rol) } ?: Tab.entries.toList(),
                    onGuardar = { encuesta ->
                        faena.registrarEncuesta(encuesta)
                        ultimaEncuesta = encuesta
                        nav.navigate(Ruta.AST)
                    },
                    onVolver = { nav.popBackStack() },
                    onTab = ::irATab
                )
            }

            composable(Ruta.AST) { entrada ->
                val padre = remember(entrada) { nav.getBackStackEntry(Ruta.FAENA) }
                val faena: FaenaViewModel = viewModel(viewModelStoreOwner = padre)
                val ui by faena.ui.collectAsState()

                AstScreen(
                    vm = viewModel(),
                    centro = ui.apertura.centroCultivo.ifBlank { "Centro por definir" },
                    fecha = ui.apertura.fecha.ifBlank { "Sin fecha" },
                    onGuardar = { ast ->
                        faena.registrarAst(ast)
                        nav.navigate(Ruta.RESUMEN)
                    },
                    onVolver = { nav.popBackStack() }
                )
            }

            composable(Ruta.RESUMEN) { entrada ->
                val padre = remember(entrada) { nav.getBackStackEntry(Ruta.FAENA) }
                val faena: FaenaViewModel = viewModel(viewModelStoreOwner = padre)
                val ui by faena.ui.collectAsState()
                val actual = usuario

                if (actual != null) {
                    ResumenFaenaScreen(
                        vm = viewModel(),
                        resumen = ResumenFaena(
                            apertura = ui.apertura,
                            checklist = null,
                            ast = ui.ast,
                            encuesta = ui.encuesta
                        ),
                        decision = ui.decision,
                        rol = actual.rol,
                        nombreUsuario = actual.nombre,
                        onDecidir = { decision ->
                            faena.registrarDecision(decision)
                        },
                        onVolver = { nav.popBackStack() },
                        onFinalizar = {
                            nav.navigate(Ruta.DASHBOARD) {
                                popUpTo(Ruta.DASHBOARD) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}

```
