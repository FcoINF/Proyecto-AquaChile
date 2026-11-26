package com.duoc.aquaflow.ui.components

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.duoc.aquaflow.viewmodel.PronosticoUbicacionViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BarraPronosticoUbicacion(vm: PronosticoUbicacionViewModel) {
    val context = LocalContext.current
    val ui by vm.ui.collectAsState()
    fun tienePermiso() = ContextCompat.checkSelfPermission(context, ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    LaunchedEffect(vm) {
        if (tienePermiso() && ui.datos == null && ui.mensaje == null) vm.consultar()
    }
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permisos ->
        if (permisos.values.any { it }) vm.consultar() else vm.permisoDenegado()
    }

    val textoLinea = when {
        ui.cargando -> "Consultando pronóstico…"
        ui.datos != null -> {
            val datos = ui.datos!!
            listOfNotNull(
                datos.current?.temperatura?.let { "${numero(it)} °C" },
                datos.current?.viento?.let { "Viento ${numero(it)} kn" },
                datos.daily?.lluvia?.firstOrNull()?.let { "Lluvia $it%" }
            ).joinToString(" · ").ifBlank { "Pronóstico actualizado" }
        }
        ui.mensaje != null -> ui.mensaje!!
        else -> "Ver pronóstico de mi ubicación"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1D2C38)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = textoLinea,
                modifier = Modifier.weight(1f),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (ui.cargando) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 8.dp).size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF9CC7FF)
                )
            } else {
                IconButton(
                    onClick = {
                        if (tienePermiso()) {
                            vm.consultar()
                        } else {
                            pedirPermiso.launch(arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION))
                        }
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = if (ui.datos != null) "Actualizar pronóstico" else "Ver pronóstico de mi ubicación",
                        tint = Color(0xFF9CC7FF),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun numero(valor: Double): String = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CL")).format(valor)