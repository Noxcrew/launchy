package com.noxcrew.launchy.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties

fun shiftHue(col: Color, hue: Float): Color {
    val hsbVals = FloatArray(3)
    val javaCol = java.awt.Color(col.red, col.green, col.blue, col.alpha)
    java.awt.Color.RGBtoHSB(javaCol.red, javaCol.green, javaCol.blue, hsbVals)
    val shifted = Color(java.awt.Color.HSBtoRGB(hue, hsbVals[1], hsbVals[2]))
    return Color(shifted.red, shifted.green, shifted.blue, col.alpha)
}

@Composable
fun createColorScheme(hue: Float = 0.02f): ColorScheme = remember {
    val base = darkColorScheme()
    ColorScheme(
        primary = shiftHue(base.primary, hue),
        onPrimary = shiftHue(base.onPrimary, hue),
        primaryContainer = shiftHue(base.primaryContainer, hue),
        onPrimaryContainer = shiftHue(base.onPrimaryContainer, hue),
        inversePrimary = shiftHue(base.inversePrimary, hue),
        secondary = shiftHue(base.secondary, hue),
        onSecondary = shiftHue(base.onSecondary, hue),
        secondaryContainer = shiftHue(base.secondaryContainer, hue),
        onSecondaryContainer = shiftHue(base.onSecondaryContainer, hue),
        tertiary = shiftHue(base.tertiary, hue),
        onTertiary = shiftHue(base.onTertiary, hue),
        tertiaryContainer = shiftHue(base.tertiaryContainer, hue),
        onTertiaryContainer = shiftHue(base.onTertiaryContainer, hue),
        background = shiftHue(base.background, hue),
        onBackground = shiftHue(base.onBackground, hue),
        surface = shiftHue(base.surface, hue),
        onSurface = shiftHue(base.onSurface, hue),
        surfaceVariant = shiftHue(base.surfaceVariant, hue),
        onSurfaceVariant = shiftHue(base.onSurfaceVariant, hue),
        surfaceTint = shiftHue(base.surfaceTint, hue),
        inverseSurface = shiftHue(base.inverseSurface, hue),
        inverseOnSurface = shiftHue(base.inverseOnSurface, hue),
        error = base.error,
        onError = base.onError,
        errorContainer = shiftHue(base.errorContainer, hue),
        onErrorContainer = shiftHue(base.onErrorContainer, hue),
        outline = shiftHue(base.outline, hue),
        outlineVariant = shiftHue(base.outlineVariant, hue),
        scrim = shiftHue(base.scrim, hue),
        surfaceBright = shiftHue(base.surfaceBright, hue),
        surfaceDim = shiftHue(base.surfaceDim, hue),
        surfaceContainer = shiftHue(base.surfaceContainer, hue),
        surfaceContainerHigh = shiftHue(base.surfaceContainerHigh, hue),
        surfaceContainerHighest = shiftHue(base.surfaceContainerHighest, hue),
        surfaceContainerLow = shiftHue(base.surfaceContainerLow, hue),
        surfaceContainerLowest = shiftHue(base.surfaceContainerLowest, hue),
        primaryFixed = shiftHue(base.primaryFixed, hue),
        primaryFixedDim = shiftHue(base.primaryFixedDim, hue),
        onPrimaryFixed = shiftHue(base.onPrimaryFixed, hue),
        onPrimaryFixedVariant = shiftHue(base.onPrimaryFixedVariant, hue),
        secondaryFixed = shiftHue(base.secondaryFixed, hue),
        secondaryFixedDim = shiftHue(base.secondaryFixedDim, hue),
        onSecondaryFixed = shiftHue(base.onSecondaryFixed, hue),
        onSecondaryFixedVariant = shiftHue(base.onSecondaryFixedVariant, hue),
        tertiaryFixed = shiftHue(base.tertiaryFixed, hue),
        tertiaryFixedDim = shiftHue(base.tertiaryFixedDim, hue),
        onTertiaryFixed = shiftHue(base.onTertiaryFixed, hue),
        onTertiaryFixedVariant = shiftHue(base.onTertiaryFixedVariant, hue),
    )
}
