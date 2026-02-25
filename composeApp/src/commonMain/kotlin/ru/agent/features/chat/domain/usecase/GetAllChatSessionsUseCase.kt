package ru.agent.features.chat.domain.usecase

import kotlinx.coroutines.flow.Flow
import ru.agent.features.chat.domain.model.ChatSession
import ru.agent.features.chat.domain.repository.ChatSessionRepository

/**
 * Use Case for getting all chat sessions as a reactive Flow.
 */
class GetAllChatSessionsUseCase(
    private val chatSessionRepository: ChatSessionRepository
) {
    /**
     * Get all chat sessions as Flow.
     * Sessions are sorted by updatedAt (newest first).
     *
     * @return Flow emitting list of ChatSession
     */
    operator fun invoke(): Flow<List<ChatSession>> {
        return chatSessionRepository.getAllSessionsFlow()
    }
}
