package ru.agent.features.task.presentation.models

import ru.agent.features.task.domain.model.TaskState

/**
 * ViewState for the Task State UI.
 */
data class TaskStateViewState(
    val currentTask: TaskState? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isTaskPanelVisible: Boolean = true
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
