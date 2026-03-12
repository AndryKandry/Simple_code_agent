package ru.agent.features.scheduler.domain.usecase

import ru.agent.features.scheduler.domain.model.TaskExecution
import ru.agent.features.scheduler.domain.repository.TaskExecutionRepository

/**
 * Use case for retrieving execution history of a scheduled task.
 */
class GetTaskExecutionsUseCase(
    private val executionRepository: TaskExecutionRepository
) {
    /**
     * Retrieves the execution history for a specific task.
     *
     * @param taskId The unique identifier of the task
     * @param limit Maximum number of executions to return (default: 10)
     * @return Result containing a list of TaskExecution or an error
     */
    suspend operator fun invoke(
        taskId: String,
        limit: Int = 10
    ): Result<List<TaskExecution>> = runCatching {
        require(taskId.isNotBlank()) { "Task ID cannot be blank" }
        require(limit > 0) { "Limit must be positive" }

        // Get all executions for the task and limit the result
        executionRepository.getByTaskId(taskId)
            .sortedByDescending { it.startedAt }
            .take(limit)
    }
}
