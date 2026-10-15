package com.duoc.aquaflow.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Las maquetas usan Inter Bold. Para no depender de red ni de archivos de fuente, se usa la fuente
 * sans-serif del sistema. Para usar Inter basta con agregar los .ttf a res/font y asignar aquí
 * `AppFontFamily = FontFamily(Font(R.font.inter_regular), Font(R.font.inter_bold, FontWeight.Bold))`.
 */
val AppFontFamily: FontFamily = FontFamily.SansSerif

private val Base = Typography()

val AquaTypography = Typography(
    headlineMedium = Base.headlineMedium.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold),
    headlineSmall = Base.headlineSmall.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold),
    titleLarge = Base.titleLarge.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = Base.titleMedium.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp),
    titleSmall = Base.titleSmall.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold),
    bodyLarge = Base.bodyLarge.copy(fontFamily = AppFontFamily, fontSize = 17.sp, lineHeight = 24.sp),
    bodyMedium = Base.bodyMedium.copy(fontFamily = AppFontFamily, fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = Base.bodySmall.copy(fontFamily = AppFontFamily, fontSize = 13.sp),
    labelLarge = Base.labelLarge.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp),
    labelMedium = Base.labelMedium.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold),
    labelSmall = Base.labelSmall.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold)
)
