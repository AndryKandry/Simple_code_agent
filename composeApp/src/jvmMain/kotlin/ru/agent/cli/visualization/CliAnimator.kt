package ru.agent.cli.visualization

import co.touchlab.kermit.Logger
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import ru.agent.cli.visualization.domain.ProgressState

/**
 * CLI Animator for spinners, progress bars, and animations.
 *
 * Provides:
 * - Animated spinners with Unicode/ASCII fallback
 * - Progress bar visualization
 * - Success/Error animations
 * - Real-time progress updates
 */
class CliAnimator(
    private val terminal: Terminal = Terminal(),
    private val capabilities: TerminalCapabilities.Capabilities = TerminalCapabilities.getCapabilities()
) {
    private val logger = Logger.withTag("CliAnimator")

    private var spinnerJob: Job? = null
    private val animatorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Spinner state
    private val spinnerChars = capabilities.getSpinnerChars()
    private var spinnerIndex = 0

    // Status icons
    private val statusIcons = capabilities.getStatusIcons()

    /**
     * Show animated spinner while waiting for an operation.
     * Uses a dedicated line for the spinner to avoid overlapping with previous output.
     *
     * @param message Message to display with spinner
     * @param stateFlow Optional StateFlow to monitor for completion
     * @return Job that can be cancelled to stop spinner
     */
    fun showSpinner(
        message: String,
        stateFlow: StateFlow<ProgressState>? = null
    ): Job {
        // Cancel existing spinner if any
        stopSpinner()

        spinnerJob = animatorScope.launch {
            try {
                val chars = spinnerChars
                var lastMessage = message

                while (isActive) {
                    // Check state flow for completion
                    val state = stateFlow?.value
                    if (state != null && state !is ProgressState.InProgress) {
                        // State changed, stop spinner
                        break
                    }

                    // Update message if state changed
                    val currentMessage = (state as? ProgressState.InProgress)?.message ?: message

                    // Print spinner frame on its own visual area
                    val char = chars[spinnerIndex % chars.size]
                    val fullLine = "${cyan(char)} ${bold(currentMessage)}"

                    // Use carriage return + clear line to update in place
                    // This keeps the spinner on its own line
                    terminal.print("\r\u001B[K$fullLine")

                    lastMessage = currentMessage
                    spinnerIndex++
                    delay(SPINNER_DELAY_MS)
                }

                // Clear spinner line and move to next line
                terminal.print("\r\u001B[K")

            } catch (e: CancellationException) {
                // Expected when spinner is stopped
                terminal.print("\r\u001B[K")
            } catch (e: Exception) {
                logger.e(throwable = e) { "Spinner error" }
            }
        }

        return spinnerJob!!
    }

    /**
     * Stop the animated spinner.
     */
    fun stopSpinner() {
        spinnerJob?.cancel()
        spinnerJob = null
        spinnerIndex = 0
    }

    /**
     * Show success animation with checkmark.
     *
     * @param message Success message
     */
    fun showSuccess(message: String) {
        terminal.println("${green(statusIcons.success)} ${green(bold(message))}")
    }

    /**
     * Show error animation with X mark.
     *
     * @param message Error message
     */
    fun showError(message: String) {
        terminal.println("${red(statusIcons.error)} ${red(bold(message))}")
    }

    /**
     * Show warning message.
     *
     * @param message Warning message
     */
    fun showWarning(message: String) {
        terminal.println("${yellow(statusIcons.warning)} ${yellow(message)}")
    }

    /**
     * Show info message.
     *
     * @param message Info message
     */
    fun showInfo(message: String) {
        terminal.println("${blue(statusIcons.info)} $message")
    }

    /**
     * Display a progress bar with step info.
     *
     * @param state Current progress state
     * @param showSubProgress Whether to show sub-progress
     * @return Formatted progress bar string
     */
    fun formatProgressBar(
        state: ProgressState.InProgress,
        showSubProgress: Boolean = true
    ): String {
        val progressChars = capabilities.getProgressChars()
        val percentage = state.getClampedPercentage()
        val stepInfo = state.getStepInfo()

        // Build progress bar
        val barWidth = PROGRESS_BAR_WIDTH
        val filledWidth = (percentage / 100.0 * barWidth).toInt()
        val emptyWidth = barWidth - filledWidth

        val bar = buildString {
            // Color based on percentage
            val barColor = when {
                percentage < 30 -> red
                percentage < 70 -> yellow
                else -> green
            }

            append(barColor(progressChars.filled.repeat(filledWidth)))
            append(gray(progressChars.empty.repeat(emptyWidth)))
        }

        // Build status line
        return buildString {
            append("$bar ")

            // Percentage
            append(bold("${percentage}%"))

            // Step info
            if (stepInfo != null) {
                append(gray(" [$stepInfo]"))
            }

            // Message
            append(" ${state.message}")

            // Sub-progress
            if (showSubProgress && state.subProgress != null) {
                val sub = state.subProgress
                val subPercent = sub.getPercentage()
                append("\n")
                append(gray("  └─ ${sub.message} [$subPercent%]"))
            }
        }
    }

    /**
     * Display real-time progress updates.
     *
     * @param stateFlow Progress state flow to monitor
     * @param onComplete Callback when progress completes
     * @return Job monitoring the progress
     */
    fun monitorProgress(
        stateFlow: StateFlow<ProgressState>,
        onComplete: ((ProgressState) -> Unit)? = null
    ): Job {
        return animatorScope.launch {
            var lastPercentage = -1

            stateFlow.collect { state ->
                when (state) {
                    is ProgressState.Idle -> {
                        // Do nothing
                    }

                    is ProgressState.InProgress -> {
                        // Only update if percentage changed significantly
                        if (state.percentage != lastPercentage) {
                            terminal.print("\r\u001B[K")
                            terminal.print(formatProgressBar(state))
                            lastPercentage = state.percentage
                        }
                    }

                    is ProgressState.Completed -> {
                        terminal.print("\r\u001B[K")
                        val duration = state.durationMs?.let { " (${it}ms)" } ?: ""
                        showSuccess("${state.message}$duration")
                        onComplete?.invoke(state)
                        cancel()
                    }

                    is ProgressState.Failed -> {
                        terminal.print("\r\u001B[K")
                        showError(state.message)
                        onComplete?.invoke(state)
                        cancel()
                    }

                    is ProgressState.Interrupted -> {
                        terminal.print("\r\u001B[K")
                        showWarning(state.message)
                        onComplete?.invoke(state)
                        cancel()
                    }
                }
            }
        }
    }

    /**
     * Display interrupted progress with save indicator.
     *
     * @param state Interrupted state
     * @param saved Whether state was saved
     */
    fun showInterrupted(state: ProgressState.Interrupted, saved: Boolean) {
        val savedText = if (saved) {
            green(" [State saved]")
        } else {
            gray(" [State not saved]")
        }

        terminal.println("${yellow(statusIcons.warning)} ${yellow(state.message)}$savedText")

        if (state.canResume) {
            terminal.println(gray("  Tip: You can resume this operation"))
        }
    }

    /**
     * Cleanup resources.
     */
    fun cleanup() {
        stopSpinner()
        animatorScope.cancel()
    }

    companion object {
        const val SPINNER_DELAY_MS = 80L
        const val PROGRESS_BAR_WIDTH = 20
    }
}
