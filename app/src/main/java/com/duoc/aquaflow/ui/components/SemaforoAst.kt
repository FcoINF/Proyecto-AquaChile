package com.duoc.aquaflow.ui.components

import androidx.compose.runtime.Composable
import com.duoc.aquaflow.model.ResultadoAst
import com.duoc.aquaflow.ui.theme.SemaforoAmbar
import com.duoc.aquaflow.ui.theme.SemaforoRojo
import com.duoc.aquaflow.ui.theme.SemaforoVerde

// Semáforo del AST; reutiliza la `Etiqueta` de Semaforo.kt.
@Composable
fun SemaforoAst(r: ResultadoAst) = Etiqueta(
    "AST: ${r.texto}",
    when (r) {
        ResultadoAst.CUMPLE -> SemaforoVerde
        ResultadoAst.OBSERVADO -> SemaforoAmbar
        ResultadoAst.REQUIERE_REVISION -> SemaforoRojo
    }
)