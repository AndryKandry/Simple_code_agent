package ru.agent.features.memory.domain.usecase

import co.touchlab.kermit.Logger
import ru.agent.features.invariant.domain.repository.InvariantRepository
import ru.agent.features.memory.domain.model.MemoryContext
import ru.agent.features.memory.domain.model.ShortTermMemory
import ru.agent.features.memory.domain.repository.LongTermMemoryRepository
import ru.agent.features.memory.domain.repository.ShortTermMemoryRepository
import ru.agent.features.memory.domain.repository.WorkingMemoryRepository
import ru.agent.features.profile.domain.repository.UserProfileRepository
import ru.agent.features.rag.domain.model.ChunkScore
import ru.agent.features.rag.domain.service.RagSearchService

/**
 * UseCase для получения полного контекста памяти.
 *
 * Агрегирует данные из всех трех уровней памяти:
 * - Short-term Memory (in-memory)
 * - Working Memory (database)
 * - Long-term Memory (database)
 * - Project Invariants (active rules)
 * - RAG Index (relevant code chunks)
 */
class GetMemoryContextUseCase(
    private val shortTermMemoryRepository: ShortTermMemoryRepository,
    private val workingMemoryRepository: WorkingMemoryRepository,
    private val longTermMemoryRepository: LongTermMemoryRepository,
    private val invariantRepository: InvariantRepository,
    private val ragSearchService: RagSearchService
) {
    private val logger = Logger.withTag("GetMemoryContextUseCase")

    /**
     * Получить полный контекст памяти для сессии.
     *
     * @param sessionId ID сессии чата
     * @param userId ID пользователя (по умолчанию "default")
     * @param searchQuery Опциональный запрос для поиска в базе знаний и RAG индексе
     * @param ragEnabled Включить RAG поиск (по умолчанию true)
     * @return MemoryContext с агрегированными данными
     */
    suspend operator fun invoke(
        sessionId: String,
        userId: String = UserProfileRepository.DEFAULT_USER_ID,
        searchQuery: String? = null,
        ragEnabled: Boolean = true
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

        // 6. Get active invariants (project rules)
        val activeInvariants = invariantRepository.getActiveInvariants()
            .filter { it.isActive }

        logger.d { "Active invariants: ${activeInvariants.size}" }

        // 7. Get relevant chunks from RAG index
        val relevantChunks: List<ChunkScore> = if (ragEnabled && !searchQuery.isNullOrBlank()) {
            try {
                ragSearchService.search(searchQuery).also { chunks ->
                    logger.d { "RAG chunks found: ${chunks.size}" }
                    chunks.forEach { chunk ->
                        logger.v { "  - [${chunk.rank}] ${chunk.fileName}: similarity=${chunk.similarity}" }
                    }
                }
            } catch (e: Exception) {
                logger.e { "RAG search failed: ${e.message}" }
                emptyList()
            }
        } else {
            emptyList()
        }

        return MemoryContext(
            shortTermMemory = shortTermMemory,
            workingMemory = workingMemory,
            userProfile = userProfile,
            relevantKnowledge = relevantKnowledge,
            activeAnchors = activeAnchors,
            activeInvariants = activeInvariants,
            relevantChunks = relevantChunks
        ).also {
            logger.i { "Memory context built for session: $sessionId with ${activeInvariants.size} invariants and ${relevantChunks.size} RAG chunks" }
        }
    }
}
