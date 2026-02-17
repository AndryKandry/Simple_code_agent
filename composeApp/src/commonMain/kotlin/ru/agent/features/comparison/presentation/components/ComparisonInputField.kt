package ru.agent.features.comparison.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import ru.agent.features.comparison.presentation.theme.ComparisonColors

/**
 * Input field for API comparison queries
 */
@Composable
fun ComparisonInputField(
    text: String,
    onTextChanged: (String) -> Unit,
    onCompareClicked: () -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                ComparisonColors.InputFieldBackground,
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                placeholder = {
                    Text(
                        text = "Enter your query to compare responses...",
                        color = ComparisonColors.HintTextColor
                    )
                },
                enabled = isEnabled,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (text.isNotBlank() && isEnabled) {
                            onCompareClicked()
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ComparisonColors.TextColor,
                    unfocusedTextColor = ComparisonColors.TextColor,
                    focusedBorderColor = ComparisonColors.InputFieldFocusedBorderColor,
                    unfocusedBorderColor = ComparisonColors.InputFieldBorderColor,
                    cursorColor = ComparisonColors.InputFieldFocusedBorderColor
                ),
                shape = RoundedCornerShape(8.dp),
                maxLines = 3
            )

            Button(
                onClick = onCompareClicked,
                enabled = isEnabled && text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ComparisonColors.ButtonColor,
                    disabledContainerColor = ComparisonColors.ButtonDisabledColor
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Compare",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        if (!isEnabled) {
            Text(
                text = "Comparing responses...",
                style = MaterialTheme.typography.bodySmall,
                color = ComparisonColors.HintTextColor,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
