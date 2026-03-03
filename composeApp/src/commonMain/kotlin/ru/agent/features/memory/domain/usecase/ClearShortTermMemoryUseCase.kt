package ru.agent.features.memory.domain.usecase

import co.touchlab.kermit.Logger
import ru.agent.features.memory.domain.repository.ShortTermMemoryRepository

/**
 * UseCase для очистки краткосрочной памяти.
 *
 * Используется при:
 * - Начале новой сессии чата
 * - Очистке контекста по запросу пользователя
 * - Смене темы разговора
 */
class ClearShortTermMemoryUseCase(
    private val shortTermMemoryRepository: ShortTermMemoryRepository
) {
    private val logger = Logger.withTag("ClearShortTermMemoryUseCase")

    /**
     * Очистить краткосрочную память для конкретной сессии.
     *
     * @param sessionId ID сессии чата
     */
    operator fun invoke(sessionId: String) {
        logger.i { "Clearing STM for session: $sessionId" }
        shortTermMemoryRepository.clearMemory(sessionId)
    }

    /**
     * Очистить всю краткосрочную память (все сессии).
     */
    fun clearAll() {
        logger.w { "Clearing ALL short-term memory" }
        shortTermMemoryRepository.clearAll()
    }
}
