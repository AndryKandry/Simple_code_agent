package ru.agent.features.chat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

// Тёмные цвета для input field
private val InputFieldBackground = Color(0xFF2A2A3E)
private val InputFieldBorder = Color(0xFF4A4A6A)
private val InputFieldText = Color.White
private val InputFieldHint = Color.White.copy(alpha = 0.5f)
private val SendButtonColor = Color(0xFFE91E63)
private val SendButtonDisabledColor = Color(0xFF4A4A6A)

@Composable
fun ChatInputField(
    text: String,
    onTextChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E2E))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChanged,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    text = "Type a message...",
                    color = InputFieldHint
                )
            },
            enabled = isEnabled,
            maxLines = 5,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (text.isNotBlank() && isEnabled) {
                        onSendClicked()
                    }
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = InputFieldText,
                unfocusedTextColor = InputFieldText,
                disabledTextColor = InputFieldHint,
                focusedContainerColor = InputFieldBackground,
                unfocusedContainerColor = InputFieldBackground,
                disabledContainerColor = InputFieldBackground.copy(alpha = 0.5f),
                focusedBorderColor = InputFieldBorder,
                unfocusedBorderColor = InputFieldBorder.copy(alpha = 0.5f),
                disabledBorderColor = InputFieldBorder.copy(alpha = 0.3f),
                cursorColor = SendButtonColor
            ),
            shape = RoundedCornerShape(24.dp)
        )

        TextButton(
            onClick = onSendClicked,
            enabled = isEnabled && text.isNotBlank()
        ) {
            Text(
                text = "Send",
                color = if (isEnabled && text.isNotBlank()) {
                    SendButtonColor
                } else {
                    SendButtonDisabledColor
                }
            )
        }
    }
}
