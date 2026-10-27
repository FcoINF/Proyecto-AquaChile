package com.duoc.aquaflow.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.ui.theme.AquaAzulAccion

/** Barra inferior principal, con apariencia compacta de tarjeta flotante. */
@Composable
fun MainBottomBar(actual: Tab, tabs: List<Tab>, onSeleccionar: (Tab) -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 330.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, Color(0xFFD1D5DB))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(58.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val seleccionada = tab == actual
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .selectable(
                                selected = seleccionada,
                                onClick = { onSeleccionar(tab) },
                                role = Role.Tab,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icono,
                            contentDescription = null,
                            modifier = Modifier.size(21.dp),
                            tint = if (seleccionada) AquaAzulAccion else Color(0xFF64748B)
                        )
                        Text(
                            text = tab.etiqueta,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (seleccionada) AquaAzulAccion else Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}
