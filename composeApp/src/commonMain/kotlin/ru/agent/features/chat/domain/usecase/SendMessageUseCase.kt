package ru.agent.features.chat.domain.usecase

import ru.agent.common.wrappers.ResultWrapper
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.repository.ChatRepository

class SendMessageUseCase(
    private val chatRepository: ChatRepository
) {
    /**
     * Send a message in the specified chat session.
     *
     * @param sessionId ID of the chat session
     * @param message Message content to send
     * @param checkpointId ID of the checkpoint for branching (null for main branch)
     * @return ResultWrapper containing the response Message or an error
     */
    suspend operator fun invoke(
        sessionId: String,
        message: String,
        checkpointId: String? = null
    ): ResultWrapper<Message> {
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

        return chatRepository.sendMessage(sessionId.trim(), message.trim(), checkpointId)
    }
}
