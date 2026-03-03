package ru.agent.features.memory.domain.usecase

import co.touchlab.kermit.Logger
import ru.agent.features.memory.domain.model.MemoryContext
import ru.agent.features.memory.domain.model.ShortTermMemory
import ru.agent.features.memory.domain.repository.LongTermMemoryRepository
import ru.agent.features.memory.domain.repository.ShortTermMemoryRepository
import ru.agent.features.memory.domain.repository.WorkingMemoryRepository
import ru.agent.features.profile.domain.repository.UserProfileRepository

/**
 * UseCase для получения полного контекста памяти.
 *
 * Агрегирует данные из всех трех уровней памяти:
 * - Short-term Memory (in-memory)
 * - Working Memory (database)
 * - Long-term Memory (database)
 */
class GetMemoryContextUseCase(
    private val shortTermMemoryRepository: ShortTermMemoryRepository,
    private val workingMemoryRepository: WorkingMemoryRepository,
    private val longTermMemoryRepository: LongTermMemoryRepository
) {
    private val logger = Logger.withTag("GetMemoryContextUseCase")

    /**
     * Получить полный контекст памяти для сессии.
     *
     * @param sessionId ID сессии чата
     * @param userId ID пользователя (по умолчанию "default")
     * @param searchQuery Опциональный запрос для поиска в базе знаний
     * @return MemoryContext с агрегированными данными
     */
    suspend operator fun invoke(
        sessionId: String,
        userId: String = UserProfileRepository.DEFAULT_USER_ID,
        searchQuery: String? = null
    ): MemoryContext {
        logger.d { "Building memory context for session: $sessionId" }

        // 1. Get Short-term Memory (in-memory, fast)
        val shortTermMemory = shortTermMemoryRepository.getMemory(sessionId)
            ?: ShortTermMemory(sessionId = sessionId)

        logger.d { "STM: ${shortTermMemory.messages.size} messages" }

        // 2. Get Working Memory (current task, database)
        val workingMemory = workingMemoryRepository.getWorkingMemory(sessionId)

        logger.d { "WM: ${if (workingMemory != null) "present" else "empty"}" }

        // 3. Get User Profile (LTM, database)
        val userProfile = longTermMemoryRepository.getUserProfile(userId)

        logger.d { "Profile: ${if (userProfile != null) "found" else "not found"}" }

        // 4. Get relevant knowledge entries (LTM, database)
        val relevantKnowledge = if (!searchQuery.isNullOrBlank()) {
            longTermMemoryRepository.searchKnowledge(searchQuery, limit = 5)
        } else {
            emptyList()
        }

        logger.d { "Knowledge entries: ${relevantKnowledge.size}" }

        // 5. Get active context anchors (LTM, database)
        val activeAnchors = longTermMemoryRepository.getActiveAnchors()

        logger.d { "Active anchors: ${activeAnchors.size}" }

        return MemoryContext(
            shortTermMemory = shortTermMemory,
            workingMemory = workingMemory,
            userProfile = userProfile,
            relevantKnowledge = relevantKnowledge,
            activeAnchors = activeAnchors
        ).also {
            logger.i { "Memory context built for session: $sessionId" }
        }
    }
}
