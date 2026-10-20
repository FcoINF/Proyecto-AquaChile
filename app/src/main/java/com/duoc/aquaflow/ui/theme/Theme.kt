package com.duoc.aquaflow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta fija del MVP: no depende del fondo de pantalla del teléfono.
private val AquaFlowColors = lightColorScheme(
    primary = AquaNaranja,
    onPrimary = AquaAzul,
    primaryContainer = AquaNaranjaSuave,
    onPrimaryContainer = AquaAzul,
    secondary = AquaAzul,
    onSecondary = AquaBlanco,
    secondaryContainer = AquaGrisSuave,
    onSecondaryContainer = AquaAzul,
    tertiary = AquaAzul,
    onTertiary = AquaBlanco,
    tertiaryContainer = AquaGrisSuave,
    onTertiaryContainer = AquaAzul,
    background = AquaBlanco,
    onBackground = AquaAzul,
    surface = AquaBlanco,
    onSurface = AquaAzul,
    surfaceVariant = AquaGrisSuave,
    onSurfaceVariant = Color(0xFF43515C),
    surfaceTint = AquaNaranja,
    outline = Color(0xFF697680),
    outlineVariant = Color(0xFFCAD1D6),
    error = Color(0xFFBA1A1A),
    onError = AquaBlanco,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = AquaAzul,
    inverseOnSurface = AquaBlanco,
    inversePrimary = AquaNaranjaSuave
)

@Composable
fun AquaFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AquaFlowColors, typography = Typography) {
        Surface(color = MaterialTheme.colorScheme.background, content = content)
    }
}
