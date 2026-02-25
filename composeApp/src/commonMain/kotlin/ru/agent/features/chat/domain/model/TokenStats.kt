package ru.agent.features.chat.domain.model

/**
 * Statistics about token usage in the chat.
 *
 * @property inputTokens Estimated tokens for the current input text
 * @property historyTokens Total tokens used in the conversation history
 * @property lastResponseTokens Tokens used in the last API response (from usage.promptTokens + usage.completionTokens)
 * @property totalTokens Total tokens including input, history, and response
 */
data class TokenStats(
    val inputTokens: Int = 0,
    val historyTokens: Int = 0,
    val lastResponseTokens: Int = 0,
    val totalTokens: Int = 0
) {
    /**
     * Calculate usage percentage relative to the model's context limit.
     *
     * @param maxContextTokens Maximum context tokens for the model
     * @return Usage percentage (0-100+)
     */
    fun getUsagePercentage(maxContextTokens: Int): Float {
        if (maxContextTokens <= 0) return 0f
        return (totalTokens.toFloat() / maxContextTokens) * 100f
    }

    /**
     * Check if token usage is at warning level (80-94%).
     */
    fun isWarningLevel(maxContextTokens: Int): Boolean {
        val percentage = getUsagePercentage(maxContextTokens)
        return percentage >= 80f && percentage < 95f
    }

    /**
     * Check if token usage is at critical level (95%+).
     */
    fun isCriticalLevel(maxContextTokens: Int): Boolean {
        return getUsagePercentage(maxContextTokens) >= 95f
    }

    /**
     * Get remaining tokens available.
     */
    fun getRemainingTokens(maxContextTokens: Int): Int {
        return (maxContextTokens - totalTokens).coerceAtLeast(0)
    }

    companion object {
        val Empty = TokenStats()
    }
}
