package ru.agent.features.scheduler.domain.usecase

import ru.agent.core.util.TimeUtils
import ru.agent.features.scheduler.domain.model.TaskStatus
import ru.agent.features.scheduler.domain.repository.ScheduledTaskRepository

/**
 * Use case for resuming a paused scheduled task.
 */
class ResumeScheduledTaskUseCase(
    private val repository: ScheduledTaskRepository,
    private val calculateNextRunUseCase: CalculateNextRunUseCase
) {
    /**
     * Resumes a paused scheduled task by its ID.
     *
     * The task must be in PAUSED status to be resumed.
     * The nextRunAt timestamp is recalculated based on the cron expression.
     *
     * @param id The unique identifier of the task to resume
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(id: String): Result<Unit> = runCatching {
        require(id.isNotBlank()) { "Task ID cannot be blank" }

        // Verify task exists
        val task = repository.getById(id)
            ?: throw NoSuchElementException("Task not found with id: $id")

        // Check if task can be resumed
        when (task.status) {
            TaskStatus.PAUSED -> {
                // Valid state for resuming
            }
            TaskStatus.PENDING -> throw IllegalStateException("Task is already pending, no need to resume")
            TaskStatus.RUNNING -> throw IllegalStateException("Task is currently running")
            TaskStatus.CANCELLED -> throw IllegalStateException("Cannot resume a cancelled task")
        }

        val now = TimeUtils.currentTimeMillis()

        // Recalculate nextRunAt based on current time
        val nextRunAt = calculateNextRunUseCase(task.cronExpression, now).getOrThrow()
            ?: throw IllegalArgumentException("Invalid cron expression: ${task.cronExpression}")

        // Update status to PENDING and set new nextRunAt
        val updatedTask = task.copy(
            status = TaskStatus.PENDING,
            nextRunAt = nextRunAt,
            updatedAt = now
        )

        repository.update(updatedTask)
    }
}
