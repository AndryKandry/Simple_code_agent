package ru.agent.features.chat.domain.usecase

import ru.agent.common.wrappers.ResultWrapper
import ru.agent.features.chat.domain.model.ChatSession
import ru.agent.features.chat.domain.repository.ChatSessionRepository

/**
 * Use Case for creating a new chat session.
 */
class CreateChatSessionUseCase(
    private val chatSessionRepository: ChatSessionRepository
) {
    /**
     * Create a new chat session with the specified title.
     *
     * @param title Title for the new session (default: "New Chat")
     * @return ResultWrapper containing the created ChatSession or an error
     */
    suspend operator fun invoke(title: String = "New Chat"): ResultWrapper<ChatSession> {
        // Validate title
        if (title.isBlank()) {
            return ResultWrapper.Error(
                throwable = IllegalArgumentException("Title cannot be blank"),
                message = "Chat session title cannot be empty"
            )
        }

        return chatSessionRepository.createSession(title.trim())
    }
}
