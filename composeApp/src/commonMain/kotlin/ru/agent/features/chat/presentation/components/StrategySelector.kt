package ru.agent.features.chat.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.agent.features.chat.domain.model.ContextStrategy

/**
 * Dropdown selector for context management strategies.
 *
 * @param selectedStrategy Currently selected strategy
 * @param availableStrategies List of available strategies (null = all available)
 * @param onStrategySelected Callback when a strategy is selected
 * @param modifier Modifier for the component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrategySelector(
    selectedStrategy: ContextStrategy,
    availableStrategies: List<ContextStrategy>? = null,
    onStrategySelected: (ContextStrategy) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val strategies = availableStrategies ?: ContextStrategy.entries

    Box(
        modifier = modifier
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.width(180.dp)
        ) {
            OutlinedTextField(
                value = selectedStrategy.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Strategy") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.White.copy(alpha = 0.5f),
                    unfocusedIndicatorColor = Color.White.copy(alpha = 0.3f),
                    focusedLabelColor = Color.White.copy(alpha = 0.7f),
                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                    cursorColor = Color.White
                ),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                strategies.forEach { strategy ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = strategy.displayName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = strategy.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            onStrategySelected(strategy)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}

/**
 * Compact strategy indicator without dropdown.
 * Used when space is limited.
 *
 * @param strategy Current strategy
 * @param modifier Modifier for the component
 */
@Composable
fun StrategyIndicator(
    strategy: ContextStrategy,
    modifier: Modifier = Modifier
) {
    Text(
        text = strategy.displayName,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White.copy(alpha = 0.7f),
        modifier = modifier
    )
}
