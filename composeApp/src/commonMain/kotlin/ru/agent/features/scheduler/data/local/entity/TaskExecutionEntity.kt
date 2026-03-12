package ru.agent.features.scheduler.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.agent.features.scheduler.domain.model.ExecutionStatus
import ru.agent.features.scheduler.domain.model.TaskExecution

/**
 * Room entity for persisting task execution records.
 *
 * @property id Unique execution identifier
 * @property taskId Reference to the scheduled task
 * @property startedAt Execution start timestamp
 * @property completedAt Execution completion timestamp (null if running)
 * @property status Execution status
 * @property result Execution result message (optional)
 * @property error Error message if execution failed (optional)
 */
@Entity(
    tableName = "task_executions",
    foreignKeys = [
        ForeignKey(
            entity = ScheduledTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["taskId"]),
        Index(value = ["status"]),
        Index(value = ["startedAt"])
    ]
)
data class TaskExecutionEntity(
    @PrimaryKey
    val id: String,
    val taskId: String,
    val startedAt: Long,
    val completedAt: Long?,
    val status: String,
    val result: String?,
    val error: String?
) {
    /**
     * Converts entity to domain model.
     */
    fun toDomainModel(): TaskExecution {
        return TaskExecution(
            id = id,
            taskId = taskId,
            startedAt = startedAt,
            completedAt = completedAt,
            status = ExecutionStatus.valueOf(status),
            result = result,
            error = error
        )
    }

    companion object {
        /**
         * Creates entity from domain model.
         */
        fun fromDomainModel(domainModel: TaskExecution): TaskExecutionEntity {
            return TaskExecutionEntity(
                id = domainModel.id,
                taskId = domainModel.taskId,
                startedAt = domainModel.startedAt,
                completedAt = domainModel.completedAt,
                status = domainModel.status.name,
                result = domainModel.result,
                error = domainModel.error
            )
        }
    }
}
