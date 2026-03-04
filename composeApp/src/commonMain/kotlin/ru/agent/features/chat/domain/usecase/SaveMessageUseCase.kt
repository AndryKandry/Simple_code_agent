package ru.agent.features.chat.domain.usecase

import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.repository.ChatRepository

/**
 * Use case for saving a message directly to chat history.
 * Used for assistant messages generated during task execution.
 */
class SaveMessageUseCase(
    private val chatRepository: ChatRepository
) {
    /**
     * Save a message to chat history.
     *
     * @param sessionId ID of the chat session
     * @param message Message to save
     */
    suspend operator fun invoke(sessionId: String, message: Message) {
        chatRepository.saveMessage(sessionId, message)
    }
}
