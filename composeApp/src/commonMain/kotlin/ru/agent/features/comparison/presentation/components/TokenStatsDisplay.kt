package ru.agent.features.comparison.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.agent.features.comparison.domain.model.TokenUsage
import ru.agent.features.comparison.presentation.theme.ComparisonColors

/**
 * Displays token usage statistics
 */
@Composable
fun TokenStatsDisplay(
    usage: TokenUsage?,
    finishReason: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Token Stats:",
            style = MaterialTheme.typography.labelMedium,
            color = ComparisonColors.LabelColor,
            fontWeight = FontWeight.Bold
        )

        if (usage != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TokenStatItem(
                    label = "Prompt",
                    value = usage.promptTokens,
                    color = ComparisonColors.PromptTokensColor
                )
                TokenStatItem(
                    label = "Completion",
                    value = usage.completionTokens,
                    color = ComparisonColors.CompletionTokensColor
                )
                TokenStatItem(
                    label = "Total",
                    value = usage.totalTokens,
                    color = ComparisonColors.TotalTokensColor
                )
            }

            finishReason?.let { reason ->
                Text(
                    text = "finish_reason: $reason",
                    style = MaterialTheme.typography.bodySmall,
                    color = ComparisonColors.LabelColor
                )
            }
        } else {
            Text(
                text = "No token stats available",
                style = MaterialTheme.typography.bodySmall,
                color = ComparisonColors.HintTextColor
            )
        }
    }
}

@Composable
private fun TokenStatItem(
    label: String,
    value: Int,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = ComparisonColors.LabelColor
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
