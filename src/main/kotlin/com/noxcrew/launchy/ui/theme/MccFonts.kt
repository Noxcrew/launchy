package com.noxcrew.launchy.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font

object MccFonts {
    val NotoSans = FontFamily(
        listOf(
            // Body
            Font(
                resource = "font/NotoSans-VariableFont_wdth,wght.ttf",
                weight = FontWeight.W400,
                style = FontStyle.Normal,
                variationSettings = FontVariation.Settings(FontVariation.weight(400))
            ),
            // Overline
            Font(
                resource = "font/NotoSans-VariableFont_wdth,wght.ttf",
                weight = FontWeight.W600,
                style = FontStyle.Normal,
                variationSettings = FontVariation.Settings(FontVariation.weight(600))
            )
        )
    )
}