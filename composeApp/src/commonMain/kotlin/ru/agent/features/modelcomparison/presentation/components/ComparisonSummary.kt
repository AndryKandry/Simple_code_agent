package ru.agent.features.modelcomparison.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.agent.features.modelcomparison.presentation.theme.ModelComparisonColors

/**
 * Panel displaying comparison analysis summary
 */
@Composable
fun ComparisonSummary(
    analysis: String?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = ModelComparisonColors.SurfaceColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Comparison Analysis",
                color = ModelComparisonColors.TextColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                isLoading -> {
                    Text(
                        text = "Generating analysis...",
                        color = ModelComparisonColors.TextSecondaryColor,
                        fontSize = 14.sp
                    )
                }
                analysis != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = analysis,
                            color = ModelComparisonColors.TextColor,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
                else -> {
                    Text(
                        text = "Click 'Analyze' to generate a comparison summary",
                        color = ModelComparisonColors.TextSecondaryColor,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
