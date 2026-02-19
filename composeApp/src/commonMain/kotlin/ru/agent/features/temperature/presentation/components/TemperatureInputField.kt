package ru.agent.features.temperature.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.agent.features.temperature.presentation.theme.TemperatureColors

/**
 * Input field with compare button for temperature comparison
 */
@Composable
fun TemperatureInputField(
    text: String,
    onTextChanged: (String) -> Unit,
    onCompareClicked: () -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChanged,
            placeholder = {
                Text(
                    "Enter your prompt to compare temperatures...",
                    color = TemperatureColors.TextSecondaryColor
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = isEnabled,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TemperatureColors.MediumTemperatureColor,
                unfocusedBorderColor = TemperatureColors.SurfaceVariantColor,
                cursorColor = TemperatureColors.MediumTemperatureColor,
                focusedTextColor = TemperatureColors.TextColor,
                unfocusedTextColor = TemperatureColors.TextColor,
                disabledTextColor = TemperatureColors.TextSecondaryColor
            ),
            shape = RoundedCornerShape(8.dp),
            minLines = 2,
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onCompareClicked,
                enabled = isEnabled && text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TemperatureColors.MediumTemperatureColor,
                    disabledContainerColor = TemperatureColors.ButtonDisabledColor
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "Compare Temperatures (Ctrl+Enter)",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            OutlinedButton(
                onClick = { onTextChanged("") },
                enabled = isEnabled,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TemperatureColors.TextColor
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Clear", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
