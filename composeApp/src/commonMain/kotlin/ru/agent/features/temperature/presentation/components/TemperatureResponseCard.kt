package ru.agent.features.temperature.presentation.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.agent.features.temperature.domain.model.TemperatureLevel
import ru.agent.features.temperature.domain.model.TemperatureResponse
import ru.agent.features.temperature.presentation.theme.TemperatureColors

/**
 * Card component for displaying a single temperature response
 */
@Composable
fun TemperatureResponseCard(
    level: TemperatureLevel,
    response: TemperatureResponse?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val cardColor = when (level) {
        TemperatureLevel.LOW -> TemperatureColors.LowCardColor
        TemperatureLevel.MEDIUM -> TemperatureColors.MediumCardColor
        TemperatureLevel.HIGH -> TemperatureColors.HighCardColor
    }

    val accentColor = when (level) {
        TemperatureLevel.LOW -> TemperatureColors.LowTemperatureColor
        TemperatureLevel.MEDIUM -> TemperatureColors.MediumTemperatureColor
        TemperatureLevel.HIGH -> TemperatureColors.HighTemperatureColor
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Temperature ${level.value}",
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = level.displayName,
                        color = TemperatureColors.TextSecondaryColor,
                        fontSize = 12.sp
                    )
                }

                // Stats badge
                if (response != null && response.isSuccess) {
                    Surface(
                        color = accentColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${response.tokenUsage.completionTokens} tokens",
                            color = accentColor,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            color = accentColor,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    response?.error != null -> {
                        Text(
                            text = "Error: ${response.error}",
                            color = TemperatureColors.ErrorColor,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    response?.content?.isNotBlank() == true -> {
                        Text(
                            text = response.content,
                            color = TemperatureColors.TextColor,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        )
                    }
                    else -> {
                        Text(
                            text = "Waiting for comparison...",
                            color = TemperatureColors.TextSecondaryColor,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }

            // Footer stats
            if (response != null && response.isSuccess) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${response.durationMs}ms",
                        color = TemperatureColors.TextSecondaryColor,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${response.wordCount} words",
                        color = TemperatureColors.TextSecondaryColor,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
