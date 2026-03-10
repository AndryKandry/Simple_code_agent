package ru.agent.features.task.domain.validator

import ru.agent.features.task.domain.model.TaskStage
import ru.agent.features.task.domain.model.TaskState

/**
 * Implementation of TaskTransitionValidator.
 * Enforces business rules for task stage transitions.
 */
class TaskTransitionValidatorImpl : TaskTransitionValidator {

    override fun validateTransition(
        currentState: TaskState,
        targetStage: TaskStage
    ): TransitionValidationResult {
        val violations = mutableListOf<TransitionViolation>()

        when {
            // PLANNING -> EXECUTION: Requires plan
            currentState.taskStage == TaskStage.PLANNING && targetStage == TaskStage.EXECUTION -> {
                if (!currentState.hasPlan()) {
                    violations.add(
                        TransitionViolation(
                            code = CODE_NO_PLAN,
                            message = "Cannot start execution without a plan",
                            userFriendlyMessage = "Cannot proceed to execution: a plan is required first. Please wait for plan generation or provide feedback.",
                            severity = ViolationSeverity.ERROR
                        )
                    )
                }
            }

            // EXECUTION -> VALIDATION: Warn if no result (but allow)
            currentState.taskStage == TaskStage.EXECUTION && targetStage == TaskStage.VALIDATION -> {
                if (!currentState.hasExecutionResult()) {
                    violations.add(
                        TransitionViolation(
                            code = CODE_NO_RESULT_WARNING,
                            message = "No execution result to validate",
                            userFriendlyMessage = "Warning: No execution result available for validation. Proceed with caution.",
                            severity = ViolationSeverity.WARNING
                        )
                    )
                }
            }

            // EXECUTION -> DONE (skip validation): Requires result
            currentState.taskStage == TaskStage.EXECUTION && targetStage == TaskStage.DONE -> {
                if (!currentState.hasExecutionResult()) {
                    violations.add(
                        TransitionViolation(
                            code = CODE_NO_RESULT_ERROR,
                            message = "Cannot complete task without execution result",
                            userFriendlyMessage = "Cannot complete task: no execution result available. Please wait for execution to finish or provide feedback.",
                            severity = ViolationSeverity.ERROR
                        )
                    )
                }
            }

            // VALIDATION -> DONE: Requires result
            currentState.taskStage == TaskStage.VALIDATION && targetStage == TaskStage.DONE -> {
                if (!currentState.hasExecutionResult()) {
                    violations.add(
                        TransitionViolation(
                            code = CODE_NO_RESULT_ERROR,
                            message = "Cannot complete task without execution result",
                            userFriendlyMessage = "Cannot complete task: no execution result available. Please wait for execution to finish or provide feedback.",
                            severity = ViolationSeverity.ERROR
                        )
                    )
                }
            }
        }

        return TransitionValidationResult(
            isValid = violations.none { it.severity == ViolationSeverity.ERROR },
            violations = violations
        )
    }

    /**
     * Extension to check if task has a plan.
     */
    private fun TaskState.hasPlan(): Boolean {
        return !plan.isNullOrEmpty() || planSteps.isNotEmpty()
    }

    /**
     * Extension to check if task has execution result.
     */
    private fun TaskState.hasExecutionResult(): Boolean {
        return !executionResult.isNullOrEmpty()
    }

    companion object {
        const val CODE_NO_PLAN = "NO_PLAN"
        const val CODE_NO_RESULT_WARNING = "NO_RESULT_WARNING"
        const val CODE_NO_RESULT_ERROR = "NO_RESULT_ERROR"
    }
}
