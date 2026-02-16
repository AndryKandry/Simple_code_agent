package ru.agent.design.extensions

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.agent.theme.DefaultTheme

@Composable
fun Modifier.backgroundGradient(
    color1: Color = DefaultTheme.colors.gradient.first,
    color2: Color = DefaultTheme.colors.gradient.second,
    shape: Shape = RectangleShape
): Modifier {
    val gradient = Brush.linearGradient(
        0.0f to color1,
        200.0f to color2,
        start = Offset.Zero,
        end = Offset.Infinite
    )
    return this.background(
        brush = gradient,
        shape = shape
    )
}

@Composable
fun Modifier.backgroundVerticalGradient(
    color1: Color = DefaultTheme.colors.gradient.first,
    color2: Color = DefaultTheme.colors.gradient.second,
    shape: Shape = RectangleShape
): Modifier {
    val gradient = Brush.verticalGradient(
        0.0f to color1,
        1000.0f to color2,
        startY = 0f,
        endY = Float.POSITIVE_INFINITY,
    )
    return this.background(
        brush = gradient,
        shape = shape
    )
}

@Composable
fun Modifier.backgroundComponentGradient(
    color1: Color = DefaultTheme.colors.componentsGradient.first,
    color2: Color = DefaultTheme.colors.componentsGradient.second,
    shape: Shape = RectangleShape
): Modifier {
    val gradient = Brush.horizontalGradient(
        0.0f to color1,
        500.0f to color2
    )
    return this.background(
        brush = gradient,
        shape = shape
    )
}

fun Modifier.modifyIf(
    condition: Boolean,
    ifTrue: Modifier,
    ifFalse: Modifier = Modifier,
): Modifier = then(if (condition) ifTrue else ifFalse)

fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
    this.clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() }) {
        onClick()
    }
}

fun Modifier.customRippleClickable(color: Color?, onClick: () -> Unit): Modifier = composed {
    this.clickable(
        indication = if (color != null) {
            remember { ripple(color = color) }
        } else {
            LocalIndication.current
        },
        interactionSource = remember { MutableInteractionSource() }) {
        onClick()
    }
}

fun Modifier.marqueeText(
    textLengthPx: Int,
    marqueeSpacing: Dp = 20.dp
): Modifier = composed {
    this.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            if (size.width < textLengthPx) {
                drawFadedEdge(leftEdge = true)
                drawFadedEdge(leftEdge = false)
            }
        }
        .basicMarquee(
            iterations = Int.MAX_VALUE,
            spacing = MarqueeSpacing(marqueeSpacing)
        )
}
