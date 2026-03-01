package ru.agent.features.chat.domain.repository

import ru.agent.features.chat.domain.model.Message

/**
 * API interface for generating conversation summaries.
 *
 * This abstraction allows the domain layer to request summaries
 * without depending on specific API implementations (DeepSeek, OpenAI, etc.).
 *
 * Follows Clean Architecture: domain depends on abstractions, not implementations.
 */
interface SummaryApi {

    /**
     * Generate a summary for a list of conversation messages.
     *
     * @param messages Messages to summarize
     * @param maxTokens Maximum tokens for the generated summary
     * @return Generated summary text or null if generation fails
     */
    suspend fun generateSummary(
        messages: List<Message>,
        maxTokens: Int = DEFAULT_MAX_TOKENS
    ): String?

    companion object {
        const val DEFAULT_MAX_TOKENS = 200
    }
}
