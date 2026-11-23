package com.duoc.aquaflow.ui.components

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    Column(
        Modifier.fillMaxWidth().background(Color(0xFF1D2C38)).padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ui.datos?.let { datos ->
            Text("Pronóstico de tu ubicación · Open-Meteo", color = Color.White, style = MaterialTheme.typography.labelMedium)
            val actual = listOfNotNull(
                datos.current?.temperatura?.let { "${numero(it)} °C ahora" },
                datos.current?.viento?.let { "Viento ${numero(it)} kn" }
            ).joinToString(" · ")
            val diario = listOfNotNull(
                datos.daily?.minima?.firstOrNull()?.let { "Mín. ${numero(it)} °C" },
                datos.daily?.maxima?.firstOrNull()?.let { "Máx. ${numero(it)} °C" },
                datos.daily?.lluvia?.firstOrNull()?.let { "Lluvia $it %" }
            ).joinToString(" · ")
            if (actual.isNotBlank()) Text(actual, color = Color.White, style = MaterialTheme.typography.bodySmall)
            if (diario.isNotBlank()) Text("Hoy: $diario", color = Color.White, style = MaterialTheme.typography.bodySmall)
        }
        ui.mensaje?.let { Text(it, color = Color.White, style = MaterialTheme.typography.bodySmall) }
        TextButton(
            enabled = !ui.cargando,
            onClick = {
                if (tienePermiso()) {
                    vm.consultar()
                } else {
                    pedirPermiso.launch(arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION))
                }
            },
            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF9CC7FF), disabledContentColor = Color.White)
        ) {
            Text(when {
                ui.cargando -> "Consultando ubicación y pronóstico…"
                ui.datos != null -> "Actualizar pronóstico"
                else -> "Ver pronóstico de mi ubicación"
            })
        }
    }
}

private fun numero(valor: Double): String = NumberFormat.getNumberInstance(Locale("es", "CL")).format(valor)
