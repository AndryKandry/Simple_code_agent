package ru.agent.features.task.presentation.models

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
