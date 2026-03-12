package ru.agent.features.scheduler.domain.model

/**
 * Domain model for a scheduled task.
 *
 * @property id Unique task identifier
 * @property name Human-readable task name
 * @property description Optional task description
 * @property cronExpression Cron expression for scheduling
 * @property taskType Type of task (REMINDER, SHELL_COMMAND, MCP_TOOL)
 * @property taskData Task-specific data
 * @property status Current task status
 * @property nextRunAt Timestamp of next execution (null if paused/cancelled)
 * @property lastRunAt Timestamp of last execution (null if never run)
 * @property createdAt Task creation timestamp
 * @property updatedAt Last update timestamp
 * @property tags Optional list of tags for organization
 */
data class ScheduledTask(
    val id: String,
    val name: String,
    val description: String?,
    val cronExpression: String,
    val taskType: TaskType,
    val taskData: TaskData,
    val status: TaskStatus,
    val nextRunAt: Long?,
    val lastRunAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val tags: List<String> = emptyList()
)
