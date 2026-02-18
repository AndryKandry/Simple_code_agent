package ru.agent.features.reasoning.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.agent.features.reasoning.domain.model.ReasoningMethod
import ru.agent.features.reasoning.domain.model.ReasoningResult
import ru.agent.features.reasoning.domain.model.ReasoningStatus
import ru.agent.features.reasoning.presentation.theme.ReasoningColors

/**
 * Panel displaying a single reasoning method result
 */
@Composable
fun ReasoningPanel(
    method: ReasoningMethod,
    result: ReasoningResult?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val methodColor = ReasoningColors.getColorForMethod(method.colorHex)
    val isCurrentlyLoading = isLoading && (result == null || result.status == ReasoningStatus.LOADING)

    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = ReasoningColors.PanelBackground
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Header with method name and status indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(methodColor)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Method name
                Text(
                    text = method.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = methodColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Status indicator
                when {
                    isCurrentlyLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = methodColor,
                            strokeWidth = 2.dp
                        )
                    }
                    result?.status == ReasoningStatus.SUCCESS -> {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ReasoningColors.SuccessColor)
                        )
                    }
                    result?.status == ReasoningStatus.ERROR -> {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ReasoningColors.ErrorColor)
                        )
                    }
                }
            }

            // Description
            Text(
                text = method.description,
                style = MaterialTheme.typography.bodySmall,
                color = ReasoningColors.HintTextColor,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = ReasoningColors.DividerColor
            )

            // Content area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        ReasoningColors.ContentBackground,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                when {
                    isCurrentlyLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = methodColor
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Processing...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ReasoningColors.HintTextColor
                                )
                            }
                        }
                    }
                    result?.status == ReasoningStatus.ERROR -> {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = "Error",
                                style = MaterialTheme.typography.labelMedium,
                                color = ReasoningColors.ErrorColor,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result.error ?: "Unknown error",
                                style = MaterialTheme.typography.bodySmall,
                                color = ReasoningColors.ErrorColor
                            )
                        }
                    }
                    result?.status == ReasoningStatus.SUCCESS && result.content != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = result.content,
                                style = MaterialTheme.typography.bodySmall,
                                color = ReasoningColors.TextColor
                            )
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Waiting for comparison...",
                                style = MaterialTheme.typography.bodySmall,
                                color = ReasoningColors.HintTextColor
                            )
                        }
                    }
                }
            }

            // Duration footer
            if (result?.durationMs != null && result.durationMs > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = ReasoningColors.DividerColor
                )
                Text(
                    text = "Duration: ${result.durationMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = ReasoningColors.LabelColor
                )
            }
        }
    }
}
