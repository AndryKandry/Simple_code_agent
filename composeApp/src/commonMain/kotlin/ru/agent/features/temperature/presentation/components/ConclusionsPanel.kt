package ru.agent.features.temperature.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import ru.agent.features.temperature.domain.model.TemperatureRecommendation
import ru.agent.features.temperature.presentation.theme.TemperatureColors

/**
 * Panel displaying conclusions and recommendations
 */
@Composable
fun ConclusionsPanel(
    recommendations: List<TemperatureRecommendation>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = TemperatureColors.SurfaceColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Conclusions & Recommendations",
                color = TemperatureColors.TextColor,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            recommendations.forEach { recommendation ->
                RecommendationItem(recommendation)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun RecommendationItem(
    recommendation: TemperatureRecommendation
) {
    val accentColor = when (recommendation.level) {
        TemperatureLevel.LOW -> TemperatureColors.LowTemperatureColor
        TemperatureLevel.MEDIUM -> TemperatureColors.MediumTemperatureColor
        TemperatureLevel.HIGH -> TemperatureColors.HighTemperatureColor
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = accentColor,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.size(8.dp)
            ) {}
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Temperature ${recommendation.level.value} - ${recommendation.level.displayName}",
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Best for: ${recommendation.bestFor.take(3).joinToString(", ")}",
            color = TemperatureColors.TextSecondaryColor,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}
