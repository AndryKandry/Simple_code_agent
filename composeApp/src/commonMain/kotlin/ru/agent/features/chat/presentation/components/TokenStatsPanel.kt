package ru.agent.features.chat.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.agent.features.chat.domain.model.ModelLimits
import ru.agent.features.chat.domain.model.TokenStats
import ru.agent.features.chat.domain.model.UsageLevel

// Colors for token usage indicator
private val NormalColor = Color(0xFF4CAF50)    // Green
private val WarningColor = Color(0xFFFFC107)   // Yellow/Amber
private val CriticalColor = Color(0xFFF44336)  // Red
private val BackgroundColor = Color(0xFF1E1E2E)
private val CardBackgroundColor = Color(0xFF2A2A3E)
private val TextColor = Color.White
private val SecondaryTextColor = Color.White.copy(alpha = 0.7f)

/**
 * Panel displaying token usage statistics.
 *
 * Shows:
 * - Token count for current input
 * - Token count for conversation history
 * - Total tokens used
 * - Progress bar with color-coded usage level
 * - Warning message if approaching context limit
 *
 * @param tokenStats Token statistics to display
 * @param maxTokens Maximum context tokens
 * @param showWarning Whether to show warning banner
 * @param warningMessage Warning message to display
 * @param onDismissWarning Callback when warning is dismissed
 * @param normalColor Color for normal usage level
 * @param warningColor Color for warning usage level
 * @param criticalColor Color for critical usage level
 * @param modifier Modifier for the panel
 */
@Composable
fun TokenStatsPanel(
    tokenStats: TokenStats,
    maxTokens: Int = ModelLimits.DEFAULT_CONTEXT_LIMIT,
    showWarning: Boolean = false,
    warningMessage: String? = null,
    onDismissWarning: () -> Unit = {},
    normalColor: Color = NormalColor,
    warningColor: Color = WarningColor,
    criticalColor: Color = CriticalColor,
    modifier: Modifier = Modifier
) {
    val usageLevel = ModelLimits.getUsageLevel(tokenStats.totalTokens, maxTokens)
    val usagePercentage = (tokenStats.getUsagePercentage(maxTokens) / 100f).coerceIn(0f, 1f)

    val progressColor by animateColorAsState(
        targetValue = when (usageLevel) {
            UsageLevel.Normal -> normalColor
            UsageLevel.Warning -> warningColor
            UsageLevel.Critical -> criticalColor
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "progress_color"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BackgroundColor)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Token stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Input tokens
            TokenStatItem(
                label = "Input",
                value = ModelLimits.formatTokenCount(tokenStats.inputTokens),
                modifier = Modifier.weight(1f)
            )

            // History tokens
            TokenStatItem(
                label = "History",
                value = ModelLimits.formatTokenCount(tokenStats.historyTokens),
                modifier = Modifier.weight(1f)
            )

            // Total tokens
            TokenStatItem(
                label = "Total",
                value = "${ModelLimits.formatTokenCount(tokenStats.totalTokens)} / ${ModelLimits.formatTokenCount(maxTokens)}",
                modifier = Modifier.weight(1.2f),
                isHighlighted = usageLevel != UsageLevel.Normal,
                highlightColor = warningColor
            )
        }

        // Progress bar
        TokenProgressBar(
            progress = usagePercentage,
            color = progressColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        // Warning message
        if (showWarning && warningMessage != null) {
            WarningBanner(
                message = warningMessage,
                isCritical = usageLevel == UsageLevel.Critical,
                onDismiss = onDismissWarning,
                warningColor = warningColor,
                criticalColor = criticalColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun TokenStatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    highlightColor: Color = WarningColor
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = SecondaryTextColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal
        )
        Text(
            text = value,
            color = if (isHighlighted) highlightColor else TextColor,
            fontSize = 13.sp,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun TokenProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(CardBackgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(4.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
    }
}

@Composable
private fun WarningBanner(
    message: String,
    isCritical: Boolean,
    onDismiss: () -> Unit,
    warningColor: Color = WarningColor,
    criticalColor: Color = CriticalColor,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isCritical) {
        criticalColor.copy(alpha = 0.15f)
    } else {
        warningColor.copy(alpha = 0.15f)
    }
    val textColor = if (isCritical) criticalColor else warningColor

    Row(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = message,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss",
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Compact token badge for displaying in message items.
 */
@Composable
fun MessageTokenBadge(
    tokenCount: Int,
    modifier: Modifier = Modifier
) {
    if (tokenCount > 0) {
        Text(
            text = "${ModelLimits.formatTokenCount(tokenCount)} tokens",
            color = SecondaryTextColor,
            fontSize = 10.sp,
            modifier = modifier
        )
    }
}
