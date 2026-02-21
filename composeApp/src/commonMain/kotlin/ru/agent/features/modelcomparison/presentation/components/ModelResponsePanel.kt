package ru.agent.features.modelcomparison.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.agent.features.modelcomparison.domain.model.ModelResponse
import ru.agent.features.modelcomparison.domain.model.ModelType
import ru.agent.features.modelcomparison.presentation.theme.ModelComparisonColors
import ru.agent.features.modelcomparison.presentation.utils.formatCostPrecise

/**
 * Card component for displaying a single model response
 */
@Composable
fun ModelResponsePanel(
    modelType: ModelType,
    response: ModelResponse?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onOpenLink: ((String) -> Unit)? = null
) {
    val cardColor = when (modelType) {
        ModelType.WEAK -> ModelComparisonColors.WeakCardColor
        ModelType.MEDIUM -> ModelComparisonColors.MediumCardColor
        ModelType.STRONG -> ModelComparisonColors.StrongCardColor
    }

    val accentColor = when (modelType) {
        ModelType.WEAK -> ModelComparisonColors.WeakModelColor
        ModelType.MEDIUM -> ModelComparisonColors.MediumModelColor
        ModelType.STRONG -> ModelComparisonColors.StrongModelColor
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
            // Header with model info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Model tier name
                    Text(
                        text = "${modelType.displayName} Model",
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    // Model ID with HuggingFace link
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        ModelLinkText(
                            modelId = modelType.huggingFaceId,
                            url = modelType.huggingFaceUrl,
                            accentColor = accentColor,
                            onOpenLink = onOpenLink
                        )
                    }

                    // Model parameters
                    Text(
                        text = "${modelType.parameters} parameters • ${modelType.architecture}",
                        color = ModelComparisonColors.TextSecondaryColor,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 2.dp)
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
                            color = ModelComparisonColors.ErrorColor,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    response?.content?.isNotBlank() == true -> {
                        Text(
                            text = response.content,
                            color = ModelComparisonColors.TextColor,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        )
                    }
                    else -> {
                        Text(
                            text = "Waiting for comparison...",
                            color = ModelComparisonColors.TextSecondaryColor,
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
                        color = ModelComparisonColors.MetricsTimeColor,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "\$${formatCostPrecise(response.estimatedCost)}",
                        color = ModelComparisonColors.MetricsCostColor,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${response.wordCount} words",
                        color = ModelComparisonColors.TextSecondaryColor,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

/**
 * Clickable model ID text that links to HuggingFace
 */
@Composable
private fun ModelLinkText(
    modelId: String,
    url: String,
    accentColor: androidx.compose.ui.graphics.Color,
    onOpenLink: ((String) -> Unit)?
) {
    val annotatedString = buildAnnotatedString {
        pushStringAnnotation(tag = "URL", annotation = url)
        withStyle(
            style = SpanStyle(
                color = accentColor,
                fontSize = 11.sp,
                textDecoration = TextDecoration.Underline
            )
        ) {
            append(modelId)
        }
        pop()
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(
                enabled = onOpenLink != null,
                role = Role.Button,
                onClick = { onOpenLink?.invoke(url) }
            )
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = annotatedString,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "↗",
            color = accentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
