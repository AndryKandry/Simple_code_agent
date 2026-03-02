package ru.agent.features.chat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Format token count for display.
 * For values >= 1000, uses compact notation (e.g., "1.2k", "15k").
 */
private fun formatTokenCount(count: Int): String {
    return when {
        count >= 10000 -> "${count / 1000}k"
        count >= 1000 -> {
            val formatted = count / 100.0
            "${(formatted / 10).toInt()}.${((formatted % 10).toInt())}k"
        }
        else -> count.toString()
    }
}

/**
 * Component displaying context statistics.
 *
 * Shows:
 * - Number of messages in context
 * - Estimated token count (formatted for large numbers)
 *
 * @param messageCount Number of messages currently in context
 * @param tokenCount Estimated token count
 * @param maxMessages Maximum messages (for sliding window)
 * @param modifier Modifier for the component
 */
@Composable
fun ContextStats(
    messageCount: Int,
    tokenCount: Int,
    maxMessages: Int? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .wrapContentWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Message count
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (maxMessages != null) {
                    "Messages: $messageCount/$maxMessages"
                } else {
                    "Messages: $messageCount"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
        }

        // Separator
        Text(
            text = "|",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.4f)
        )

        // Token count with formatting
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tokens: ~${formatTokenCount(tokenCount)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Compact version of context stats for smaller spaces.
 *
 * @param messageCount Number of messages currently in context
 * @param tokenCount Estimated token count
 * @param modifier Modifier for the component
 */
@Composable
fun CompactContextStats(
    messageCount: Int,
    tokenCount: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = "$messageCount msgs | ~${formatTokenCount(tokenCount)} tok",
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.6f),
        modifier = modifier
    )
}
