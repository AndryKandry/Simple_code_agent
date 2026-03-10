package ru.agent.features.task.domain.validator

import ru.agent.features.task.domain.model.TaskStage
import ru.agent.features.task.domain.model.TaskState

/**
 * Validates business rules for task stage transitions.
 * Separates state machine rules (canTransitionTo) from business rules.
 */
interface TaskTransitionValidator {
    /**
     * Validates if a transition from current stage to target stage is allowed
     * according to business rules.
     *
     * @param currentState Current task state
     * @param targetStage Target stage to transition to
     * @return Validation result with any violations
     */
    fun validateTransition(
        currentState: TaskState,
        targetStage: TaskStage
    ): TransitionValidationResult
}

/**
 * Result of transition validation.
 */
data class TransitionValidationResult(
    val isValid: Boolean,
    val violations: List<TransitionViolation>
)

/**
 * A single violation of business rules.
 */
data class TransitionViolation(
    val code: String,
    val message: String,
    val userFriendlyMessage: String,
    val severity: ViolationSeverity
)

/**
 * Severity of the violation.
 */
enum class ViolationSeverity {
    ERROR,   // Blocks transition
    WARNING  // Allows transition but warns user
}
