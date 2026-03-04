package ru.agent.features.task.presentation

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.agent.core.presentation.BaseViewModel
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.task.domain.model.TaskStage
import ru.agent.features.task.domain.usecase.CancelTaskUseCase
import ru.agent.features.task.domain.usecase.CreateTaskUseCase
import ru.agent.features.task.domain.usecase.GetTaskStateUseCase
import ru.agent.features.task.domain.usecase.PauseTaskUseCase
import ru.agent.features.task.domain.usecase.ResumeTaskUseCase
import ru.agent.features.task.domain.usecase.TransitionTaskStageUseCase
import ru.agent.features.task.presentation.models.TaskStateAction
import ru.agent.features.task.presentation.models.TaskStateEvent
import ru.agent.features.task.presentation.models.TaskStateViewState

/**
 * ViewModel for managing task state.
 */
class TaskStateViewModel(
    private val getTaskStateUseCase: GetTaskStateUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val transitionTaskStageUseCase: TransitionTaskStageUseCase,
    private val pauseTaskUseCase: PauseTaskUseCase,
    private val resumeTaskUseCase: ResumeTaskUseCase,
    private val cancelTaskUseCase: CancelTaskUseCase
) : BaseViewModel<TaskStateViewState, TaskStateAction, TaskStateEvent>(
    initialState = TaskStateViewState()
) {

    private val logger = Logger.withTag("TaskStateViewModel")
    private var observeJob: Job? = null

    override fun obtainEvent(viewEvent: TaskStateEvent) {
        logger.d { "Event received: $viewEvent" }
        when (viewEvent) {
            is TaskStateEvent.CreateTask -> handleCreateTask(viewEvent)
            is TaskStateEvent.PauseTask -> handlePauseTask()
            is TaskStateEvent.ResumeTask -> handleResumeTask()
            is TaskStateEvent.TogglePause -> handleTogglePause()
            is TaskStateEvent.CancelTask -> handleCancelTask()
            is TaskStateEvent.AdvanceStage -> handleAdvanceStage()
            is TaskStateEvent.SkipValidationAndComplete -> handleSkipValidation()
            is TaskStateEvent.ToggleTaskPanel -> handleToggleTaskPanel()
            is TaskStateEvent.LoadActiveTask -> handleLoadActiveTask(viewEvent.sessionId)
            is TaskStateEvent.UpdateCurrentStep -> handleUpdateCurrentStep(viewEvent.step)
            is TaskStateEvent.UpdateExpectedAction -> handleUpdateExpectedAction(viewEvent.action)
            is TaskStateEvent.ClearError -> handleClearError()
        }
    }

    /**
     * Creates a new task for a session.
     */
    private fun handleCreateTask(event: TaskStateEvent.CreateTask) {
        viewModelScope.launch {
            viewState = viewState.copy(isLoading = true)
            try {
                val task = createTaskUseCase(
                    sessionId = event.sessionId,
                    taskName = event.taskName,
                    taskDescription = event.taskDescription,
                    totalSteps = event.totalSteps
                )
                viewState = viewState.copy(
                    currentTask = task,
                    isLoading = false,
                    isTaskPanelVisible = true,
                    error = null
                )
                logger.i { "Task created: ${task.taskId}" }
                viewAction = TaskStateAction.ShowSuccess("Task created: ${task.taskName}")
            } catch (e: Exception) {
                logger.e(throwable = e) { "Failed to create task" }
                viewState = viewState.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to create task"
                )
                viewAction = TaskStateAction.ShowError(e.message ?: "Failed to create task")
            }
        }
    }

    /**
     * Pauses the current task.
     */
    private fun handlePauseTask() {
        val taskId = viewState.currentTask?.taskId ?: return

        viewModelScope.launch {
            try {
                val updatedTask = pauseTaskUseCase(
                    taskId = taskId,
                    reason = "User paused task",
                    contextSnapshot = mapOf(
                        "stage" to viewState.currentTask!!.taskStage.name,
                        "step" to viewState.currentTask!!.currentStep.toString()
                    )
                )
                if (updatedTask != null) {
                    viewState = viewState.copy(currentTask = updatedTask)
                    logger.i { "Task paused: $taskId" }
                    viewAction = TaskStateAction.ShowSuccess("Task paused")
                }
            } catch (e: Exception) {
                logger.e(throwable = e) { "Failed to pause task" }
                viewAction = TaskStateAction.ShowError(e.message ?: "Failed to pause task")
            }
        }
    }

    /**
     * Resumes the current task.
     */
    private fun handleResumeTask() {
        val taskId = viewState.currentTask?.taskId ?: return

        viewModelScope.launch {
            try {
                val updatedTask = resumeTaskUseCase(taskId)
                if (updatedTask != null) {
                    viewState = viewState.copy(currentTask = updatedTask)
                    logger.i { "Task resumed: $taskId" }
                    viewAction = TaskStateAction.ShowSuccess("Task resumed")
                }
            } catch (e: Exception) {
                logger.e(throwable = e) { "Failed to resume task" }
                viewAction = TaskStateAction.ShowError(e.message ?: "Failed to resume task")
            }
        }
    }

    /**
     * Toggles pause/resume state.
     */
    private fun handleTogglePause() {
        val task = viewState.currentTask
        if (task == null) {
            logger.w { "No task to toggle pause" }
            return
        }

        if (task.isPaused) {
            handleResumeTask()
        } else {
            handlePauseTask()
        }
    }

    /**
     * Cancels the current task.
     */
    private fun handleCancelTask() {
        val taskId = viewState.currentTask?.taskId ?: return

        viewModelScope.launch {
            try {
                cancelTaskUseCase(taskId)
                viewState = viewState.copy(
                    currentTask = null,
                    isTaskPanelVisible = false
                )
                logger.i { "Task canceled: $taskId" }
                viewAction = TaskStateAction.TaskCanceled(taskId)
            } catch (e: Exception) {
                logger.e(throwable = e) { "Failed to cancel task" }
                viewAction = TaskStateAction.ShowError(e.message ?: "Failed to cancel task")
            }
        }
    }

    /**
     * Advances the task to the next stage.
     */
    private fun handleAdvanceStage() {
        val taskId = viewState.currentTask?.taskId ?: return
        val currentStage = viewState.currentTask?.taskStage ?: return

        viewModelScope.launch {
            try {
                val updatedTask = transitionTaskStageUseCase.advanceToNextStage(taskId)
                if (updatedTask != null) {
                    viewState = viewState.copy(currentTask = updatedTask)
                    logger.i { "Task advanced to: ${updatedTask.taskStage}" }
                    viewAction = TaskStateAction.StageChanged(
                        fromStage = currentStage.name,
                        toStage = updatedTask.taskStage.name
                    )

                    // Check if task is completed
                    if (updatedTask.isCompleted()) {
                        viewAction = TaskStateAction.TaskCompleted(
                            taskId = updatedTask.taskId,
                            taskName = updatedTask.taskName
                        )
                    }
                }
            } catch (e: Exception) {
                logger.e(throwable = e) { "Failed to advance task stage" }
                viewAction = TaskStateAction.ShowError(e.message ?: "Failed to advance task")
            }
        }
    }

    /**
     * Skips validation and completes the task.
     */
    private fun handleSkipValidation() {
        val taskId = viewState.currentTask?.taskId ?: return

        viewModelScope.launch {
            try {
                val updatedTask = transitionTaskStageUseCase.skipValidationAndComplete(taskId)
                if (updatedTask != null) {
                    viewState = viewState.copy(currentTask = updatedTask)
                    logger.i { "Task completed (validation skipped): $taskId" }
                    viewAction = TaskStateAction.TaskCompleted(
                        taskId = updatedTask.taskId,
                        taskName = updatedTask.taskName
                    )
                }
            } catch (e: Exception) {
                logger.e(throwable = e) { "Failed to skip validation" }
                viewAction = TaskStateAction.ShowError(e.message ?: "Failed to skip validation")
            }
        }
    }

    /**
     * Toggles the task panel visibility.
     */
    private fun handleToggleTaskPanel() {
        viewState = viewState.copy(isTaskPanelVisible = !viewState.isTaskPanelVisible)
    }

    /**
     * Loads the active task for a session and starts observing it.
     */
    private fun handleLoadActiveTask(sessionId: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            viewState = viewState.copy(isLoading = true)
            getTaskStateUseCase.observeActiveTaskForSession(sessionId)
                .catch { exception ->
                    logger.e { "Error observing task: ${exception.message}" }
                    viewState = viewState.copy(
                        isLoading = false,
                        error = "Failed to load task: ${exception.message}"
                    )
                }
                .collectLatest { task ->
                    logger.d { "Task updated: ${task?.taskId}" }
                    viewState = viewState.copy(
                        currentTask = task,
                        isLoading = false,
                        error = null,
                        isTaskPanelVisible = task != null && !task.isCompleted()
                    )
                }
        }
    }

    /**
     * Updates the current step in the task.
     */
    private fun handleUpdateCurrentStep(step: Int) {
        val currentTask = viewState.currentTask ?: return

        viewModelScope.launch {
            val updatedTask = currentTask.copy(
                currentStep = step.coerceIn(1, currentTask.totalSteps),
                updatedAt = currentTimeMillis()
            )
            // Note: This would need a proper update use case in a real app
            // For now, we just update the local state
            viewState = viewState.copy(currentTask = updatedTask)
        }
    }

    /**
     * Updates the expected action for the current step.
     */
    private fun handleUpdateExpectedAction(action: String) {
        val currentTask = viewState.currentTask ?: return

        viewModelScope.launch {
            val updatedTask = currentTask.copy(
                expectedAction = action,
                updatedAt = currentTimeMillis()
            )
            // Note: This would need a proper update use case in a real app
            viewState = viewState.copy(currentTask = updatedTask)
        }
    }

    /**
     * Clears any error message.
     */
    private fun handleClearError() {
        viewState = viewState.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        observeJob?.cancel()
    }
}
