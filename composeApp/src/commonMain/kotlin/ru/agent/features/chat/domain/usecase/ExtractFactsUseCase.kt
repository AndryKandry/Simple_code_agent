package ru.agent.features.chat.domain.usecase

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.first
import ru.agent.features.chat.domain.model.Fact
import ru.agent.features.chat.domain.optimization.FactsExtractor
import ru.agent.features.chat.domain.repository.FactsRepository

/**
 * UseCase for extracting facts from a conversation turn.
 *
 * This UseCase:
 * 1. Loads existing facts for the session
 * 2. Calls the FactsExtractor to analyze the new conversation turn
 * 3. Saves new/updated facts to the repository
 */
class ExtractFactsUseCase(
    private val factsExtractor: FactsExtractor,
    private val factsRepository: FactsRepository
) {
    private val logger = Logger.withTag("ExtractFactsUseCase")

    /**
     * Extract and save facts from a conversation turn.
     *
     * @param sessionId The session ID
     * @param userMessage The user's message content
     * @param assistantResponse The assistant's response content
     * @param sourceMessageId Optional ID of the message for tracking
     * @param existingFacts Facts already in the session (for deduplication)
     * @return Result with list of extracted and saved facts
     */
    suspend operator fun invoke(
        sessionId: String,
        userMessage: String,
        assistantResponse: String,
        sourceMessageId: String? = null,
        existingFacts: List<Fact> = emptyList()
    ): Result<List<Fact>> {
        return try {
            logger.i { "Extracting facts for session: $sessionId" }

            // Skip extraction if messages are too short
            if (userMessage.length < MIN_MESSAGE_LENGTH && assistantResponse.length < MIN_MESSAGE_LENGTH) {
                logger.d { "Messages too short for fact extraction" }
                return Result.success(emptyList())
            }

            // Extract facts using LLM
            val extractionResult = factsExtractor.extractFacts(
                sessionId = sessionId,
                userMessage = userMessage,
                assistantResponse = assistantResponse,
                existingFacts = existingFacts,
                sourceMessageId = sourceMessageId
            )

            val extractedFacts = extractionResult.getOrDefault(emptyList())

            if (extractedFacts.isEmpty()) {
                logger.d { "No new facts extracted" }
                return Result.success(emptyList())
            }

            // Upsert facts to repository
            val upsertResult = factsRepository.upsertFacts(extractedFacts)

            upsertResult.fold(
                onSuccess = { savedFacts ->
                    logger.i { "Saved ${savedFacts.size} facts" }
                    Result.success(savedFacts)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to save facts" }
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            logger.e(e) { "Failed to extract facts" }
            Result.failure(e)
        }
    }

    /**
     * Load existing facts for a session.
     * This is a helper method for callers who need to load facts first.
     *
     * @param sessionId The session ID
     * @return List of existing facts or empty list on error
     */
    suspend fun loadExistingFacts(sessionId: String): List<Fact> {
        return try {
            factsRepository.getFactsForSession(sessionId).first()
        } catch (e: Exception) {
            logger.e(e) { "Failed to load existing facts" }
            emptyList()
        }
    }

    companion object {
        const val MIN_MESSAGE_LENGTH = 20
    }
}
