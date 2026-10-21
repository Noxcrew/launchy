package com.noxcrew.launchy.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

object MccColors {
    val White = Color(0xFFFAFCFD)
    val InkBlack = Color(0xFF1D3053)
    val Black = Color(0xFF1F2028)

    val Light1 = Color(0xFFF0F6FC)
    val Light2 = Color(0xFFE0E9EE)
    val Light3 = Color(0xFF7E909F)

    val Dark1 = Color(0xFF282A2D)
    val Dark2 = Color(0xFF2F3136)
    val Dark3 = Color(0xFF37393E)
    val Dark4 = Color(0xFF909398)
    val DarkLight = Color(0xFFEEEFF0)

    val Red = Color(0xFFF0264B)
    val Yellow = Color(0xFFFFDB29)
    val GradientYellow = Color(0xFFFFB211)

    val LightBlue = Color(0xFF0EAEF0)
    val GradientLightBlue = Color(0xFF0E6AF0)

    val Blue = Color(0xFF3C69F0)
}

public val MccColorScheme = darkColorScheme(
    primary = MccColors.Yellow,
    onPrimary = MccColors.InkBlack,
    primaryContainer = MccColors.Dark1,
    onPrimaryContainer = MccColors.DarkLight,
    inversePrimary = MccColors.Yellow,
    secondary = MccColors.Yellow,
    onSecondary = MccColors.InkBlack,
    secondaryContainer = MccColors.Dark2,
    onSecondaryContainer = MccColors.DarkLight,
    tertiary = MccColors.Red,
    onTertiary = MccColors.White,
    tertiaryContainer = MccColors.Dark2,
    onTertiaryContainer = MccColors.DarkLight,
    background = MccColors.Dark1,
    onBackground = MccColors.DarkLight,
    surface = MccColors.Dark2,
    onSurface = MccColors.DarkLight,
    surfaceVariant = MccColors.Dark2,
    onSurfaceVariant = MccColors.DarkLight,
    surfaceTint = MccColors.DarkLight,
    inverseSurface = MccColors.DarkLight,
    inverseOnSurface = MccColors.Dark1,
    errorContainer = MccColors.Red,
    onErrorContainer = MccColors.White,
)