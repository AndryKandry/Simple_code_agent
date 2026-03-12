package ru.agent.features.scheduler.domain.usecase

import ru.agent.core.util.TimeUtils
import ru.agent.features.scheduler.domain.model.ScheduledTask
import ru.agent.features.scheduler.domain.model.TaskData
import ru.agent.features.scheduler.domain.model.TaskStatus
import ru.agent.features.scheduler.domain.model.TaskType
import ru.agent.features.scheduler.domain.repository.ScheduledTaskRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Use case for creating a new scheduled task.
 */
class CreateScheduledTaskUseCase(
    private val repository: ScheduledTaskRepository,
    private val calculateNextRunUseCase: CalculateNextRunUseCase
) {
    /**
     * Creates a new scheduled task.
     *
     * @param name Human-readable task name
     * @param description Optional task description
     * @param cronExpression Cron expression for scheduling
     * @param taskType Type of task (REMINDER, SHELL_COMMAND, MCP_TOOL)
     * @param taskData Task-specific data
     * @param tags Optional list of tags for organization
     * @return Result containing the created ScheduledTask or an error
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(
        name: String,
        description: String?,
        cronExpression: String,
        taskType: TaskType,
        taskData: TaskData,
        tags: List<String> = emptyList()
    ): Result<ScheduledTask> = runCatching {
        // Validate name
        require(name.isNotBlank()) { "Task name cannot be blank" }

        // Validate cron expression and calculate next run time
        val nextRunAt = calculateNextRunUseCase(cronExpression).getOrThrow()
            ?: throw IllegalArgumentException("Invalid cron expression: $cronExpression")

        val now = TimeUtils.currentTimeMillis()

        val task = ScheduledTask(
            id = Uuid.random().toString(),
            name = name,
            description = description,
            cronExpression = cronExpression,
            taskType = taskType,
            taskData = taskData,
            status = TaskStatus.PENDING,
            nextRunAt = nextRunAt,
            lastRunAt = null,
            createdAt = now,
            updatedAt = now,
            tags = tags
        )

        repository.create(task)
    }
}
