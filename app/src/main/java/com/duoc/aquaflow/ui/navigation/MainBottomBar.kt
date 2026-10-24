package com.duoc.aquaflow.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.duoc.aquaflow.ui.theme.AquaAzul
import com.duoc.aquaflow.ui.theme.AquaNaranja
import com.duoc.aquaflow.ui.theme.AquaNaranjaSuave

/** Barra inferior de navegación principal. */
@Composable
fun MainBottomBar(actual: Tab, tabs: List<Tab>, onSeleccionar: (Tab) -> Unit) {
    NavigationBar(containerColor = Color.White) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = tab == actual,
                onClick = { onSeleccionar(tab) },
                icon = { Icon(tab.icono, contentDescription = tab.etiqueta) },
                label = { Text(tab.etiqueta) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AquaNaranja,
                    selectedTextColor = AquaAzul,
                    indicatorColor = AquaNaranjaSuave
                )
            )
        }
    }
}
