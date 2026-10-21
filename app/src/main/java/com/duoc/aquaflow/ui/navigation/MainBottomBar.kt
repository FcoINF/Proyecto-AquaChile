package com.duoc.aquaflow.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/** Barra inferior de navegación principal. */
@Composable
fun MainBottomBar(actual: Tab, tabs: List<Tab>, onSeleccionar: (Tab) -> Unit) {
    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = tab == actual,
                onClick = { onSeleccionar(tab) },
                icon = { Icon(tab.icono, contentDescription = tab.etiqueta) },
                label = { Text(tab.etiqueta) }
            )
        }
    }
}
