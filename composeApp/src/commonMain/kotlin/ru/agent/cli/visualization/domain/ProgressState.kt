package ru.agent.cli.visualization.domain

/**
 * Progress state for CLI visualization.
 *
 * Represents the current state of a task or operation progress.
 */
sealed class ProgressState {
    /**
     * No active progress.
     */
    data object Idle : ProgressState()

    /**
     * Progress is active with current status.
     *
     * @param message Current progress message
     * @param currentStep Current step number (1-based)
     * @param totalSteps Total number of steps (0 if unknown)
     * @param percentage Completion percentage (0-100)
     * @param subProgress Sub-task progress (optional)
     */
    data class InProgress(
        val message: String,
        val currentStep: Int = 0,
        val totalSteps: Int = 0,
        val percentage: Int = 0,
        val subProgress: SubProgress? = null
    ) : ProgressState() {
        /**
         * Get formatted step info (e.g., "2/5").
         */
        fun getStepInfo(): String? = if (totalSteps > 0) {
            "$currentStep/$totalSteps"
        } else {
            null
        }

        /**
         * Get progress bar percentage (clamped to 0-100).
         */
        fun getClampedPercentage(): Int = percentage.coerceIn(0, 100)
    }

    /**
     * Progress completed successfully.
     *
     * @param message Completion message
     * @param durationMs Duration in milliseconds
     */
    data class Completed(
        val message: String,
        val durationMs: Long? = null
    ) : ProgressState()

    /**
     * Progress failed with error.
     *
     * @param message Error message
     * @param error Exception that caused the failure
     */
    data class Failed(
        val message: String,
        val error: Throwable? = null
    ) : ProgressState()

    /**
     * Progress was interrupted (e.g., Ctrl+C).
     *
     * @param message Interruption message
     * @param canResume Whether the operation can be resumed
     */
    data class Interrupted(
        val message: String = "Operation interrupted",
        val canResume: Boolean = false
    ) : ProgressState()

    /**
     * Sub-progress for nested operations.
     *
     * @param message Sub-task message
     * @param current Current item
     * @param total Total items
     */
    data class SubProgress(
        val message: String,
        val current: Int = 0,
        val total: Int = 0
    ) {
        /**
         * Get sub-progress percentage.
         */
        fun getPercentage(): Int = if (total > 0) {
            ((current.toDouble() / total) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
    }
}
