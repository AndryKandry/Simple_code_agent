package ru.agent.cli.visualization

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.agent.cli.visualization.domain.ProgressState

/**
 * Progress tracking service for CLI operations.
 *
 * Provides:
 * - Overall progress tracking
 * - Step-by-step progress
 * - Sub-progress for nested operations
 * - Thread-safe state management via StateFlow
 */
class ProgressTracker {
    private val logger = Logger.withTag("ProgressTracker")

    private val _progressState = MutableStateFlow<ProgressState>(ProgressState.Idle)
    val progressState: StateFlow<ProgressState> = _progressState.asStateFlow()

    // Timing
    private var startTime: Long? = null

    /**
     * Start tracking progress.
     *
     * @param message Initial progress message
     * @param totalSteps Total number of steps (0 if unknown)
     */
    fun start(message: String, totalSteps: Int = 0) {
        startTime = System.currentTimeMillis()
        _progressState.value = ProgressState.InProgress(
            message = message,
            currentStep = 0,
            totalSteps = totalSteps,
            percentage = 0
        )
        logger.d { "Progress started: $message ($totalSteps steps)" }
    }

    /**
     * Update progress with new message and percentage.
     *
     * @param message Progress message
     * @param percentage Completion percentage (0-100)
     */
    fun update(message: String, percentage: Int) {
        val currentState = _progressState.value
        if (currentState is ProgressState.InProgress) {
            _progressState.value = currentState.copy(
                message = message,
                percentage = percentage.coerceIn(0, 100)
            )
        } else {
            logger.w { "Attempted to update progress while not in progress" }
        }
    }

    /**
     * Update step progress.
     *
     * @param stepNumber Current step (1-based)
     * @param message Step message
     */
    fun updateStep(stepNumber: Int, message: String) {
        val currentState = _progressState.value
        if (currentState is ProgressState.InProgress) {
            val totalSteps = currentState.totalSteps
            val percentage = if (totalSteps > 0) {
                ((stepNumber.toDouble() / totalSteps) * 100).toInt()
            } else {
                currentState.percentage
            }

            _progressState.value = currentState.copy(
                message = message,
                currentStep = stepNumber,
                percentage = percentage.coerceIn(0, 100)
            )
            logger.d { "Step $stepNumber/$totalSteps: $message" }
        }
    }

    /**
     * Update sub-progress for nested operations.
     *
     * @param message Sub-task message
     * @param current Current item
     * @param total Total items
     */
    fun updateSubProgress(message: String, current: Int, total: Int) {
        val currentState = _progressState.value
        if (currentState is ProgressState.InProgress) {
            val subProgress = ProgressState.SubProgress(
                message = message,
                current = current,
                total = total
            )
            _progressState.value = currentState.copy(subProgress = subProgress)
            logger.d { "Sub-progress: $message ($current/$total)" }
        }
    }

    /**
     * Clear sub-progress.
     */
    fun clearSubProgress() {
        val currentState = _progressState.value
        if (currentState is ProgressState.InProgress) {
            _progressState.value = currentState.copy(subProgress = null)
        }
    }

    /**
     * Mark progress as completed successfully.
     *
     * @param message Completion message
     */
    fun complete(message: String = "Completed") {
        val duration = startTime?.let { System.currentTimeMillis() - it }
        _progressState.value = ProgressState.Completed(
            message = message,
            durationMs = duration
        )
        logger.i { "Progress completed: $message (${duration}ms)" }
        startTime = null
    }

    /**
     * Mark progress as failed.
     *
     * @param message Error message
     * @param error Exception that caused the failure
     */
    fun fail(message: String, error: Throwable? = null) {
        _progressState.value = ProgressState.Failed(
            message = message,
            error = error
        )
        logger.e(throwable = error) { "Progress failed: $message" }
        startTime = null
    }

    /**
     * Mark progress as interrupted (e.g., Ctrl+C).
     *
     * @param message Interruption message
     * @param canResume Whether the operation can be resumed
     */
    fun interrupt(message: String = "Operation interrupted", canResume: Boolean = false) {
        _progressState.value = ProgressState.Interrupted(
            message = message,
            canResume = canResume
        )
        logger.w { "Progress interrupted: $message (canResume=$canResume)" }
        // Don't clear startTime - keep it for potential resume
    }

    /**
     * Reset progress to idle state.
     */
    fun reset() {
        _progressState.value = ProgressState.Idle
        startTime = null
        logger.d { "Progress reset" }
    }

    /**
     * Get current progress state.
     */
    fun getState(): ProgressState = _progressState.value

    /**
     * Check if progress is active.
     */
    fun isInProgress(): Boolean = _progressState.value is ProgressState.InProgress

    /**
     * Get elapsed time in milliseconds.
     */
    fun getElapsedTime(): Long? {
        return startTime?.let { System.currentTimeMillis() - it }
    }

    /**
     * Create a snapshot of current progress for saving.
     */
    fun createSnapshot(): ProgressSnapshot? {
        val state = _progressState.value
        return if (state is ProgressState.InProgress) {
            ProgressSnapshot(
                message = state.message,
                currentStep = state.currentStep,
                totalSteps = state.totalSteps,
                percentage = state.percentage,
                subProgress = state.subProgress,
                elapsedTime = getElapsedTime()
            )
        } else {
            null
        }
    }

    /**
     * Restore progress from snapshot.
     */
    fun restoreFromSnapshot(snapshot: ProgressSnapshot) {
        startTime = System.currentTimeMillis() - (snapshot.elapsedTime ?: 0)
        _progressState.value = ProgressState.InProgress(
            message = snapshot.message,
            currentStep = snapshot.currentStep,
            totalSteps = snapshot.totalSteps,
            percentage = snapshot.percentage,
            subProgress = snapshot.subProgress
        )
        logger.i { "Progress restored from snapshot: ${snapshot.message}" }
    }

    /**
     * Progress snapshot for persistence.
     */
    data class ProgressSnapshot(
        val message: String,
        val currentStep: Int,
        val totalSteps: Int,
        val percentage: Int,
        val subProgress: ProgressState.SubProgress?,
        val elapsedTime: Long?
    )
}
