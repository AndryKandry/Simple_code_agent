package ru.agent.features.chat.presentation.comparison

import ru.agent.features.chat.domain.model.ContextStrategy
import ru.agent.features.chat.domain.model.ExportFormat
import ru.agent.features.chat.domain.model.Message

/**
 * State for a single chat in comparison mode.
 */
data class ComparisonChatState(
    val sessionId: String = "",
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val tokenCount: Int = 0,
    val totalResponseTimeMs: Long = 0L,
    val messageCount: Int = 0,
    val error: String? = null
)

/**
 * State for Comparison Mode UI.
 */
data class ComparisonState(
    val isInitialized: Boolean = false,
    val isInitializing: Boolean = false,
    val isSending: Boolean = false,
    val isExporting: Boolean = false,

    val currentMessage: String = "",

    // Individual chat states for each strategy
    val chatStates: Map<ContextStrategy, ComparisonChatState> = emptyMap(),

    val selectedStrategies: Set<ContextStrategy> = ContextStrategy.entries.toSet(),
    val exportFormat: ExportFormat = ExportFormat.MARKDOWN,

    val error: String? = null,
    val successMessage: String? = null
) {
    // Helper to get chat state for a specific strategy
    fun getChatState(strategy: ContextStrategy): ComparisonChatState {
        return chatStates[strategy] ?: ComparisonChatState()
    }

    // Check if any chat is loading
    fun isAnyLoading(): Boolean {
        return chatStates.values.any { it.isLoading }
    }

    // Get total stats across all strategies
    fun getTotalStats(): ComparisonTotalStats {
        return ComparisonTotalStats(
            totalMessages = chatStates.values.sumOf { it.messages.size },
            totalTokens = chatStates.values.sumOf { it.tokenCount },
            averageResponseTime = if (chatStates.isNotEmpty()) {
                chatStates.values.sumOf { it.totalResponseTimeMs } / chatStates.size
            } else 0L
        )
    }
}

/**
 * Total statistics across all strategies.
 */
data class ComparisonTotalStats(
    val totalMessages: Int,
    val totalTokens: Int,
    val averageResponseTime: Long
)
