package ru.agent.features.scheduler.domain.usecase

import ru.agent.core.util.TimeUtils
import ru.agent.features.scheduler.domain.model.TaskStatus
import ru.agent.features.scheduler.domain.repository.ScheduledTaskRepository

/**
 * Use case for pausing a scheduled task.
 */
class PauseScheduledTaskUseCase(
    private val repository: ScheduledTaskRepository
) {
    /**
     * Pauses a scheduled task by its ID.
     *
     * The task must be in PENDING or RUNNING status to be paused.
     * The nextRunAt timestamp is preserved for later resume.
     *
     * @param id The unique identifier of the task to pause
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(id: String): Result<Unit> = runCatching {
        require(id.isNotBlank()) { "Task ID cannot be blank" }

        // Verify task exists
        val task = repository.getById(id)
            ?: throw NoSuchElementException("Task not found with id: $id")

        // Check if task can be paused
        when (task.status) {
            TaskStatus.PAUSED -> throw IllegalStateException("Task is already paused")
            TaskStatus.CANCELLED -> throw IllegalStateException("Cannot pause a cancelled task")
            TaskStatus.PENDING, TaskStatus.RUNNING -> {
                // Valid states for pausing
            }
        }

        val now = TimeUtils.currentTimeMillis()

        // Update status to PAUSED, preserve nextRunAt for resume
        val updatedTask = task.copy(
            status = TaskStatus.PAUSED,
            updatedAt = now
        )

        repository.update(updatedTask)
    }
}
