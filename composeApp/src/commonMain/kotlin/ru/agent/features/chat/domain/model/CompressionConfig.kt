package ru.agent.features.chat.domain.model

/**
 * Configuration for context compression.
 *
 * Defines parameters for how messages should be compressed
 * to fit within token limits.
 */
data class CompressionConfig(
    val keepRecentMessages: Int = 10,
    val summaryChunkSize: Int = 10,
    val maxTokens: Int = 4000,
    val summaryMaxTokens: Int = 200,
    val enableSummaryGeneration: Boolean = true,
    val fallbackToTruncation: Boolean = true
) {
    companion object {
        val DEFAULT = CompressionConfig()

        // For testing or limited contexts
        val CONSERVATIVE = CompressionConfig(
            keepRecentMessages = 15,
            summaryChunkSize = 8,
            maxTokens = 3000,
            summaryMaxTokens = 150,
            enableSummaryGeneration = true,
            fallbackToTruncation = true
        )

        // For larger context windows
        val AGGRESSIVE = CompressionConfig(
            keepRecentMessages = 5,
            summaryChunkSize = 15,
            maxTokens = 8000,
            summaryMaxTokens = 300,
            enableSummaryGeneration = true,
            fallbackToTruncation = true
        )
    }

    /**
     * Check if compression is needed for given message count.
     */
    fun needsCompression(messageCount: Int): Boolean {
        return messageCount > keepRecentMessages
    }

    /**
     * Calculate how many messages should be summarized.
     */
    fun getSummaryMessageCount(totalMessages: Int): Int {
        return maxOf(0, totalMessages - keepRecentMessages)
    }
}
