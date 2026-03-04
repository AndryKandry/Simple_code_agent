package ru.agent.features.task.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.agent.features.task.domain.model.PlanStep
import ru.agent.features.task.domain.model.TaskStage
import ru.agent.features.task.domain.model.TaskState
import ru.agent.features.task.domain.model.TaskTransition

/**
 * Room entity for persisting task state.
 */
@Entity(
    tableName = "task_states",
    foreignKeys = [
        ForeignKey(
            entity = ru.agent.features.chat.data.local.entity.ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["taskStage"]),
        Index(value = ["updatedAt"])
    ]
)
data class TaskStateEntity(
    @PrimaryKey
    val taskId: String,
    val sessionId: String,
    val taskStage: String, // Serialized TaskStage
    val currentStep: Int,
    val totalSteps: Int,
    val expectedAction: String,
    val isPaused: Boolean,
    val transitionHistoryJson: String, // Serialized List<TaskTransition>
    val createdAt: Long,
    val updatedAt: Long,
    val taskName: String,
    val taskDescription: String?,
    val plan: String?,
    val planStepsJson: String // Serialized List<PlanStep>
) {
    /**
     * Converts entity to domain model.
     */
    fun toDomainModel(): TaskState {
        val transitions = try {
            Json.decodeFromString<List<TaskTransition>>(transitionHistoryJson)
        } catch (e: Exception) {
            emptyList()
        }

        val planSteps = try {
            Json.decodeFromString<List<PlanStep>>(planStepsJson)
        } catch (e: Exception) {
            emptyList()
        }

        return TaskState(
            taskId = taskId,
            sessionId = sessionId,
            taskStage = TaskStage.valueOf(taskStage),
            currentStep = currentStep,
            totalSteps = totalSteps,
            expectedAction = expectedAction,
            isPaused = isPaused,
            transitionHistory = transitions,
            createdAt = createdAt,
            updatedAt = updatedAt,
            taskName = taskName,
            taskDescription = taskDescription,
            plan = plan,
            planSteps = planSteps
        )
    }

    companion object {
        /**
         * Creates entity from domain model.
         */
        fun fromDomainModel(domainModel: TaskState): TaskStateEntity {
            return TaskStateEntity(
                taskId = domainModel.taskId,
                sessionId = domainModel.sessionId,
                taskStage = domainModel.taskStage.name,
                currentStep = domainModel.currentStep,
                totalSteps = domainModel.totalSteps,
                expectedAction = domainModel.expectedAction,
                isPaused = domainModel.isPaused,
                transitionHistoryJson = Json.encodeToString(domainModel.transitionHistory),
                createdAt = domainModel.createdAt,
                updatedAt = domainModel.updatedAt,
                taskName = domainModel.taskName,
                taskDescription = domainModel.taskDescription,
                plan = domainModel.plan,
                planStepsJson = Json.encodeToString(domainModel.planSteps)
            )
        }
    }
}
