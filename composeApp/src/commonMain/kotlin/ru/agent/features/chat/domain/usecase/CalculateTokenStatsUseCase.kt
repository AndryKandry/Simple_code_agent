package ru.agent.features.chat.domain.usecase

import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.model.ModelLimits
import ru.agent.features.chat.domain.model.TokenStats
import ru.agent.features.chat.domain.optimization.ToonEncoder

/**
 * UseCase for calculating token statistics for the chat.
 *
 * Provides token counts for:
 * - Current input text (estimated)
 * - Conversation history
 * - Last API response
 * - Total usage
 */
class CalculateTokenStatsUseCase {

    /**
     * Calculate token statistics.
     *
     * @param inputText Current text in the input field
     * @param messages List of messages in the conversation
     * @return TokenStats with calculated values
     */
    operator fun invoke(
        inputText: String,
        messages: List<Message>
    ): TokenStats {
        // Calculate input tokens
        val inputTokens = if (inputText.isNotBlank()) {
            ToonEncoder.estimateTokens(inputText)
        } else {
            0
        }

        // Calculate history tokens from messages that have tokenCount stored
        // For messages without tokenCount, estimate using ToonEncoder
        val historyTokens = messages.sumOf { message ->
            message.tokenCount ?: estimateMessageTokens(message)
        }

        // Get last response tokens from the last assistant message
        val lastResponseTokens = messages
            .lastOrNull { it.tokenCount != null }
            ?.tokenCount ?: 0

        // Total tokens = history + input (for sending new message)
        // Note: We don't double-count lastResponseTokens as it's already in historyTokens
        val totalTokens = historyTokens + inputTokens

        return TokenStats(
            inputTokens = inputTokens,
            historyTokens = historyTokens,
            lastResponseTokens = lastResponseTokens,
            totalTokens = totalTokens
        )
    }

    /**
     * Estimate tokens for a single message using TOON encoding.
     */
    private fun estimateMessageTokens(message: Message): Int {
        val toonString = ToonEncoder.encode(listOf(message))
        return ToonEncoder.estimateTokens(toonString)
    }

    /**
     * Get warning message based on token usage.
     *
     * @param stats Current token statistics
     * @return Warning message or null if no warning needed
     */
    fun getWarningMessage(stats: TokenStats): String? {
        val maxTokens = ModelLimits.DEFAULT_CONTEXT_LIMIT

        return when {
            stats.isCriticalLevel(maxTokens) -> {
                val remaining = stats.getRemainingTokens(maxTokens)
                "Critical: Only ${ModelLimits.formatTokenCount(remaining)} tokens remaining. " +
                "Consider starting a new chat or clearing history."
            }
            stats.isWarningLevel(maxTokens) -> {
                val percentage = stats.getUsagePercentage(maxTokens).toInt()
                val remaining = stats.getRemainingTokens(maxTokens)
                "Warning: ${percentage}% of context used. " +
                "${ModelLimits.formatTokenCount(remaining)} tokens remaining."
            }
            else -> null
        }
    }

    /**
     * Check if token usage requires showing a warning to the user.
     */
    fun shouldShowWarning(stats: TokenStats): Boolean {
        return stats.isWarningLevel(ModelLimits.DEFAULT_CONTEXT_LIMIT)
    }
}
