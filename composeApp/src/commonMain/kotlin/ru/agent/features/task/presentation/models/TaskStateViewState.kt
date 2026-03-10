package ru.agent.features.task.presentation.models

import ru.agent.features.task.domain.model.TaskState
import ru.agent.features.task.domain.validator.TransitionViolation

/**
 * ViewState for the Task State UI.
 */
data class TaskStateViewState(
    val currentTask: TaskState? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isTaskPanelVisible: Boolean = true,
    /**
     * Warnings from the last transition operation.
     * These are non-blocking violations that occurred during stage transitions.
     */
    val transitionWarnings: List<TransitionViolation> = emptyList()
) {
    /**
     * Returns true if there's an active task that's not completed.
     */
    fun hasActiveTask(): Boolean {
        return currentTask != null && !currentTask.isCompleted()
    }

    /**
     * Returns true if the task can be paused/resumed.
     */
    fun canTogglePause(): Boolean {
        return currentTask != null && !currentTask.isCompleted()
    }

    /**
     * Returns true if the task can be advanced to the next stage.
     */
    fun canAdvanceStage(): Boolean {
        return currentTask?.canAdvance() == true
    }
}
