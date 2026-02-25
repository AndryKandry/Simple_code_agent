package ru.agent.features.chat.presentation.models

import ru.agent.features.chat.domain.model.ChatSession
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.model.TokenStats

data class ChatViewState(
    val currentSessionId: String? = null,
    val currentSession: ChatSession? = null,
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val inputText: String = "",
    val isSidebarOpen: Boolean = true,
    val sessions: List<ChatSession> = emptyList(),
    val isLoadingSessions: Boolean = false,
    // Token statistics
    val tokenStats: TokenStats = TokenStats.Empty,
    val showTokenWarning: Boolean = false,
    val tokenWarningMessage: String? = null,
    // Batch message navigation state (for split long messages)
    val expandedBatches: Set<String> = emptySet(),      // Раскрытые batch (показывать весь текст)
    val currentBatchPart: Map<String, Int> = emptyMap() // Текущая часть для каждого batch
)
