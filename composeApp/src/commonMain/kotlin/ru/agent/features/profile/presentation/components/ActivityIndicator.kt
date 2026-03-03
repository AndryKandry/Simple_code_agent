package ru.agent.features.profile.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.profile.presentation.theme.ProfileColors

/**
 * Проверить, был ли пользователь активен недавно.
 *
 * @param lastActiveAt Временная метка последней активности (ms)
 * @param thresholdMinutes Порог в минутах (по умолчанию 60)
 * @return true если активен в течение порога
 */
fun isRecentlyActive(lastActiveAt: Long?, thresholdMinutes: Int = 60): Boolean {
    if (lastActiveAt == null) return false
    val thresholdMs = thresholdMinutes * 60 * 1000L
    return (currentTimeMillis() - lastActiveAt) < thresholdMs
}

/**
 * Индикатор активности пользователя.
 *
 * Отображает зелёную точку если пользователь был активен недавно,
 * иначе серую точку.
 *
 * @param lastActiveAt Временная метка последней активности (ms)
 * @param size Размер точки
 * @param showPulse Показывать ли пульсацию для активного состояния
 * @param modifier Modifier
 */
@Composable
fun ActivityIndicator(
    lastActiveAt: Long?,
    size: Dp = 8.dp,
    showPulse: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isActive = isRecentlyActive(lastActiveAt)
    val color = if (isActive) ProfileColors.ActiveColor else ProfileColors.InactiveColor

    Box(modifier = modifier) {
        // Пульсация для активного состояния
        if (isActive && showPulse) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )

            Box(
                modifier = Modifier
                    .size(size)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(color.copy(alpha = alpha))
            )
        }

        // Основная точка
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/**
 * Компактный индикатор активности для аватара.
 *
 * @param lastActiveAt Временная метка последней активности
 * @param modifier Modifier
 */
@Composable
fun CompactActivityIndicator(
    lastActiveAt: Long?,
    modifier: Modifier = Modifier
) {
    ActivityIndicator(
        lastActiveAt = lastActiveAt,
        size = 8.dp,
        showPulse = false,
        modifier = modifier
    )
}
