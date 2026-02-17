package ru.agent.features.comparison.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.agent.features.comparison.domain.model.ResponseData
import ru.agent.features.comparison.presentation.theme.ComparisonColors

/**
 * Panel displaying a single API response with parameters and token stats
 */
@Composable
fun ResponsePanel(
    title: String,
    titleColor: Color,
    containerColor: Color,
    responseData: ResponseData?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = ComparisonColors.DividerColor
            )

            // Parameters display
            ParametersDisplay(
                parameters = responseData?.parameters,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = ComparisonColors.DividerColor
            )

            // Response content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        ComparisonColors.ContentBackground,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = titleColor
                            )
                        }
                    }
                    responseData != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = responseData.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ComparisonColors.TextColor
                            )
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Response will appear here...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ComparisonColors.HintTextColor
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = ComparisonColors.DividerColor
            )

            // Token statistics
            TokenStatsDisplay(
                usage = responseData?.usage,
                finishReason = responseData?.finishReason,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
