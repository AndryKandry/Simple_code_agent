package ru.agent.features.memory.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.features.memory.domain.model.ExecutionState
import ru.agent.features.memory.domain.model.TaskInfo
import ru.agent.features.memory.domain.model.TaskStatus
import ru.agent.features.memory.domain.model.WorkingMemory

/**
 * Repository для рабочей памяти (WM).
 *
 * Хранит данные текущей задачи персистентно в Room Database.
 */
interface WorkingMemoryRepository {

    /**
     * Получить рабочую память для сессии.
     *
     * @param sessionId ID сессии чата
     * @return WorkingMemory или null
     */
    suspend fun getWorkingMemory(sessionId: String): WorkingMemory?

    /**
     * Получить рабочую память как Flow.
     *
     * @param sessionId ID сессии чата
     * @return Flow с WorkingMemory
     */
    fun getWorkingMemoryFlow(sessionId: String): Flow<WorkingMemory?>

    /**
     * Создать или обновить рабочую память.
     *
     * @param workingMemory Данные для сохранения
     * @return ResultWrapper с сохраненной WorkingMemory
     */
    suspend fun saveWorkingMemory(workingMemory: WorkingMemory): ResultWrapper<WorkingMemory>

    /**
     * Обновить состояние выполнения.
     *
     * @param sessionId ID сессии чата
     * @param state Новое состояние
     */
    suspend fun updateExecutionState(sessionId: String, state: ExecutionState)

    /**
     * Установить текущую задачу.
     *
     * @param sessionId ID сессии чата
     * @param taskInfo Информация о задаче
     */
    suspend fun setTask(sessionId: String, taskInfo: TaskInfo)

    /**
     * Обновить статус задачи.
     *
     * @param sessionId ID сессии чата
     * @param status Новый статус
     * @param progress Прогресс (0.0 - 1.0)
     */
    suspend fun updateTaskStatus(sessionId: String, status: TaskStatus, progress: Float = 0f)

    /**
     * Сохранить временные данные.
     *
     * @param sessionId ID сессии чата
     * @param data JSON-строка с временными данными
     */
    suspend fun saveTemporaryData(sessionId: String, data: String)

    /**
     * Получить временные данные.
     *
     * @param sessionId ID сессии чата
     * @return JSON-строка с временными данными или null
     */
    suspend fun getTemporaryData(sessionId: String): String?

    /**
     * Очистить рабочую память для сессии.
     *
     * @param sessionId ID сессии чата
     */
    suspend fun clearWorkingMemory(sessionId: String)

    /**
     * Получить все активные задачи (статус IN_PROGRESS).
     *
     * @return Список WorkingMemory с активными задачами
     */
    suspend fun getActiveTasks(): List<WorkingMemory>
}
