package com.duoc.aquaflow.ui.components

import androidx.compose.runtime.Composable
import com.duoc.aquaflow.model.ResultadoFaena
import com.duoc.aquaflow.ui.theme.SemaforoAzul
import com.duoc.aquaflow.ui.theme.SemaforoRojo
import com.duoc.aquaflow.ui.theme.SemaforoVerde

// Semáforo global de la faena; reutiliza la `Etiqueta` de Semaforo.kt.
@Composable
fun SemaforoFaena(r: ResultadoFaena) = Etiqueta(
    "Faena: ${r.texto}",
    when (r) {
        ResultadoFaena.CUMPLE -> SemaforoVerde
        ResultadoFaena.OBSERVADO -> SemaforoAzul
        ResultadoFaena.REQUIERE_REVISION -> SemaforoRojo
    }
)
