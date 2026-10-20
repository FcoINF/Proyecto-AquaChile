package com.duoc.aquaflow.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.duoc.aquaflow.R

// Fuentes empaquetadas: disponibles también sin conexión.
private val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_bold, FontWeight.Bold)
)
private val base = Typography()

val Typography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
    displayMedium = base.displayMedium.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
    displaySmall = base.displaySmall.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
    headlineLarge = base.headlineLarge.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
    titleSmall = base.titleSmall.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
    bodyLarge = base.bodyLarge.copy(fontFamily = Inter),
    bodyMedium = base.bodyMedium.copy(fontFamily = Inter),
    bodySmall = base.bodySmall.copy(fontFamily = Inter),
    labelLarge = base.labelLarge.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
    labelMedium = base.labelMedium.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
    labelSmall = base.labelSmall.copy(fontFamily = Inter, fontWeight = FontWeight.Bold)
)
