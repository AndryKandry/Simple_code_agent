package ru.agent.features.modelcomparison.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.agent.features.modelcomparison.domain.model.ModelType
import ru.agent.features.modelcomparison.presentation.theme.ModelComparisonColors

/**
 * Input field with model selection and compare button
 */
@Composable
fun ModelInputField(
    text: String,
    onTextChanged: (String) -> Unit,
    onCompareClicked: () -> Unit,
    selectedModels: Set<ModelType>,
    onModelToggle: (ModelType) -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Model selection chips
        Text(
            text = "Select Models:",
            color = ModelComparisonColors.TextSecondaryColor,
            style = MaterialTheme.typography.labelSmall
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModelType.entries.forEach { modelType ->
                val chipColor = when (modelType) {
                    ModelType.WEAK -> ModelComparisonColors.WeakModelColor
                    ModelType.MEDIUM -> ModelComparisonColors.MediumModelColor
                    ModelType.STRONG -> ModelComparisonColors.StrongModelColor
                }

                FilterChip(
                    selected = modelType in selectedModels,
                    onClick = { onModelToggle(modelType) },
                    label = {
                        Text(
                            text = modelType.displayName,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipColor.copy(alpha = 0.3f),
                        selectedLabelColor = chipColor
                    ),
                    modifier = Modifier.padding(2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input field
        OutlinedTextField(
            value = text,
            onValueChange = onTextChanged,
            placeholder = {
                Text(
                    "Enter your prompt to compare models...",
                    color = ModelComparisonColors.TextSecondaryColor
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = isEnabled,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ModelComparisonColors.MediumModelColor,
                unfocusedBorderColor = ModelComparisonColors.SurfaceVariantColor,
                cursorColor = ModelComparisonColors.MediumModelColor,
                focusedTextColor = ModelComparisonColors.TextColor,
                unfocusedTextColor = ModelComparisonColors.TextColor,
                disabledTextColor = ModelComparisonColors.TextSecondaryColor
            ),
            shape = RoundedCornerShape(8.dp),
            minLines = 2,
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onCompareClicked,
                enabled = isEnabled && text.isNotBlank() && selectedModels.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ModelComparisonColors.MediumModelColor,
                    disabledContainerColor = ModelComparisonColors.ButtonDisabledColor
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "Compare Models (Ctrl+Enter)",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            OutlinedButton(
                onClick = { onTextChanged("") },
                enabled = isEnabled,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ModelComparisonColors.TextColor
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Clear", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
