package ru.agent.features.task.presentation.models

import ru.agent.features.task.domain.validator.TransitionViolation

/**
 * Actions that can be triggered from the TaskStateViewModel.
 */
sealed class TaskStateAction {
    /**
     * Show an error message.
     */
    data class ShowError(val message: String) : TaskStateAction()

    /**
     * Show a success message.
     */
    data class ShowSuccess(val message: String) : TaskStateAction()

    /**
     * Show transition warnings to the user.
     * These are non-blocking violations that occurred during stage transitions.
     */
    data class ShowTransitionWarnings(val warnings: List<TransitionViolation>) : TaskStateAction()

    /**
     * Navigate to a specific task.
     */
    data class NavigateToTask(val taskId: String) : TaskStateAction()

    /**
     * Task was completed successfully.
     */
    data class TaskCompleted(val taskId: String, val taskName: String) : TaskStateAction()

    /**
     * Task was canceled.
     */
    data class TaskCanceled(val taskId: String) : TaskStateAction()

    /**
     * Task stage changed.
     */
    data class StageChanged(val fromStage: String, val toStage: String) : TaskStateAction()
}
