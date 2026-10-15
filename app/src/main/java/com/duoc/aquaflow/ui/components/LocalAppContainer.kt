package com.duoc.aquaflow.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import com.duoc.aquaflow.di.AppContainer

/** Acceso al contenedor de dependencias desde cualquier composable. */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer no fue provisto. Envuelve la UI con CompositionLocalProvider(LocalAppContainer provides ...).")
}
