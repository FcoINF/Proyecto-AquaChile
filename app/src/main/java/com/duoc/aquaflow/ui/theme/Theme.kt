package com.duoc.aquaflow.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Tema único en modo claro: la app se usa en exteriores (pontones) donde el alto contraste y la
 * lectura bajo luz solar importan más que un tema oscuro. No se usa color dinámico para respetar
 * la identidad de marca.
 */
private val AquaColorScheme = lightColorScheme(
    primary = AquaOrange,
    onPrimary = AquaNavy,
    primaryContainer = AquaOrangeContainer,
    onPrimaryContainer = AquaNavy,
    secondary = AquaNavy,
    onSecondary = AquaSurface,
    secondaryContainer = AquaNavyContainer,
    onSecondaryContainer = AquaNavy,
    tertiary = AquaInfo,
    onTertiary = AquaSurface,
    background = AquaBackground,
    onBackground = AquaNavy,
    surface = AquaSurface,
    onSurface = AquaNavy,
    surfaceVariant = AquaNavyContainer,
    onSurfaceVariant = AquaOnSurfaceVariant,
    surfaceContainerLowest = AquaSurface,
    surfaceContainerLow = AquaSurface,
    surfaceContainer = AquaSurface,
    surfaceContainerHigh = AquaNavyContainer,
    surfaceContainerHighest = AquaNavyContainer,
    outline = AquaOutline,
    outlineVariant = AquaOutlineVariant,
    error = AquaDanger,
    onError = AquaSurface,
    errorContainer = AquaDangerContainer,
    onErrorContainer = AquaDanger
)

private val AquaShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun AquaFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AquaColorScheme,
        typography = AquaTypography,
        shapes = AquaShapes,
        content = content
    )
}
