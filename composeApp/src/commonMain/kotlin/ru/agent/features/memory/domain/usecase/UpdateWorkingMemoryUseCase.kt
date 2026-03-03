package ru.agent.features.memory.domain.usecase

import co.touchlab.kermit.Logger
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.memory.domain.model.ExecutionState
import ru.agent.features.memory.domain.model.TaskInfo
import ru.agent.features.memory.domain.model.TaskStatus
import ru.agent.features.memory.domain.model.WorkingMemory
import ru.agent.features.memory.domain.repository.WorkingMemoryRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * UseCase для обновления рабочей памяти.
 *
 * Управляет текущей задачей и состоянием выполнения.
 */
class UpdateWorkingMemoryUseCase(
    private val workingMemoryRepository: WorkingMemoryRepository
) {
    private val logger = Logger.withTag("UpdateWorkingMemoryUseCase")

    /**
     * Создать или обновить рабочую память для сессии.
     *
     * @param sessionId ID сессии чата
     * @param taskInfo Информация о задаче (опционально)
     * @param executionState Состояние выполнения (опционально)
     * @param temporaryData Временные данные (опционально)
     * @return ResultWrapper с обновленной WorkingMemory
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(
        sessionId: String,
        taskInfo: TaskInfo? = null,
        executionState: ExecutionState? = null,
        temporaryData: String? = null
    ): ResultWrapper<WorkingMemory> {
        logger.i { "Updating working memory for session: $sessionId" }

        val existing = workingMemoryRepository.getWorkingMemory(sessionId)
        val now = currentTimeMillis()

        val workingMemory = if (existing != null) {
            existing.copy(
                taskInfo = taskInfo ?: existing.taskInfo,
                executionState = executionState ?: existing.executionState,
                temporaryData = temporaryData ?: existing.temporaryData,
                updatedAt = now
            )
        } else {
            WorkingMemory(
                id = Uuid.random().toString(),
                sessionId = sessionId,
                taskInfo = taskInfo,
                executionState = executionState ?: ExecutionState.IDLE,
                temporaryData = temporaryData,
                createdAt = now,
                updatedAt = now
            )
        }

        return workingMemoryRepository.saveWorkingMemory(workingMemory).also { result ->
            when (result) {
                is ResultWrapper.Success -> logger.i { "Working memory updated" }
                is ResultWrapper.Error -> logger.e { "Failed to update working memory: ${result.message}" }
            }
        }
    }

    /**
     * Установить состояние выполнения.
     */
    suspend fun setExecutionState(sessionId: String, state: ExecutionState) {
        logger.d { "Setting execution state: $state for session: $sessionId" }
        workingMemoryRepository.updateExecutionState(sessionId, state)
    }

    /**
     * Начать новую задачу.
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun startTask(
        sessionId: String,
        taskType: ru.agent.features.memory.domain.model.TaskType,
        description: String,
        parentTaskId: String? = null
    ): ResultWrapper<WorkingMemory> {
        logger.i { "Starting task: $description for session: $sessionId" }

        val now = currentTimeMillis()
        val taskInfo = TaskInfo(
            taskId = Uuid.random().toString(),
            taskType = taskType,
            description = description,
            status = TaskStatus.IN_PROGRESS,
            progress = 0f,
            startedAt = now,
            completedAt = null,
            parentTaskId = parentTaskId
        )

        return invoke(
            sessionId = sessionId,
            taskInfo = taskInfo,
            executionState = ExecutionState.EXECUTING
        )
    }

    /**
     * Обновить прогресс задачи.
     */
    suspend fun updateProgress(sessionId: String, progress: Float, status: TaskStatus? = null) {
        logger.d { "Updating progress: $progress for session: $sessionId" }
        val current = workingMemoryRepository.getWorkingMemory(sessionId)
        if (current != null && current.taskInfo != null) {
            val updatedTask = current.taskInfo.copy(
                progress = progress.coerceIn(0f, 1f),
                status = status ?: current.taskInfo.status,
                completedAt = if (status == TaskStatus.COMPLETED || status == TaskStatus.FAILED) {
                    currentTimeMillis()
                } else null
            )
            workingMemoryRepository.setTask(sessionId, updatedTask)
        }
    }

    /**
     * Завершить задачу успешно.
     */
    suspend fun completeTask(sessionId: String) {
        logger.i { "Completing task for session: $sessionId" }
        updateProgress(sessionId, 1f, TaskStatus.COMPLETED)
        workingMemoryRepository.updateExecutionState(sessionId, ExecutionState.IDLE)
    }

    /**
     * Завершить задачу с ошибкой.
     */
    suspend fun failTask(sessionId: String) {
        logger.w { "Failing task for session: $sessionId" }
        updateProgress(sessionId, 0f, TaskStatus.FAILED)
        workingMemoryRepository.updateExecutionState(sessionId, ExecutionState.ERROR)
    }

    /**
     * Очистить рабочую память.
     */
    suspend fun clear(sessionId: String) {
        logger.i { "Clearing working memory for session: $sessionId" }
        workingMemoryRepository.clearWorkingMemory(sessionId)
    }
}
