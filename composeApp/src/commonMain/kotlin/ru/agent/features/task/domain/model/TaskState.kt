package ru.agent.features.task.domain.model

import kotlinx.serialization.Serializable
import ru.agent.core.time.currentTimeMillis
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Represents a single step in the task plan.
 */
@Serializable
data class PlanStep(
    val number: Int,
    val description: String,
    val isCompleted: Boolean = false
)

/**
 * Represents the complete state of a task.
 *
 * @property taskId Unique identifier for the task
 * @property sessionId ID of the chat session this task belongs to
 * @property taskStage Current stage of the task
 * @property currentStep Current step number within the stage
 * @property totalSteps Total number of steps in the task
 * @property expectedAction Description of the expected next action
 * @property isPaused Whether the task is currently paused
 * @property transitionHistory History of all stage transitions
 * @property createdAt When the task was created
 * @property updatedAt When the task was last updated
 * @property taskName Human-readable name of the task
 * @property taskDescription Optional description of what the task does
 * @property plan LLM-generated plan for the task (in PLANNING stage)
 * @property planSteps Structured list of plan steps
 * @property waitingForUserInput Whether the task is waiting for user approval/feedback
 * @property userFeedback User feedback/rejection reason (if any)
 * @property executionResult Result of the execution stage
 * @property validationResult Result of the validation stage
 * @property summary Final summary of the completed task
 */
data class TaskState(
    val taskId: String,
    val sessionId: String,
    val taskStage: TaskStage,
    val currentStep: Int = 1,
    val totalSteps: Int = 1,
    val expectedAction: String = "",
    val isPaused: Boolean = false,
    val transitionHistory: List<TaskTransition> = emptyList(),
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = currentTimeMillis(),
    val taskName: String = "",
    val taskDescription: String? = null,
    val plan: String? = null,
    val planSteps: List<PlanStep> = emptyList(),
    val waitingForUserInput: Boolean = false,
    val userFeedback: String? = null,
    val executionResult: String? = null,
    val validationResult: String? = null,
    val summary: String? = null
) {
    /**
     * Returns the progress percentage (0-100).
     */
    fun progressPercentage(): Int {
        if (totalSteps == 0) return 0
        return ((currentStep.toFloat() / totalSteps) * 100).toInt().coerceIn(0, 100)
    }

    /**
     * Returns the count of completed plan steps.
     */
    fun completedStepsCount(): Int = planSteps.count { it.isCompleted }

    /**
     * Returns the total number of plan steps.
     */
    fun totalPlanSteps(): Int = planSteps.size

    /**
     * Returns the progress percentage based on completed plan steps.
     * Falls back to currentStep/totalSteps if no plan steps exist.
     */
    fun planProgressPercentage(): Int {
        return if (planSteps.isNotEmpty()) {
            if (totalPlanSteps() == 0) 0
            else ((completedStepsCount().toFloat() / totalPlanSteps()) * 100).toInt().coerceIn(0, 100)
        } else {
            progressPercentage()
        }
    }

    /**
     * Returns true if the task is in a final state (DONE).
     */
    fun isCompleted(): Boolean = taskStage == TaskStage.DONE

    /**
     * Returns true if the task can be advanced to the next stage.
     */
    fun canAdvance(): Boolean {
        return !isPaused && !isCompleted() && taskStage.nextStage() != null
    }

    /**
     * Returns true if the task has a plan ready (PLANNING stage with plan).
     */
    fun hasPlan(): Boolean = !plan.isNullOrEmpty() || planSteps.isNotEmpty()

    /**
     * Returns true if the task is waiting for user approval (in PLANNING or VALIDATION stage).
     */
    fun isWaitingForApproval(): Boolean {
        return waitingForUserInput && (taskStage == TaskStage.PLANNING || taskStage == TaskStage.VALIDATION)
    }

    /**
     * Returns the prompt text for user approval based on current stage.
     */
    fun getApprovalPrompt(): String {
        return when (taskStage) {
            TaskStage.PLANNING -> "Approve the plan to start execution, or provide feedback to modify it."
            TaskStage.VALIDATION -> "Approve the result to complete the task, or provide feedback to retry."
            else -> ""
        }
    }

    companion object {
        /**
         * Creates a new task in PLANNING stage.
         */
        @OptIn(ExperimentalUuidApi::class)
        fun create(
            sessionId: String,
            taskName: String,
            taskDescription: String? = null,
            totalSteps: Int = 1
        ): TaskState {
            require(totalSteps > 0) { "totalSteps must be positive, was: $totalSteps" }
            val now = currentTimeMillis()
            return TaskState(
                taskId = Uuid.random().toString(),
                sessionId = sessionId,
                taskStage = TaskStage.PLANNING,
                currentStep = 1,
                totalSteps = totalSteps,
                taskName = taskName,
                taskDescription = taskDescription,
                expectedAction = "Generating plan...",
                createdAt = now,
                updatedAt = now
            )
        }
    }
}
