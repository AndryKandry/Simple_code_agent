package ru.agent.features.chat.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.agent.features.chat.domain.model.TokenMetrics
import ru.agent.features.chat.presentation.theme.ChatColors
import kotlin.math.roundToInt

/**
 * Panel displaying token compression metrics.
 *
 * Shows:
 * - Tokens before/after compression
 * - Compression ratio percentage
 * - Total tokens saved
 * - Number of messages processed
 */
@Composable
fun MetricsPanel(
    lastMetrics: TokenMetrics?,
    totalTokensSaved: Int,
    compressionCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ChatColors.UserMessageColor.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header - always visible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Token Metrics",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Quick stats in header
                if (totalTokensSaved > 0) {
                    Text(
                        text = "Saved: ${formatTokens(totalTokensSaved)} tokens",
                        fontSize = 12.sp,
                        color = ChatColors.AccentColor
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Expand/collapse button
                IconButton(
                    onClick = onToggle,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Expanded content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    // Global stats
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricItem(
                            label = "Total Saved",
                            value = formatTokens(totalTokensSaved),
                            highlight = true
                        )
                        MetricItem(
                            label = "Compressions",
                            value = compressionCount.toString(),
                            highlight = false
                        )
                    }

                    // Last compression details
                    if (lastMetrics != null) {
                        HorizontalDivider()

                        Text(
                            text = "Last Compression",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricItem(
                                label = "Before",
                                value = formatTokens(lastMetrics.tokensBefore),
                                highlight = false
                            )
                            MetricItem(
                                label = "After",
                                value = formatTokens(lastMetrics.tokensAfter),
                                highlight = false
                            )
                            MetricItem(
                                label = "Saved",
                                value = "${formatTokens(lastMetrics.tokensSaved)} (${formatPercent(lastMetrics.compressionRatio)})",
                                highlight = true
                            )
                        }

                        // Messages processed
                        Text(
                            text = "Messages: ${lastMetrics.messagesProcessed} | Strategy: ${lastMetrics.strategy.name}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else {
                        Text(
                            text = "No compression data yet. Send more messages to see metrics.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            color = if (highlight) ChatColors.AccentColor else Color.White
        )
    }
}

@Composable
private fun HorizontalDivider(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.1f))
    )
}

/**
 * Format token count for display.
 * Shows K for thousands.
 */
private fun formatTokens(tokens: Int): String {
    return when {
        tokens >= 1000 -> "${(tokens / 100.0).roundToInt() / 10.0}K"
        else -> tokens.toString()
    }
}

/**
 * Format compression ratio as percentage.
 */
private fun formatPercent(ratio: Float): String {
    val savedPercent = ((1 - ratio) * 100).roundToInt()
    return if (savedPercent > 0) "-$savedPercent%" else "$savedPercent%"
}
