package ru.agent.features.chat.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.model.SenderType
import ru.agent.features.chat.domain.split.SplitResult
import ru.agent.features.chat.domain.usecase.SplitMessageUseCase
import ru.agent.features.chat.presentation.models.ChatEvent

@Composable
fun MessageList(
    messages: List<Message>,
    modifier: Modifier = Modifier,
    expandedBatches: Set<String> = emptySet(),
    currentBatchPart: Map<String, Int> = emptyMap(),
    onEvent: (ChatEvent) -> Unit = {},
    splitMessageUseCase: SplitMessageUseCase = koinInject()
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (messages.isEmpty()) {
        EmptyChatState(modifier = modifier)
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(
                items = messages,
                key = { it.id }
            ) { message ->
                // Split long messages for display using UseCase
                val splitResult = remember(message.id, message.content) {
                    splitMessageUseCase(message.content)
                }

                val isExpanded = splitResult.batchId in expandedBatches
                // Validate currentPart is within bounds
                val currentPart = (currentBatchPart[splitResult.batchId] ?: 1).coerceIn(1, splitResult.totalParts)

                SplitMessageItem(
                    message = message,
                    splitResult = splitResult,
                    isExpanded = isExpanded,
                    currentPart = currentPart,
                    onToggleExpansion = {
                        onEvent(ChatEvent.ToggleBatchExpansion(splitResult.batchId))
                    },
                    onNavigatePart = { direction ->
                        onEvent(ChatEvent.NavigateBatchPart(splitResult.batchId, direction, splitResult.totalParts))
                    },
                    modifier = Modifier.animateItem(
                        placementSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                )
            }
        }
    }
}

/**
 * Component for displaying a message with split support.
 */
@Composable
private fun SplitMessageItem(
    message: Message,
    splitResult: SplitResult,
    isExpanded: Boolean,
    currentPart: Int,
    onToggleExpansion: () -> Unit,
    onNavigatePart: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (message.senderType == SenderType.USER) {
            Alignment.End
        } else {
            Alignment.Start
        }
    ) {
        // Validate currentPart and determine display content
        val validCurrentPart = currentPart.coerceIn(1, splitResult.totalParts)
        val displayContent = if (isExpanded) {
            splitResult.originalContent
        } else {
            val part = splitResult.parts.getOrNull(validCurrentPart - 1)
            part?.content ?: splitResult.originalContent
        }

        // Display message bubble
        MessageItem(
            message = message.copy(content = displayContent),
            modifier = Modifier
        )

        // Show navigation controls for split messages (collapsed mode)
        if (splitResult.needsSplit && !isExpanded) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous button
                IconButton(
                    onClick = { onNavigatePart(-1) },
                    enabled = validCurrentPart > 1,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color.White,
                        disabledContentColor = Color.White.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous part"
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Part indicator
                Text(
                    text = "$validCurrentPart/${splitResult.totalParts}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Next button
                IconButton(
                    onClick = { onNavigatePart(1) },
                    enabled = validCurrentPart < splitResult.totalParts,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color.White,
                        disabledContentColor = Color.White.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next part"
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Show all button
                TextButton(onClick = onToggleExpansion) {
                    Text(
                        text = "Show all",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Show collapse button for expanded messages
        if (splitResult.needsSplit && isExpanded) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(onClick = onToggleExpansion) {
                    Text(
                        text = "Show by parts",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyChatState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Start a conversation",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "with DeepSeek AI assistant",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}
