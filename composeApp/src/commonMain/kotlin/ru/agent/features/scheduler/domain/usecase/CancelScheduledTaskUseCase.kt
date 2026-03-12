package ru.agent.features.scheduler.domain.usecase

import ru.agent.core.util.TimeUtils
import ru.agent.features.scheduler.domain.model.TaskStatus
import ru.agent.features.scheduler.domain.repository.ScheduledTaskRepository

/**
 * Use case for cancelling a scheduled task.
 */
class CancelScheduledTaskUseCase(
    private val repository: ScheduledTaskRepository
) {
    /**
     * Cancels a scheduled task by its ID.
     *
     * Sets the task status to CANCELLED and clears the nextRunAt timestamp.
     *
     * @param id The unique identifier of the task to cancel
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(id: String): Result<Unit> = runCatching {
        require(id.isNotBlank()) { "Task ID cannot be blank" }

        // Verify task exists
        val task = repository.getById(id)
            ?: throw NoSuchElementException("Task not found with id: $id")

        // Check if task can be cancelled (not already cancelled)
        if (task.status == TaskStatus.CANCELLED) {
            throw IllegalStateException("Task is already cancelled")
        }

        val now = TimeUtils.currentTimeMillis()

        // Update status to CANCELLED and clear nextRunAt
        val updatedTask = task.copy(
            status = TaskStatus.CANCELLED,
            nextRunAt = null,
            updatedAt = now
        )

        repository.update(updatedTask)
    }
}
