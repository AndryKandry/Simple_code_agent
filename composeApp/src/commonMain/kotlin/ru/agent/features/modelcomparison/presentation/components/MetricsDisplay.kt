package ru.agent.features.modelcomparison.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.agent.features.modelcomparison.domain.model.ComparisonResult
import ru.agent.features.modelcomparison.domain.model.ModelType
import ru.agent.features.modelcomparison.presentation.theme.ModelComparisonColors
import ru.agent.features.modelcomparison.presentation.utils.formatCost

/**
 * Display component for comparison metrics summary
 */
@Composable
fun MetricsDisplay(
    result: ComparisonResult?,
    modifier: Modifier = Modifier
) {
    if (result == null) return

    Surface(
        modifier = modifier,
        color = ModelComparisonColors.SurfaceColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Comparison Metrics",
                color = ModelComparisonColors.TextColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Total duration
                MetricItem(
                    label = "Duration",
                    value = "${result.totalDurationMs}ms",
                    color = ModelComparisonColors.MetricsTimeColor,
                    modifier = Modifier.weight(1f)
                )

                // Total tokens
                MetricItem(
                    label = "Tokens",
                    value = result.totalTokens.toString(),
                    color = ModelComparisonColors.MetricsTokensColor,
                    modifier = Modifier.weight(1f)
                )

                // Total cost
                MetricItem(
                    label = "Cost",
                    value = "\$${formatCost(result.totalCost)}",
                    color = ModelComparisonColors.MetricsCostColor,
                    modifier = Modifier.weight(1f)
                )
            }

            // Per-model breakdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                result.responses.forEach { (modelType, response) ->
                    ModelMetricBadge(
                        modelType = modelType,
                        response = response,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = label,
            color = ModelComparisonColors.TextSecondaryColor,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ModelMetricBadge(
    modelType: ModelType,
    response: ru.agent.features.modelcomparison.domain.model.ModelResponse,
    modifier: Modifier = Modifier
) {
    val accentColor = when (modelType) {
        ModelType.WEAK -> ModelComparisonColors.WeakModelColor
        ModelType.MEDIUM -> ModelComparisonColors.MediumModelColor
        ModelType.STRONG -> ModelComparisonColors.StrongModelColor
    }

    Surface(
        modifier = modifier,
        color = accentColor.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = modelType.displayName,
                color = accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${response.durationMs}ms",
                color = ModelComparisonColors.TextSecondaryColor,
                fontSize = 10.sp
            )
        }
    }
}
