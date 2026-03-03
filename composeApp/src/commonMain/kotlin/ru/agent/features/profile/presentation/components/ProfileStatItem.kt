package ru.agent.features.profile.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.agent.features.profile.presentation.theme.ProfileColors

/**
 * Элемент статистики профиля.
 *
 * @param icon Иконка
 * @param label Название показателя
 * @param value Значение
 * @param modifier Modifier
 * @param iconTint Цвет иконки
 */
@Composable
fun ProfileStatItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    iconTint: Color = ProfileColors.StatsIconColor
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            color = ProfileColors.StatsValueColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            color = ProfileColors.StatsLabelColor,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Горизонтальный ряд статистики.
 *
 * @param totalMessages Количество сообщений
 * @param totalSessions Количество сессий
 * @param totalTasksCompleted Выполненных задач
 * @param modifier Modifier
 */
@Composable
fun ProfileStatsRow(
    totalMessages: Long,
    totalSessions: Long,
    totalTasksCompleted: Long,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileStatItem(
            icon = Icons.Filled.Email,
            label = "Messages",
            value = formatNumber(totalMessages),
            modifier = Modifier.weight(1f)
        )

        ProfileStatItem(
            icon = Icons.Filled.Refresh,
            label = "Sessions",
            value = formatNumber(totalSessions),
            modifier = Modifier.weight(1f)
        )

        ProfileStatItem(
            icon = Icons.Filled.CheckCircle,
            label = "Tasks",
            value = formatNumber(totalTasksCompleted),
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Форматирование числа с разделителями.
 *
 * @param number Число
 * @return Отформатированная строка
 */
fun formatNumber(number: Long): String {
    return when {
        number >= 1_000_000 -> {
            val value = number / 100_000 / 10.0  // делим так чтобы получить 1 decimal
            "${value}M"
        }
        number >= 1_000 -> {
            val value = number / 100 / 10.0
            "${value}K"
        }
        else -> number.toString()
    }
}

