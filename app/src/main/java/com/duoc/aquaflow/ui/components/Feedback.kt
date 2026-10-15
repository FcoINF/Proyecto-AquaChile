package com.duoc.aquaflow.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.ui.theme.AquaNavy
import com.duoc.aquaflow.ui.theme.AquaOnSurfaceVariant
import com.duoc.aquaflow.ui.theme.AquaOrange

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AquaOrange)
    }
}

@Composable
fun EmptyState(
    titulo: String,
    texto: String,
    modifier: Modifier = Modifier,
    icono: ImageVector = Icons.Filled.Info
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icono, contentDescription = null, tint = AquaOnSurfaceVariant, modifier = Modifier.size(40.dp))
        Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = AquaOnSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/** Se muestra cuando la faena no existe o el usuario no tiene permiso para verla. */
@Composable
fun NoDisponible(onVolver: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        EmptyState(
            titulo = "Faena no disponible",
            texto = "La faena no existe o no tienes permiso para verla.",
            icono = Icons.Filled.Lock
        )
        Spacer(Modifier.height(12.dp))
        SecondaryButton(texto = "Volver", onClick = onVolver)
    }
}

/** Aviso destacado con una lista de mensajes (bloqueos, faltantes o advertencias). */
@Composable
fun AlertBanner(
    titulo: String,
    mensajes: List<String>,
    tono: Tone,
    modifier: Modifier = Modifier,
    icono: ImageVector = Icons.Filled.Warning
) {
    Surface(color = tono.fondo, contentColor = tono.contenido, shape = RoundedCornerShape(14.dp), modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(22.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(titulo, style = MaterialTheme.typography.titleSmall)
                mensajes.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

/** Indicador del modo de trabajo local y de los registros aún no sincronizados. */
@Composable
fun BannerLocal(pendientesSync: Int, modifier: Modifier = Modifier) {
    Surface(color = AquaNavy, contentColor = androidx.compose.ui.graphics.Color.White, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("MODO LOCAL ACTIVO · ROOM DB", style = MaterialTheme.typography.labelSmall, color = AquaOrange)
            Text(
                if (pendientesSync == 0) "Todo guardado" else "$pendientesSync sin sincronizar",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/** Muestra [mensaje] como snackbar una sola vez y avisa cuando fue mostrado. */
@Composable
fun MensajeSnackbar(mensaje: String?, host: SnackbarHostState, onConsumido: () -> Unit) {
    LaunchedEffect(mensaje) {
        if (mensaje != null) {
            host.showSnackbar(mensaje)
            onConsumido()
        }
    }
}
