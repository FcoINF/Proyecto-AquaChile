package com.duoc.aquaflow.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.duoc.aquaflow.model.MedicionesInmersion

@Composable
internal fun DatosMediciones(mediciones: MedicionesInmersion) {
    Text("Tiempo de fondo · ${mediciones.tiempoFondoMinutos} min")
    Text("Profundidad máxima · ${mediciones.profundidadMaximaMetros} m")
    Text("Descenso máximo · ${mediciones.descensoMaximo} m/min")
    Text("Ascenso máximo · ${mediciones.ascensoMaximo} m/min")
    Text("Origen · ${mediciones.origen.texto}")
    mediciones.alertas.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
}
