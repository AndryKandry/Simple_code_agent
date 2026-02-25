package ru.agent.features.chat.domain.usecase

import ru.agent.features.chat.domain.optimization.OptimizedContext
import ru.agent.features.chat.domain.repository.ChatRepository

/**
 * Use Case for getting optimized context for AI requests.
 *
 * Uses token optimization strategies to ensure context fits within
 * the model's context window while maintaining conversation coherence.
 */
class GetOptimizedContextUseCase(
    private val chatRepository: ChatRepository
) {
    /**
     * Get optimized context for a specific chat session.
     *
     * @param sessionId ID of the chat session
     * @return OptimizedContext with selected messages and token-efficient encoding
     */
    suspend operator fun invoke(sessionId: String): OptimizedContext {
        return chatRepository.getOptimizedContext(sessionId)
    }
}
