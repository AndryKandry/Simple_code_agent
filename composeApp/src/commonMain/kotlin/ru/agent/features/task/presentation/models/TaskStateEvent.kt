package ru.agent.features.task.presentation.models

/**
 * Events that can be triggered from the Task State UI.
 */
sealed class TaskStateEvent {
    /**
     * Create a new task.
     */
    data class CreateTask(
        val sessionId: String,
        val taskName: String,
        val taskDescription: String? = null,
        val totalSteps: Int = 1
    ) : TaskStateEvent()

    /**
     * Pause the current task.
     */
    object PauseTask : TaskStateEvent()

    /**
     * Resume the current task.
     */
    object ResumeTask : TaskStateEvent()

    /**
     * Toggle pause/resume state.
     */
    object TogglePause : TaskStateEvent()

    /**
     * Cancel the current task.
     */
    object CancelTask : TaskStateEvent()

    /**
     * Advance the task to the next stage.
     */
    object AdvanceStage : TaskStateEvent()

    /**
     * Skip validation and complete the task.
     */
    object SkipValidationAndComplete : TaskStateEvent()

    /**
     * Toggle the visibility of the task panel.
     */
    object ToggleTaskPanel : TaskStateEvent()

    /**
     * Load the active task for a session.
     */
    data class LoadActiveTask(val sessionId: String) : TaskStateEvent()

    /**
     * Update the current step in the task.
     */
    data class UpdateCurrentStep(val step: Int) : TaskStateEvent()

    /**
     * Update the expected action for the current step.
     */
    data class UpdateExpectedAction(val action: String) : TaskStateEvent()

    /**
     * Clear any error message.
     */
    object ClearError : TaskStateEvent()
}
