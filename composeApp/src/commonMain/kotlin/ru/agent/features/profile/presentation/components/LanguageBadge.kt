package ru.agent.features.profile.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.agent.features.profile.presentation.theme.getLanguageColor
import ru.agent.features.profile.presentation.theme.getLanguageInitial

/**
 * Размеры бейджа языка.
 */
object BadgeSize {
    val Small = 24.dp
    val Medium = 32.dp
    val Large = 40.dp
}

/**
 * Бейдж языка программирования.
 *
 * Отображает цветной бейдж с initial языка.
 *
 * @param language Название языка
 * @param size Размер бейджа
 * @param showIcon Показывать ли initial языка
 * @param modifier Modifier
 */
@Composable
fun LanguageBadge(
    language: String,
    size: Dp = BadgeSize.Medium,
    showIcon: Boolean = true,
    modifier: Modifier = Modifier
) {
    val backgroundColor = getLanguageColor(language)
    val initial = getLanguageInitial(language)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(
                horizontal = when {
                    size >= BadgeSize.Large -> 8.dp
                    size >= BadgeSize.Medium -> 6.dp
                    else -> 4.dp
                },
                vertical = when {
                    size >= BadgeSize.Large -> 4.dp
                    size >= BadgeSize.Medium -> 3.dp
                    else -> 2.dp
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (showIcon) {
            Text(
                text = initial,
                color = Color.White,
                fontSize = when {
                    size >= BadgeSize.Large -> 14.sp
                    size >= BadgeSize.Medium -> 12.sp
                    else -> 10.sp
                },
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Компактный бейдж языка для отображения в индикаторах.
 *
 * @param language Название языка
 * @param modifier Modifier
 */
@Composable
fun CompactLanguageBadge(
    language: String,
    modifier: Modifier = Modifier
) {
    LanguageBadge(
        language = language,
        size = BadgeSize.Small,
        modifier = modifier
    )
}
