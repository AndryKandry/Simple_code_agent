package ru.agent.features.chat.domain.model

/**
 * Domain model representing a summary of multiple messages.
 *
 * Used for context compression to replace old messages with
 * a condensed summary while preserving key information.
 */
data class MessageSummary(
    val id: String,
    val sessionId: String,
    val summary: String,
    val startMessageId: String,
    val endMessageId: String,
    val messageCount: Int,
    val createdAt: Long,
    val tokenCount: Int
) {
    /**
     * Calculate token savings compared to original messages.
     * Assumes average 4 characters per token.
     */
    fun calculateTokenSavings(averageCharsPerToken: Int = 4): Int {
        // This is an estimate - actual savings depend on message content
        return messageCount * 50 - tokenCount // Rough estimate
    }
}
