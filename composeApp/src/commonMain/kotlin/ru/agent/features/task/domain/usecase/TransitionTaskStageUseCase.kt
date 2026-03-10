package ru.agent.features.task.domain.usecase

import ru.agent.core.time.currentTimeMillis
import ru.agent.features.task.domain.model.TaskStage
import ru.agent.features.task.domain.model.TaskState
import ru.agent.features.task.domain.model.TaskTransition
import ru.agent.features.task.domain.model.TransitionResult
import ru.agent.features.task.domain.repository.TaskStateRepository
import ru.agent.features.task.domain.validator.TaskTransitionValidator
import ru.agent.features.task.domain.validator.ViolationSeverity

/**
 * Use case for transitioning a task to a new stage.
 *
 * Validates transitions according to the state machine rules:
 * - PLANNING -> EXECUTION
 * - EXECUTION -> VALIDATION
 * - EXECUTION -> DONE (skip validation)
 * - VALIDATION -> DONE
 *
 * Also validates business rules via TaskTransitionValidator:
 * - PLANNING -> EXECUTION: Requires plan
 * - EXECUTION -> VALIDATION: Warns if no result (allows)
 * - VALIDATION -> DONE: Requires result
 *
 * Returns [TransitionResult] with updated state and any warnings.
 *
 * ## API Versioning
 *
 * **BREAKING CHANGE (v2.0):** The main [invoke] method now returns [TransitionResult] instead of [TaskState]?.
 *
 * ### Migration Guide:
 *
 * **Before (v1.x):**
 * ```kotlin
 * val task: TaskState? = transitionTaskStageUseCase(taskId, targetStage)
 * if (task != null) { ... }
 * ```
 *
 * **After (v2.0):**
 * ```kotlin
 * // Option 1: Full result with warnings
 * val result: TransitionResult = transitionTaskStageUseCase(taskId, targetStage)
 * if (result.taskState != null) {
 *     // Check warnings
 *     if (result.hasWarnings()) {
 *         println("Warnings: ${result.warnings}")
 *     }
 * }
 *
 * // Option 2: Legacy behavior (warnings are ignored)
 * val task: TaskState? = transitionTaskStageUseCase.transitionWithoutWarnings(taskId, targetStage)
 * if (task != null) { ... }
 * ```
 *
 * @see TransitionResult
 * @see transitionWithoutWarnings for backward compatibility
 */
class TransitionTaskStageUseCase(
    private val repository: TaskStateRepository,
    private val transitionValidator: TaskTransitionValidator
) {
    /**
     * Transitions a task to a new stage.
     *
     * @param taskId The task ID
     * @param targetStage The target stage to transition to
     * @param reason Optional reason for the transition
     * @param contextSnapshot Optional context data to preserve
     * @return [TransitionResult] with the updated task state and any warnings
     * @throws IllegalStateException if transition is invalid (ERROR severity violations)
     */
    suspend operator fun invoke(
        taskId: String,
        targetStage: TaskStage,
        reason: String? = null,
        contextSnapshot: Map<String, String> = emptyMap()
    ): TransitionResult {
        val currentState = repository.getTaskState(taskId)
            ?: return TransitionResult.failure()

        // Validate state machine transition
        if (!currentState.taskStage.canTransitionTo(targetStage)) {
            throw IllegalStateException(
                "Invalid transition from ${currentState.taskStage} to $targetStage"
            )
        }

        // Validate business rules
        val validationResult = transitionValidator.validateTransition(currentState, targetStage)
        if (!validationResult.isValid) {
            val errorMessage = validationResult.violations
                .filter { it.severity == ViolationSeverity.ERROR }
                .joinToString("\n") { it.userFriendlyMessage }
            throw IllegalStateException(errorMessage)
        }

        // Collect warnings (WARNING severity violations)
        val warnings = validationResult.violations.filter { it.severity == ViolationSeverity.WARNING }

        // Don't transition if already at target stage
        if (currentState.taskStage == targetStage) {
            return TransitionResult.successWithWarnings(currentState, warnings)
        }

        // Create transition record
        val transition = TaskTransition(
            fromStage = currentState.taskStage,
            toStage = targetStage,
            timestamp = currentTimeMillis(),
            reason = reason,
            contextSnapshot = contextSnapshot
        )

        // Create updated state
        val updatedState = currentState.copy(
            taskStage = targetStage,
            transitionHistory = currentState.transitionHistory + transition,
            updatedAt = currentTimeMillis()
        )

        // Save and return with warnings
        repository.saveTaskState(updatedState)
        return TransitionResult.successWithWarnings(updatedState, warnings)
    }

    /**
     * Legacy method for backward compatibility.
     * Returns only the task state without warnings.
     *
     * @deprecated Use [invoke] which returns [TransitionResult] instead
     * @param taskId The task ID
     * @param targetStage The target stage to transition to
     * @param reason Optional reason for the transition
     * @param contextSnapshot Optional context data to preserve
     * @return The updated task state, or null if transition failed
     * @throws IllegalStateException if transition is invalid
     */
    suspend fun transitionWithoutWarnings(
        taskId: String,
        targetStage: TaskStage,
        reason: String? = null,
        contextSnapshot: Map<String, String> = emptyMap()
    ): TaskState? {
        return invoke(taskId, targetStage, reason, contextSnapshot).taskState
    }

    /**
     * Advances the task to the next stage in the normal flow.
     *
     * @param taskId The task ID
     * @param reason Optional reason for the transition
     * @return [TransitionResult] with the updated task state and any warnings, or failure if not found or already at DONE
     */
    suspend fun advanceToNextStage(
        taskId: String,
        reason: String? = null
    ): TransitionResult {
        val currentState = repository.getTaskState(taskId)
            ?: return TransitionResult.failure()

        val nextStage = currentState.taskStage.nextStage()
            ?: return TransitionResult.failure() // Already at DONE

        return invoke(taskId, nextStage, reason)
    }

    /**
     * Skips validation and moves directly to DONE from EXECUTION.
     *
     * @param taskId The task ID
     * @param reason Optional reason for skipping validation
     * @return [TransitionResult] with the updated task state and any warnings, or failure if not found or not in EXECUTION
     */
    suspend fun skipValidationAndComplete(
        taskId: String,
        reason: String? = "Validation skipped"
    ): TransitionResult {
        val currentState = repository.getTaskState(taskId)
            ?: return TransitionResult.failure()

        if (currentState.taskStage != TaskStage.EXECUTION) {
            return TransitionResult.failure()
        }

        return invoke(taskId, TaskStage.DONE, reason)
    }
}
