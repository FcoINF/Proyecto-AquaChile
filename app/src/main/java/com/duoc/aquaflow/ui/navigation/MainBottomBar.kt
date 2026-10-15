package com.duoc.aquaflow.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.ui.theme.AquaNavy
import com.duoc.aquaflow.ui.theme.AquaOnSurfaceVariant
import com.duoc.aquaflow.ui.theme.AquaOrangeContainer
import com.duoc.aquaflow.ui.theme.AquaSurface

/** Barra inferior. La pestaña "Usuarios" solo existe para el administrador. */
@Composable
fun MainBottomBar(actual: Tab, rol: Rol, onSeleccionar: (Tab) -> Unit) {
    val tabs = Tab.values().filter { it != Tab.USUARIOS || rol == Rol.ADMINISTRADOR }
    NavigationBar(containerColor = AquaSurface) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = tab == actual,
                onClick = { onSeleccionar(tab) },
                icon = { Icon(tab.icono, contentDescription = null) },
                label = { Text(tab.etiqueta) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AquaNavy,
                    selectedTextColor = AquaNavy,
                    indicatorColor = AquaOrangeContainer,
                    unselectedIconColor = AquaOnSurfaceVariant,
                    unselectedTextColor = AquaOnSurfaceVariant
                )
            )
        }
    }
}
