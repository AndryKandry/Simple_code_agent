package ru.agent.features.chat.domain.optimization

import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.repository.SummaryApi

/**
 * Generates summaries of conversation history using injected SummaryApi.
 *
 * Used for context compression to reduce token count while
 * preserving key information from old messages.
 *
 * Follows Clean Architecture: depends on domain abstraction (SummaryApi),
 * not on data layer implementations.
 */
class SummaryGenerator(
    private val summaryApi: SummaryApi,
    private val summaryMaxTokens: Int = DEFAULT_SUMMARY_MAX_TOKENS
) {

    companion object {
        const val DEFAULT_SUMMARY_MAX_TOKENS = 200
    }

    /**
     * Generate a summary for a list of messages.
     *
     * @param messages Messages to summarize
     * @return Generated summary text or null if generation fails
     */
    suspend fun generateSummary(messages: List<Message>): String? {
        return summaryApi.generateSummary(
            messages = messages,
            maxTokens = summaryMaxTokens
        )
    }

    /**
     * Estimate if summary generation is worthwhile.
     * Returns true if summarizing will likely save tokens.
     */
    fun shouldGenerateSummary(messages: List<Message>, threshold: Int = 3): Boolean {
        // Don't summarize if too few messages
        if (messages.size < threshold) {
            return false
        }

        // Estimate current token count
        val estimatedTokens = messages.sumOf { msg ->
            ToonEncoder.estimateTokens(msg.content)
        }

        // Only summarize if we'll save tokens
        return estimatedTokens > summaryMaxTokens
    }
}
