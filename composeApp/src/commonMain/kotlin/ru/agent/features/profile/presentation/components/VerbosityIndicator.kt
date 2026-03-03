package ru.agent.features.profile.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.agent.features.memory.domain.model.ResponseVerbosity
import ru.agent.features.profile.presentation.theme.ProfileColors

/**
 * Стиль индикатора детализации.
 */
enum class VerbosityStyle {
    /** Точки (1/2/3) */
    DOTS,
    /** Текстовый chip */
    CHIP
}

/**
 * Индикатор уровня детализации ответов.
 *
 * @param verbosity Уровень детализации
 * @param style Стиль отображения
 * @param modifier Modifier
 */
@Composable
fun VerbosityIndicator(
    verbosity: ResponseVerbosity,
    style: VerbosityStyle = VerbosityStyle.DOTS,
    modifier: Modifier = Modifier
) {
    when (style) {
        VerbosityStyle.DOTS -> VerbosityDots(verbosity, modifier)
        VerbosityStyle.CHIP -> VerbosityChip(verbosity, modifier)
    }
}

/**
 * Индикатор в виде точек.
 *
 * CONCISE = 1 точка
 * NORMAL = 2 точки
 * DETAILED = 3 точки
 */
@Composable
private fun VerbosityDots(
    verbosity: ResponseVerbosity,
    modifier: Modifier = Modifier
) {
    val activeCount = when (verbosity) {
        ResponseVerbosity.CONCISE -> 1
        ResponseVerbosity.NORMAL -> 2
        ResponseVerbosity.DETAILED -> 3
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val isActive = index < activeCount
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) ProfileColors.VerbosityDotActive
                        else ProfileColors.VerbosityDotInactive
                    )
            )
        }
    }
}

/**
 * Индикатор в виде текстового chip.
 */
@Composable
private fun VerbosityChip(
    verbosity: ResponseVerbosity,
    modifier: Modifier = Modifier
) {
    val label = when (verbosity) {
        ResponseVerbosity.CONCISE -> "Brief"
        ResponseVerbosity.NORMAL -> "Normal"
        ResponseVerbosity.DETAILED -> "Detailed"
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ProfileColors.BadgeBackground)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Компактный индикатор детализации для TopAppBar.
 */
@Composable
fun CompactVerbosityIndicator(
    verbosity: ResponseVerbosity,
    modifier: Modifier = Modifier
) {
    VerbosityIndicator(
        verbosity = verbosity,
        style = VerbosityStyle.DOTS,
        modifier = modifier
    )
}
