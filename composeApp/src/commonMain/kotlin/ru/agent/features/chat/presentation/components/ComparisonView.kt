package ru.agent.features.chat.presentation.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.agent.features.chat.domain.model.ContextStrategy
import ru.agent.features.chat.domain.model.ExportFormat
import ru.agent.features.chat.presentation.comparison.ComparisonEvent
import ru.agent.features.chat.presentation.comparison.ComparisonState
import ru.agent.features.chat.presentation.theme.ChatColors

/**
 * Main view for Comparison Mode.
 *
 * Displays 3 chat columns (one for each strategy) with side-by-side comparison.
 */
@Composable
fun ComparisonView(
    state: ComparisonState,
    onEvent: (ComparisonEvent) -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(ChatColors.BackgroundStartColor, ChatColors.BackgroundEndColor)
                )
            )
    ) {
        // Header
        ComparisonHeader(
            isLoading = state.isSending || state.isInitializing,
            isExporting = state.isExporting,
            hasChats = state.chatStates.isNotEmpty(),
            onBack = onBack,
            onExport = { format -> onEvent(ComparisonEvent.ExportResults(format)) },
            onCancel = { onEvent(ComparisonEvent.CancelComparison) },
            onClear = { onEvent(ComparisonEvent.ClearResults) }
        )

        // Strategy Selection
        StrategySelectionRow(
            selectedStrategies = state.selectedStrategies,
            onToggleStrategy = { strategy -> onEvent(ComparisonEvent.ToggleStrategy(strategy)) },
            enabled = !state.isSending
        )

        // Loading indicator
        if (state.isInitializing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Chat Columns
        if (state.isInitialized) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.selectedStrategies.sortedBy { it.ordinal }.forEach { strategy ->
                    val chatState = state.getChatState(strategy)

                    ComparisonColumn(
                        strategy = strategy,
                        chatState = chatState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    )
                }
            }
        } else if (!state.isInitializing) {
            // Error state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Failed to initialize comparison sessions",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = { /* Retry would go here */ }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry")
                    }
                }
            }
        }

        // Input Area
        ComparisonInputArea(
            text = state.currentMessage,
            isLoading = state.isSending || state.isInitializing,
            onTextChanged = { text -> onEvent(ComparisonEvent.MessageChanged(text)) },
            onSend = { onEvent(ComparisonEvent.SendMessage) }
        )
    }
}

/**
 * Header with title and action buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComparisonHeader(
    isLoading: Boolean,
    isExporting: Boolean,
    hasChats: Boolean,
    onBack: () -> Unit,
    onExport: (ExportFormat) -> Unit,
    onCancel: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Comparison Mode",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.weight(1f))

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isLoading) {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop")
                    }
                }

                if (hasChats) {
                    OutlinedButton(
                        onClick = { onExport(ExportFormat.MARKDOWN) },
                        enabled = !isExporting
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export")
                    }

                    TextButton(onClick = onClear) {
                        Text("Clear")
                    }
                }
            }
        }
    }
}

/**
 * Row with strategy selection chips.
 */
@Composable
private fun StrategySelectionRow(
    selectedStrategies: Set<ContextStrategy>,
    onToggleStrategy: (ContextStrategy) -> Unit,
    enabled: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Strategies:",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            ContextStrategy.entries.forEach { strategy ->
                FilterChip(
                    selected = selectedStrategies.contains(strategy),
                    onClick = { onToggleStrategy(strategy) },
                    label = { Text(strategy.displayName) },
                    enabled = enabled
                )
            }
        }
    }
}

/**
 * Input area with text field and send button.
 */
@Composable
private fun ComparisonInputArea(
    text: String,
    isLoading: Boolean,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Send message to all strategies...")
                },
                enabled = !isLoading,
                maxLines = 3,
                shape = RoundedCornerShape(24.dp)
            )

            Button(
                onClick = onSend,
                enabled = text.isNotBlank() && !isLoading,
                shape = RoundedCornerShape(24.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .width(16.dp)
                            .height(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isLoading) "Sending..." else "Send to All")
            }
        }
    }
}
