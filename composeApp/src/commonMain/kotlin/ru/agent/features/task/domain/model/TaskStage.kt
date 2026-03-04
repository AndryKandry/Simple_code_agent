package ru.agent.features.task.domain.model

import kotlinx.serialization.Serializable

/**
 * Enum representing the stages of a task lifecycle.
 *
 * Task flow: PLANNING -> EXECUTION -> VALIDATION -> DONE
 * VALIDATION is mandatory - user must confirm the result.
 */
@Serializable
enum class TaskStage {
    PLANNING,
    EXECUTION,
    VALIDATION,
    DONE;

    /**
     * Validates if a transition from this stage to [targetStage] is allowed.
     *
     * Valid transitions:
     * - PLANNING -> EXECUTION
     * - EXECUTION -> VALIDATION (mandatory)
     * - VALIDATION -> DONE (user confirmed)
     * - VALIDATION -> EXECUTION (user rejected, retry)
     * - Any stage -> same stage (no transition)
     *
     * @param targetStage The stage to transition to
     * @return true if transition is valid, false otherwise
     */
    fun canTransitionTo(targetStage: TaskStage): Boolean {
        return when (this) {
            PLANNING -> targetStage == EXECUTION || targetStage == PLANNING
            EXECUTION -> targetStage == VALIDATION || targetStage == EXECUTION
            VALIDATION -> targetStage == DONE || targetStage == EXECUTION || targetStage == VALIDATION
            DONE -> targetStage == DONE // No transitions from DONE
        }
    }

    /**
     * Returns the next stage in the normal flow.
     * Returns null if this is the final stage (DONE).
     */
    fun nextStage(): TaskStage? {
        return when (this) {
            PLANNING -> EXECUTION
            EXECUTION -> VALIDATION
            VALIDATION -> DONE
            DONE -> null
        }
    }

    /**
     * Returns the display name for this stage.
     */
    fun displayName(): String {
        return when (this) {
            PLANNING -> "Planning"
            EXECUTION -> "Execution"
            VALIDATION -> "Validation"
            DONE -> "Done"
        }
    }
}
