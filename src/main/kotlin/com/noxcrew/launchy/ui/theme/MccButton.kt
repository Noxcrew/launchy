package com.noxcrew.launchy.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withCompositionLocals
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em

object MccButtonColors {
    val Ink: ButtonColors = ButtonColors(
        MccColors.InkBlack,
        MccColors.White,
        MccColors.Dark2,
        MccColors.DarkLight
    )

    val Yellow: ButtonColors = ButtonColors(
        containerColor = MccColors.Yellow,
        contentColor = MccColors.InkBlack,
        disabledContainerColor = MccColors.Dark2,
        disabledContentColor = MccColors.DarkLight,
    )

    val Dark: ButtonColors = ButtonColors(
        containerColor = MccColors.Dark2,
        contentColor = MccColors.White,
        disabledContainerColor = MccColors.DarkLight,
        disabledContentColor = MccColors.DarkLight,
    )
}

@Composable
fun MccButton(
    enabled: Boolean = true,
    colors: ButtonColors = MccButtonColors.Ink,
    onClick: () -> Unit,
    content: @Composable () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val shape = RoundedCornerShape(6.dp)

    val textStyle = LocalTextStyle.current.copy(
        color = if (enabled) colors.contentColor else colors.disabledContentColor,
        fontFamily = MccFonts.NotoSans,
        fontWeight = FontWeight.W600,
        fontSize = 0.75.em,
        letterSpacing = 0.125.em,
    )

    val shadowAlpha by animateFloatAsState(if (hovered) 1f else 0.16f)

    withCompositionLocals(LocalTextStyle provides textStyle, LocalContentColor provides textStyle.color) {
        Box(
            Modifier
                .shadow(
                    elevation = 16.dp,
                    shape = shape,
                    ambientColor = Color.Black.copy(shadowAlpha),
                    spotColor = Color.Black.copy(shadowAlpha)
                )
                .background(if (enabled) colors.containerColor else colors.disabledContainerColor, shape)
                .clickable(interactionSource = interactionSource, onClick = onClick, enabled = enabled, indication = null)
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(20.dp, 13.dp)
        ) {
            content()
        }
    }
}