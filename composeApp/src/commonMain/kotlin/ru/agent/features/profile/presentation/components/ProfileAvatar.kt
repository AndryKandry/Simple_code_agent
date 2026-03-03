package ru.agent.features.profile.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import ru.agent.features.profile.presentation.theme.ProfileColors

/**
 * Аватар профиля с initials на цветном фоне.
 *
 * @param name Имя пользователя
 * @param size Размер аватара
 * @param modifier Modifier
 */
@Composable
fun ProfileAvatar(
    name: String,
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    val initials = getInitials(name)
    val backgroundColor = getAvatarColor(name)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontSize = when {
                size >= 72.dp -> 28.sp
                size >= 56.dp -> 22.sp
                else -> 14.sp
            },
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Получить initials из имени.
 *
 * Примеры:
 * - "John Doe" -> "JD"
 * - "Alice" -> "A"
 * - "Bob Smith Jr" -> "BS"
 */
fun getInitials(name: String): String {
    return name
        .trim()
        .split(" ")
        .filter { it.isNotEmpty() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
}

/**
 * Получить детерминированный цвет аватара на основе имени.
 *
 * Цвет выбирается из палитры на основе hash кода имени.
 */
fun getAvatarColor(name: String): Color {
    val colors = ProfileColors.AvatarColors
    val index = abs(name.hashCode()) % colors.size
    return colors[index]
}

/**
 * Размеры аватара.
 */
object AvatarSize {
    val Indicator = 32.dp
    val Dropdown = 56.dp
    val Dialog = 72.dp
}
