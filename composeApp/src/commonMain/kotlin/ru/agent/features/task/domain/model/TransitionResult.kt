package ru.agent.features.task.domain.model

import ru.agent.features.task.domain.validator.TransitionViolation

/**
 * Result of a task stage transition operation.
 *
 * Contains the new task state (if successful) along with any warnings
 * that occurred during the transition.
 *
 * @property taskState The updated task state after transition, null if transition failed
 * @property warnings List of non-blocking violations (WARNING severity) that occurred during transition
 * @property isSuccess Whether the transition was successful
 */
data class TransitionResult(
    val taskState: TaskState?,
    val warnings: List<TransitionViolation> = emptyList(),
    val isSuccess: Boolean = taskState != null
) {
    /**
     * Returns true if there are any warnings.
     */
    fun hasWarnings(): Boolean = warnings.isNotEmpty()

    /**
     * Maps the task state to a new value if successful, otherwise returns null.
     */
    inline fun <R> map(transform: (TaskState) -> R): R? {
        return taskState?.let(transform)
    }

    /**
     * Maps the task state to a new value if successful, otherwise returns null.
     *
     * Unlike [map], this method returns nullable result from the transform function.
     *
     * @param transform The transformation function to apply to the task state
     * @return The transformed value, or null if transition failed
     */
    inline fun <R> mapOrNull(transform: (TaskState) -> R?): R? {
        return taskState?.let(transform)
    }

    companion object {
        /**
         * Creates a successful transition result without warnings.
         */
        fun success(taskState: TaskState): TransitionResult {
            return TransitionResult(
                taskState = taskState,
                warnings = emptyList()
            )
        }

        /**
         * Creates a successful transition result with warnings.
         */
        fun successWithWarnings(
            taskState: TaskState,
            warnings: List<TransitionViolation>
        ): TransitionResult {
            return TransitionResult(
                taskState = taskState,
                warnings = warnings
            )
        }

        /**
         * Creates a failed transition result.
         */
        fun failure(): TransitionResult {
            return TransitionResult(
                taskState = null,
                warnings = emptyList()
            )
        }
    }
}
