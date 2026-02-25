package ru.agent.features.chat.domain.usecase

import kotlinx.coroutines.flow.Flow
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.repository.ChatRepository

class GetChatHistoryUseCase(
    private val chatRepository: ChatRepository
) {
    /**
     * Get chat history for a specific session (one-time request).
     *
     * @param sessionId ID of the chat session
     * @return List of messages in chronological order
     */
    suspend operator fun invoke(sessionId: String): List<Message> {
        if (sessionId.isBlank()) {
            return emptyList()
        }
        return chatRepository.getChatHistory(sessionId)
    }

    /**
     * Get chat history for a specific session as Flow for reactive updates.
     *
     * @param sessionId ID of the chat session
     * @return Flow emitting list of messages in chronological order
     */
    fun asFlow(sessionId: String): Flow<List<Message>> {
        return chatRepository.getChatHistoryFlow(sessionId)
    }
}
