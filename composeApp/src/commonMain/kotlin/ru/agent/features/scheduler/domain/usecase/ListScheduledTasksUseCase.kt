package ru.agent.features.scheduler.domain.usecase

import ru.agent.features.scheduler.domain.model.ScheduledTask
import ru.agent.features.scheduler.domain.model.TaskStatus
import ru.agent.features.scheduler.domain.model.TaskType
import ru.agent.features.scheduler.domain.repository.ScheduledTaskRepository

/**
 * Use case for listing scheduled tasks with optional filtering.
 */
class ListScheduledTasksUseCase(
    private val repository: ScheduledTaskRepository
) {
    /**
     * Retrieves all scheduled tasks with optional filtering.
     *
     * @param type Optional filter by task type
     * @param status Optional filter by task status
     * @return Result containing a list of ScheduledTask or an error
     */
    suspend operator fun invoke(
        type: TaskType? = null,
        status: TaskStatus? = null
    ): Result<List<ScheduledTask>> = runCatching {
        when {
            type != null && status != null -> {
                // Filter by both type and status
                repository.getByType(type).filter { it.status == status }
            }
            type != null -> {
                // Filter by type only
                repository.getByType(type)
            }
            status != null -> {
                // Filter by status only
                repository.getByStatus(status)
            }
            else -> {
                // No filters - return all tasks
                repository.getAll()
            }
        }
    }
}
