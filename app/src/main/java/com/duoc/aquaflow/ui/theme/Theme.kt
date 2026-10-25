package com.duoc.aquaflow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Paleta fija del MVP: no depende del fondo de pantalla del teléfono.
private val AquaFlowColors = lightColorScheme(
    primary = AquaNaranja,
    onPrimary = AquaBlanco,
    primaryContainer = AquaNaranjaSuave,
    onPrimaryContainer = Color(0xFF81350E),
    secondary = AquaAzul,
    onSecondary = AquaBlanco,
    secondaryContainer = AquaGrisSuave,
    onSecondaryContainer = AquaAzul,
    tertiary = AquaVerde,
    onTertiary = AquaBlanco,
    tertiaryContainer = Color(0xFFD9F3E8),
    onTertiaryContainer = Color(0xFF005B43),
    background = AquaGrisSuave,
    onBackground = AquaAzul,
    surface = AquaBlanco,
    onSurface = AquaAzul,
    surfaceVariant = Color(0xFFEAF0F4),
    onSurfaceVariant = Color(0xFF5B6D79),
    surfaceTint = AquaNaranja,
    outline = Color(0xFF8A9AA5),
    outlineVariant = Color(0xFFDCE4E9),
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
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(18.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(28.dp)
    )
    MaterialTheme(colorScheme = AquaFlowColors, typography = Typography, shapes = shapes) {
        Surface(color = MaterialTheme.colorScheme.background, content = content)
    }
}
