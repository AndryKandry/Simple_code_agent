package ru.agent.features.memory.domain.usecase

import co.touchlab.kermit.Logger
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.memory.domain.repository.ShortTermMemoryRepository

/**
 * UseCase для добавления сообщений в краткосрочную память.
 *
 * Инкапсулирует логику работы с STM, соблюдая Clean Architecture.
 */
class AddMessageToMemoryUseCase(
    private val shortTermMemoryRepository: ShortTermMemoryRepository
) {
    private val logger = Logger.withTag("AddMessageToMemoryUseCase")

    /**
     * Добавить сообщение в краткосрочную память.
     *
     * @param sessionId ID сессии чата
     * @param message Сообщение для добавления
     */
    suspend operator fun invoke(sessionId: String, message: Message) {
        try {
            shortTermMemoryRepository.addMessage(sessionId, message)
            logger.d { "Message added to STM: session=$sessionId, sender=${message.senderType}" }
        } catch (e: Exception) {
            logger.e(throwable = e) { "Failed to add message to STM" }
        }
    }

    /**
     * Добавить несколько сообщений в краткосрочную память.
     *
     * @param sessionId ID сессии чата
     * @param messages Список сообщений для добавления
     */
    operator fun invoke(sessionId: String, messages: List<Message>) {
        try {
            shortTermMemoryRepository.addMessages(sessionId, messages)
            logger.d { "Messages added to STM: session=$sessionId, count=${messages.size}" }
        } catch (e: Exception) {
            logger.e(throwable = e) { "Failed to add messages to STM" }
        }
    }
}
