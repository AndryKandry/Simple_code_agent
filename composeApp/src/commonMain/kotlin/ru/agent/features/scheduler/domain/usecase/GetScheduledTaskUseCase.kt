package ru.agent.features.scheduler.domain.usecase

import ru.agent.features.scheduler.domain.model.ScheduledTask
import ru.agent.features.scheduler.domain.repository.ScheduledTaskRepository

/**
 * Use case for retrieving a single scheduled task by ID.
 */
class GetScheduledTaskUseCase(
    private val repository: ScheduledTaskRepository
) {
    /**
     * Retrieves a scheduled task by its ID.
     *
     * @param id The unique identifier of the task
     * @return Result containing the ScheduledTask or null if not found, or an error
     */
    suspend operator fun invoke(id: String): Result<ScheduledTask?> = runCatching {
        require(id.isNotBlank()) { "Task ID cannot be blank" }
        repository.getById(id)
    }
}
