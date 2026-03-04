package ru.agent.features.chat.domain.usecase

import ru.agent.common.wrappers.ResultWrapper
import ru.agent.features.chat.domain.repository.ChatRepository

/**
 * Use case for sending a message to LLM without saving to chat history.
 * Used for internal operations like task execution, planning, and validation.
 */
class SendSilentMessageUseCase(
    private val chatRepository: ChatRepository
) {
    /**
     * Send a message to LLM without saving to chat history.
     *
     * @param sessionId ID of the chat session
     * @param message Message content to send
     * @return ResultWrapper containing the response content or an error
     */
    suspend operator fun invoke(sessionId: String, message: String): ResultWrapper<String> {
        // Validate sessionId
        if (sessionId.isBlank()) {
            return ResultWrapper.Error(
                throwable = IllegalArgumentException("Session ID cannot be blank"),
                message = "Session ID is required"
            )
        }

        // Validate input
        if (message.isBlank()) {
            return ResultWrapper.Error(
                throwable = IllegalArgumentException("Message cannot be blank"),
                message = "Message cannot be empty"
            )
        }

        return chatRepository.sendSilentMessage(sessionId, message)
    }
}
