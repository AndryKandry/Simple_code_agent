package ru.agent.features.taskcontext.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.agent.features.taskcontext.domain.model.TaskContext

/**
 * Repository для работы с контекстом задач.
 *
 * Предоставляет методы для CRUD операций с TaskContext,
 * а также реактивные потоки для наблюдения за изменениями.
 */
interface TaskContextRepository {

    /**
     * Получить контекст задачи по ID сессии.
     *
     * @param sessionId ID сессии чата
     * @return TaskContext или null если не найден
     */
    suspend fun getBySessionId(sessionId: String): TaskContext?

    /**
     * Получить контекст задачи по ID.
     *
     * @param id ID контекста задачи
     * @return TaskContext или null если не найден
     */
    suspend fun getById(id: String): TaskContext?

    /**
     * Получить контекст задачи по ID сессии (реактивно).
     *
     * @param sessionId ID сессии чата
     * @return Flow с TaskContext (emit при изменениях)
     */
    fun observeBySessionId(sessionId: String): Flow<TaskContext?>

    /**
     * Сохранить (создать или обновить) контекст задачи.
     *
     * @param taskContext Контекст задачи для сохранения
     */
    suspend fun save(taskContext: TaskContext)

    /**
     * Создать новый контекст задачи.
     *
     * @param taskContext Контекст задачи для создания
     */
    suspend fun insert(taskContext: TaskContext)

    /**
     * Обновить существующий контекст задачи.
     *
     * @param taskContext Контекст задачи для обновления
     */
    suspend fun update(taskContext: TaskContext)

    /**
     * Удалить контекст задачи по ID сессии.
     *
     * @param sessionId ID сессии чата
     * @return Количество удаленных записей
     */
    suspend fun deleteBySessionId(sessionId: String): Int

    /**
     * Удалить контекст задачи по ID.
     *
     * @param id ID контекста задачи
     * @return Количество удаленных записей (0 или 1)
     */
    suspend fun deleteById(id: String): Int

    /**
     * Получить все контексты задач.
     *
     * @return Список всех контекстов задач
     */
    suspend fun getAll(): List<TaskContext>

    /**
     * Получить количество контекстов в базе.
     *
     * @return Количество контекстов
     */
    suspend fun count(): Int
}
