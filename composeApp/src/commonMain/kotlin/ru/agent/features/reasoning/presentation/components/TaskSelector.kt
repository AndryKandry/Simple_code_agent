package ru.agent.features.reasoning.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.agent.features.reasoning.presentation.theme.ReasoningColors

/**
 * Simple input field for entering a question
 */
@Composable
fun TaskSelector(
    inputText: String,
    onInputTextChanged: (String) -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                ReasoningColors.InputFieldBackground,
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = "Enter your question:",
            style = MaterialTheme.typography.labelMedium,
            color = ReasoningColors.LabelColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = inputText,
            onValueChange = onInputTextChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Enter a question to compare reasoning methods...",
                    color = ReasoningColors.HintTextColor
                )
            },
            enabled = isEnabled,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ReasoningColors.TextColor,
                unfocusedTextColor = ReasoningColors.TextColor,
                focusedBorderColor = ReasoningColors.InputFieldFocusedBorderColor,
                unfocusedBorderColor = ReasoningColors.InputFieldBorderColor,
                cursorColor = ReasoningColors.InputFieldFocusedBorderColor
            ),
            shape = RoundedCornerShape(8.dp),
            minLines = 2,
            maxLines = 4
        )
    }
}
