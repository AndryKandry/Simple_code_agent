package ru.agent.features.chat.domain.usecase

import ru.agent.features.chat.domain.repository.ChatRepository

class ClearChatHistoryUseCase(
    private val chatRepository: ChatRepository
) {
    /**
     * Clear chat history for a specific session.
     *
     * @param sessionId ID of the chat session to clear
     */
    suspend operator fun invoke(sessionId: String) {
        if (sessionId.isNotBlank()) {
            chatRepository.clearHistory(sessionId)
        }
    }
}
