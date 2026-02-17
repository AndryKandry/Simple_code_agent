package ru.agent.features.chat.domain.usecase

import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.repository.ChatRepository

class GetChatHistoryUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(): List<Message> {
        return chatRepository.getChatHistory()
    }
}
