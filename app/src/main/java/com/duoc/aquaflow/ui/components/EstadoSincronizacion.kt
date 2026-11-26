package com.duoc.aquaflow.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.duoc.aquaflow.data.local.EnvioFaena
import com.duoc.aquaflow.data.local.EstadoEnvio

@Composable
fun EstadoSincronizacion(envio: EnvioFaena) {
    Text(when (envio.estado) {
        EstadoEnvio.SINCRONIZADO.name -> "Sincronizado"
        EstadoEnvio.ERROR.name -> "Error de sincronización"
        else -> "Pendiente de sincronizar"
    })
    envio.error?.let { Text(it) }
}
