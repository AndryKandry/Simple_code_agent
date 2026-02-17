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
import ru.agent.features.comparison.domain.model.ApiParameters
import ru.agent.features.comparison.presentation.theme.ComparisonColors

/**
 * Displays API parameters in a formatted way
 */
@Composable
fun ParametersDisplay(
    parameters: ApiParameters?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Parameters:",
            style = MaterialTheme.typography.labelMedium,
            color = ComparisonColors.LabelColor,
            fontWeight = FontWeight.Bold
        )

        if (parameters != null) {
            ParameterRow(label = "max_tokens", value = parameters.maxTokens.toString())
            ParameterRow(label = "temperature", value = parameters.temperature.toString())
            parameters.topP?.let { ParameterRow(label = "top_p", value = it.toString()) }
            ParameterRow(
                label = "stop",
                value = parameters.stop?.joinToString(", ") { "\"$it\"" } ?: "null"
            )
            ParameterRow(
                label = "system",
                value = if (parameters.systemPrompt != null) {
                    "\"${parameters.systemPrompt.take(30)}...\""
                } else {
                    "null"
                }
            )
        } else {
            Text(
                text = "No parameters",
                style = MaterialTheme.typography.bodySmall,
                color = ComparisonColors.HintTextColor
            )
        }
    }
}

@Composable
private fun ParameterRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = ComparisonColors.LabelColor
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = ComparisonColors.TextColor
        )
    }
}
