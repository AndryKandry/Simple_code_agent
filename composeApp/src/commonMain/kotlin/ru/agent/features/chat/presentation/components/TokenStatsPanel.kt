package ru.agent.features.chat.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.text.style.TextAlign
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
 * - Hints about token usage scenarios
 *
 * @param tokenStats Token statistics to display
 * @param maxTokens Maximum context tokens
 * @param showWarning Whether to show warning banner
 * @param warningMessage Warning message to display
 * @param onDismissWarning Callback when warning is dismissed
 * @param showHints Whether to show hints panel
 * @param onToggleHints Callback when hints panel is toggled
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
    showHints: Boolean = false,
    onToggleHints: () -> Unit = {},
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

        // Hints toggle button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clickable { onToggleHints() }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "How tokens work",
                color = SecondaryTextColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = if (showHints) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (showHints) "Hide hints" else "Show hints",
                tint = SecondaryTextColor,
                modifier = Modifier.size(16.dp)
            )
        }

        // Hints panel
        AnimatedVisibility(
            visible = showHints,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            TokenUsageHints(
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

/**
 * Panel with hints about token usage scenarios.
 */
@Composable
private fun TokenUsageHints(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(CardBackgroundColor, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Token Usage Scenarios",
            color = TextColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        // Scenario 1: Short dialog
        HintItem(
            iconColor = NormalColor,
            title = "Short Dialog",
            description = "A few messages with quick questions and answers. Uses <5% of context (green indicator).",
            example = "~1,000 tokens"
        )

        // Scenario 2: Long dialog
        HintItem(
            iconColor = WarningColor,
            title = "Long Dialog",
            description = "Extended conversation with detailed responses. Uses 80-94% of context (yellow indicator).",
            example = "~50,000 tokens"
        )

        // Scenario 3: Overflow
        HintItem(
            iconColor = CriticalColor,
            title = "Context Overflow",
            description = "Conversation exceeds model limit. API returns error, last messages may be lost.",
            example = ">64,000 tokens",
            isWarning = true
        )

        Text(
            text = "Tip: Create a new chat session to reset the token counter.",
            color = SecondaryTextColor,
            fontSize = 11.sp,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun HintItem(
    iconColor: Color,
    title: String,
    description: String,
    example: String,
    isWarning: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Color indicator
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
                .background(iconColor, RoundedCornerShape(2.dp))
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    color = TextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "($example)",
                    color = SecondaryTextColor,
                    fontSize = 10.sp
                )
                if (isWarning) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Warning",
                        tint = CriticalColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Text(
                text = description,
                color = SecondaryTextColor,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }
}
