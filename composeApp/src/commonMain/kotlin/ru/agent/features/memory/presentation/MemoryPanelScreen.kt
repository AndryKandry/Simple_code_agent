package ru.agent.features.memory.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.model.SenderType
import ru.agent.features.memory.domain.model.ContextAnchor
import ru.agent.features.memory.domain.model.ExecutionState
import ru.agent.features.memory.domain.model.KnowledgeEntry
import ru.agent.features.memory.domain.model.TaskInfo
import ru.agent.features.memory.presentation.models.MemoryEvent
import ru.agent.features.memory.presentation.models.MemoryState

@Composable
fun MemoryPanelScreen(
    state: MemoryState,
    onEvent: (MemoryEvent) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
        MemoryPanelHeader(
            isOpen = state.isMemoryPanelOpen,
            onToggle = { onEvent(MemoryEvent.ToggleMemoryPanelVisibility) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Memory Stats Overview
        MemoryStatsCard(
            stmMessageCount = state.lastMessages.size,
            activeTask = state.activeTask,
            knowledgeCount = state.searchResults.size,
            anchorsCount = state.activeAnchors.size
        )

        Spacer(modifier = Modifier.height(16.dp))

        // STM Section
        StmSection(
            messages = state.lastMessages.takeLast(5),
            onClear = { onEvent(MemoryEvent.ClearMemoryPanel(state.sessionId ?: "")) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Working Memory Section
        if (state.activeTask != null) {
            WorkingMemoryCard(
                taskInfo = state.activeTask,
                executionState = state.executionState,
                onCancel = { onEvent(MemoryEvent.ClearMemoryPanel(state.sessionId ?: "")) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Knowledge Base Section
        KnowledgeBaseSection(
            entries = state.searchResults,
            searchQuery = state.searchQuery,
            onSearchQueryChange = { onEvent(MemoryEvent.SearchKnowledge(it)) },
            onAddEntry = { entry -> onEvent(MemoryEvent.SaveToKnowledge(entry)) }
        )
    }
}

@Composable
private fun MemoryPanelHeader(
    isOpen: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Memory Context",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onToggle) {
            Text(if (isOpen) "Close" else "Open")
        }
    }
}

@Composable
private fun MemoryStatsCard(
    stmMessageCount: Int,
    activeTask: TaskInfo?,
    knowledgeCount: Int,
    anchorsCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Memory Overview",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("STM", stmMessageCount.toString())
                StatItem("Knowledge", knowledgeCount.toString())
                StatItem("Anchors", anchorsCount.toString())
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun StmSection(
    messages: List<Message>,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Short-term Memory",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(onClick = onClear) {
                    Text("Clear")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (messages.isEmpty()) {
                Text(
                    text = "No messages in STM",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height(150.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = messages,
                        key = { it.id }
                    ) { message ->
                        StmMessageItem(message = message)
                    }
                }
            }
        }
    }
}

@Composable
private fun StmMessageItem(message: Message) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (message.senderType == SenderType.USER)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.secondaryContainer
            )
            .padding(8.dp)
    ) {
        Column {
            Text(
                text = if (message.senderType == SenderType.USER) "You" else "Assistant",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message.content.take(100) + if (message.content.length > 100) "..." else "",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun WorkingMemoryCard(
    taskInfo: TaskInfo,
    executionState: ExecutionState,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Working Memory",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(onClick = onCancel) {
                    Text("Cancel Task")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Task: ${taskInfo.description}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Type: ${taskInfo.taskType.name}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Status: ${taskInfo.status.name}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Progress: ${(taskInfo.progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "State: ${executionState.name}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun KnowledgeBaseSection(
    entries: List<KnowledgeEntry>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddEntry: (KnowledgeEntry) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Knowledge Base",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("Search knowledge...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (entries.isEmpty()) {
                Text(
                    text = "No knowledge entries found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
