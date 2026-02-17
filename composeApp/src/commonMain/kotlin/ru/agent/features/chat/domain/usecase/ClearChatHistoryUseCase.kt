package ru.agent.features.chat.domain.usecase

import ru.agent.features.chat.domain.repository.ChatRepository

class ClearChatHistoryUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke() {
        chatRepository.clearHistory()
    }
}
