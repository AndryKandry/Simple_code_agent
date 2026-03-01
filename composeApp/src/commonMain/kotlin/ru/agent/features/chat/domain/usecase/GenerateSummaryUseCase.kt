package ru.agent.features.chat.domain.usecase

import co.touchlab.kermit.Logger
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.model.MessageSummary
import ru.agent.features.chat.domain.optimization.SummaryGenerator
import ru.agent.features.chat.domain.optimization.ToonEncoder
import ru.agent.features.chat.domain.repository.MessageSummaryRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Use case for generating a summary of chat history.
 *
 * Explicitly generates a summary for a session's messages,
 * useful for manual summarization or regeneration.
 *
 * Note: Takes messages as parameter to avoid circular dependency with ChatRepository.
 */
class GenerateSummaryUseCase(
    private val summaryGenerator: SummaryGenerator,
    private val messageSummaryRepository: MessageSummaryRepository
) {

    private val logger = Logger.withTag("GenerateSummaryUseCase")

    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(sessionId: String, messages: List<Message>): MessageSummary? {
        logger.i { "Generating summary for session: $sessionId, messages: ${messages.size}" }

        if (messages.isEmpty()) {
            logger.w { "No messages provided for session: $sessionId" }
            return null
        }

        // Check if summary generation is worthwhile
        if (!summaryGenerator.shouldGenerateSummary(messages)) {
            logger.d { "Summary generation not worthwhile for ${messages.size} messages" }
            return null
        }

        // Generate summary
        val summaryText = summaryGenerator.generateSummary(messages)
        if (summaryText == null) {
            logger.e { "Failed to generate summary" }
            return null
        }

        // Create and save summary
        val summary = MessageSummary(
            id = Uuid.random().toString(),
            sessionId = sessionId,
            summary = summaryText,
            startMessageId = messages.first().id,
            endMessageId = messages.last().id,
            messageCount = messages.size,
            createdAt = currentTimeMillis(),
            tokenCount = ToonEncoder.estimateTokens(summaryText)
        )

        messageSummaryRepository.saveSummary(summary)
        logger.i { "Summary generated and saved for session: $sessionId" }

        return summary
    }

    /**
     * Regenerate summary for a session (force update).
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun regenerate(sessionId: String, messages: List<Message>): MessageSummary? {
        logger.i { "Regenerating summary for session: $sessionId" }

        // Delete existing summary
        messageSummaryRepository.deleteSummary(sessionId)

        // Generate new summary
        return invoke(sessionId, messages)
    }
}
