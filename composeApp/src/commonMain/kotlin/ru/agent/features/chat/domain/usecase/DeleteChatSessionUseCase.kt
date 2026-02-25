package ru.agent.features.chat.domain.usecase

import ru.agent.common.wrappers.ResultWrapper
import ru.agent.features.chat.domain.repository.ChatSessionRepository

/**
 * Use Case for deleting a chat session.
 */
class DeleteChatSessionUseCase(
    private val chatSessionRepository: ChatSessionRepository
) {
    /**
     * Delete a chat session by ID.
     *
     * @param sessionId ID of the session to delete
     * @return ResultWrapper indicating success or failure
     */
    suspend operator fun invoke(sessionId: String): ResultWrapper<Unit> {
        // Validate sessionId
        if (sessionId.isBlank()) {
            return ResultWrapper.Error(
                throwable = IllegalArgumentException("Session ID cannot be blank"),
                message = "Session ID is required"
            )
        }

        return chatSessionRepository.deleteSession(sessionId.trim())
    }
}
