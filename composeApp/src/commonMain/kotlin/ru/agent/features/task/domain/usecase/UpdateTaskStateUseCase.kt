package ru.agent.features.task.domain.usecase

import ru.agent.features.task.domain.model.TaskState
import ru.agent.features.task.domain.repository.TaskStateRepository

/**
 * Use case for updating task state directly.
 * Used when we need to save task modifications before stage transitions.
 */
class UpdateTaskStateUseCase(
    private val repository: TaskStateRepository
) {
    /**
     * Updates the task state in the repository.
     *
     * @param taskState The task state to save
     * @return The saved task state
     */
    suspend operator fun invoke(taskState: TaskState): TaskState {
        repository.saveTaskState(taskState)
        return taskState
    }
}
