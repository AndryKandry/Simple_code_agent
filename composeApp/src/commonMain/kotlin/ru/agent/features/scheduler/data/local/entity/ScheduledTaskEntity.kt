package ru.agent.features.scheduler.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.agent.features.scheduler.domain.model.ScheduledTask
import ru.agent.features.scheduler.domain.model.TaskData
import ru.agent.features.scheduler.domain.model.TaskStatus
import ru.agent.features.scheduler.domain.model.TaskType

/**
 * Room entity for persisting scheduled tasks.
 *
 * @property id Unique task identifier
 * @property name Human-readable task name
 * @property description Optional task description
 * @property cronExpression Cron expression for scheduling
 * @property taskType Type of task (REMINDER, SHELL_COMMAND, MCP_TOOL)
 * @property taskDataJson Task-specific data serialized as JSON
 * @property status Current task status
 * @property nextRunAt Timestamp of next execution (null if paused/cancelled)
 * @property lastRunAt Timestamp of last execution (null if never run)
 * @property createdAt Task creation timestamp
 * @property updatedAt Last update timestamp
 * @property tagsJson Optional list of tags serialized as JSON
 */
@Entity(
    tableName = "scheduled_tasks",
    indices = [
        Index(value = ["status"]),
        Index(value = ["taskType"]),
        Index(value = ["nextRunAt"])
    ]
)
data class ScheduledTaskEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String?,
    val cronExpression: String,
    val taskType: String,
    val taskDataJson: String,
    val status: String,
    val nextRunAt: Long?,
    val lastRunAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val tagsJson: String
) {
    /**
     * Converts entity to domain model.
     */
    fun toDomainModel(): ScheduledTask {
        val taskData = try {
            Json.decodeFromString<TaskData>(taskDataJson)
        } catch (e: Exception) {
            TaskData.Reminder(message = "Invalid task data")
        }

        val tags = try {
            Json.decodeFromString<List<String>>(tagsJson)
        } catch (e: Exception) {
            emptyList()
        }

        return ScheduledTask(
            id = id,
            name = name,
            description = description,
            cronExpression = cronExpression,
            taskType = TaskType.valueOf(taskType),
            taskData = taskData,
            status = TaskStatus.valueOf(status),
            nextRunAt = nextRunAt,
            lastRunAt = lastRunAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
            tags = tags
        )
    }

    companion object {
        /**
         * Creates entity from domain model.
         */
        fun fromDomainModel(domainModel: ScheduledTask): ScheduledTaskEntity {
            return ScheduledTaskEntity(
                id = domainModel.id,
                name = domainModel.name,
                description = domainModel.description,
                cronExpression = domainModel.cronExpression,
                taskType = domainModel.taskType.name,
                taskDataJson = Json.encodeToString(domainModel.taskData),
                status = domainModel.status.name,
                nextRunAt = domainModel.nextRunAt,
                lastRunAt = domainModel.lastRunAt,
                createdAt = domainModel.createdAt,
                updatedAt = domainModel.updatedAt,
                tagsJson = Json.encodeToString(domainModel.tags)
            )
        }
    }
}
