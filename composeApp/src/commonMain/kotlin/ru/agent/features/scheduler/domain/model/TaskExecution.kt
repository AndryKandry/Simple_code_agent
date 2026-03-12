package ru.agent.features.scheduler.domain.model

/**
 * Domain model for a task execution record.
 *
 * @property id Unique execution identifier
 * @property taskId Reference to the scheduled task
 * @property startedAt Execution start timestamp
 * @property completedAt Execution completion timestamp (null if running)
 * @property status Execution status
 * @property result Execution result message (optional)
 * @property error Error message if execution failed (optional)
 */
data class TaskExecution(
    val id: String,
    val taskId: String,
    val startedAt: Long,
    val completedAt: Long?,
    val status: ExecutionStatus,
    val result: String?,
    val error: String?
)
