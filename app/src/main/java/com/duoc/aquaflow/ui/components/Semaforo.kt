package com.duoc.aquaflow.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.model.NivelSalud
import com.duoc.aquaflow.ui.theme.SemaforoAzul
import com.duoc.aquaflow.ui.theme.SemaforoRojo
import com.duoc.aquaflow.ui.theme.SemaforoVerde

// Etiqueta de color reutilizable (la "píldora" del semáforo).
@Composable
fun Etiqueta(texto: String, color: Color) {
    Surface(color = color, shape = RoundedCornerShape(50)) {
        Text(texto, color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
    }
}

@Composable
fun SemaforoSalud(n: NivelSalud) = Etiqueta(
    "Salud: ${n.texto}",
    when (n) {
        NivelSalud.NORMAL -> SemaforoVerde
        NivelSalud.ATENCION -> SemaforoAzul
        NivelSalud.CRITICO -> SemaforoRojo
    }
)
