package ru.agent.features.taskcontext.domain.usecase

import co.touchlab.kermit.Logger
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.taskcontext.domain.model.ContextStage
import ru.agent.features.taskcontext.domain.model.TaskContext
import ru.agent.features.taskcontext.domain.repository.TaskContextRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * UseCase для инициализации контекста задачи.
 *
 * Создает новый контекст задачи для сессии или возвращает существующий.
 */
class InitializeTaskContextUseCase(
    private val taskContextRepository: TaskContextRepository
) {
    private val logger = Logger.withTag("InitializeTaskContextUseCase")

    /**
     * Инициализировать контекст задачи для сессии.
     *
     * Если контекст уже существует для данной сессии, возвращает его.
     * Иначе создает новый контекст.
     *
     * @param sessionId ID сессии чата
     * @return Инициализированный контекст задачи
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(sessionId: String): TaskContext {
        logger.d { "Initializing TaskContext for session: $sessionId" }

        // Проверяем существующий контекст
        val existing = taskContextRepository.getBySessionId(sessionId)
        if (existing != null) {
            logger.d { "TaskContext already exists: ${existing.id}, stage: ${existing.stage}" }
            return existing
        }

        // Создаем новый контекст
        val newContext = TaskContext(
            id = Uuid.random().toString(),
            sessionId = sessionId,
            stage = ContextStage.INITIALIZING,
            createdAt = currentTimeMillis(),
            updatedAt = currentTimeMillis()
        )

        taskContextRepository.insert(newContext)
        logger.i { "Created new TaskContext: ${newContext.id} for session: $sessionId" }

        return newContext
    }

    /**
     * Принудительно создать новый контекст для сессии.
     *
     * Удаляет существующий контекст (если есть) и создает новый.
     *
     * @param sessionId ID сессии чата
     * @return Созданный контекст задачи
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun createFresh(sessionId: String): TaskContext {
        logger.d { "Creating fresh TaskContext for session: $sessionId" }

        // Удаляем существующий контекст
        taskContextRepository.deleteBySessionId(sessionId)

        // Создаем новый
        val newContext = TaskContext(
            id = Uuid.random().toString(),
            sessionId = sessionId,
            stage = ContextStage.INITIALIZING,
            createdAt = currentTimeMillis(),
            updatedAt = currentTimeMillis()
        )

        taskContextRepository.insert(newContext)
        logger.i { "Created fresh TaskContext: ${newContext.id} for session: $sessionId" }

        return newContext
    }

    /**
     * Удалить контекст задачи для сессии.
     *
     * @param sessionId ID сессии чата
     * @return Количество удаленных записей
     */
    suspend fun clear(sessionId: String): Int {
        logger.d { "Clearing TaskContext for session: $sessionId" }
        return taskContextRepository.deleteBySessionId(sessionId)
    }
}
