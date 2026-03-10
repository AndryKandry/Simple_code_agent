package ru.agent.cli.formatters

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import ru.agent.features.invariant.domain.service.ValidationWarning
import ru.agent.features.task.domain.validator.TransitionViolation
import ru.agent.features.task.domain.validator.ViolationSeverity

/**
 * Formatter for validation status and related messages.
 *
 * Provides colored, user-friendly formatting for:
 * - Transition warnings (non-blocking violations during stage changes)
 * - Validation warnings (invariant violations)
 * - Validation indicators for prompt
 * - Error guidance with actionable suggestions
 */
object ValidationStatusFormatter {

    /**
     * Formats transition warnings for display to user.
     *
     * Shows a summary header and list of warnings with user-friendly messages.
     *
     * @param warnings List of transition violations with WARNING severity
     * @param showHeader Whether to show a header before warnings
     * @return Formatted string with colored warnings
     */
    fun formatTransitionWarnings(
        warnings: List<TransitionViolation>,
        showHeader: Boolean = true
    ): String {
        if (warnings.isEmpty()) return ""

        return buildString {
            if (showHeader) {
                appendLine(yellow(bold("Transition Warnings:")))
                appendLine()
            }
            warnings.forEach { warning ->
                appendLine(yellow("  [!] ${warning.userFriendlyMessage}"))
            }
        }
    }

    /**
     * Formats validation warnings for display to user.
     *
     * @param warnings List of validation warnings
     * @param showHeader Whether to show a header before warnings
     * @return Formatted string with colored warnings
     */
    fun formatValidationWarnings(
        warnings: List<ValidationWarning>,
        showHeader: Boolean = true
    ): String {
        if (warnings.isEmpty()) return ""

        return buildString {
            if (showHeader) {
                appendLine(yellow(bold("Validation Warnings:")))
                appendLine()
            }
            warnings.forEach { warning ->
                appendLine(yellow("  [!] ${warning.userFriendlyMessage}"))
            }
        }
    }

    /**
     * Creates a compact validation indicator for prompt display.
     *
     * Shows visual indicator based on validation state:
     * - Green checkmark for valid state
     * - Yellow warning sign if there are warnings
     * - Red X if there are errors
     *
     * @param hasWarnings Whether there are validation warnings
     * @param hasErrors Whether there are validation errors
     * @return Colored indicator string
     */
    fun createValidationIndicator(
        hasWarnings: Boolean,
        hasErrors: Boolean = false
    ): String {
        return when {
            hasErrors -> red("X")
            hasWarnings -> yellow("!")
            else -> green(".")
        }
    }

    /**
     * Formats error message with actionable guidance.
     *
     * Provides:
     * - Error message in red
     * - Context about what went wrong
     * - Suggested actions to fix the issue
     *
     * @param error The error message
     * @param context Optional context about where the error occurred
     * @param suggestions List of suggested actions to resolve the error
     * @return Formatted error guidance string
     */
    fun formatErrorWithGuidance(
        error: String,
        context: String? = null,
        suggestions: List<String> = emptyList()
    ): String {
        return buildString {
            appendLine(red(bold("Error:")))
            appendLine(red("  $error"))

            if (context != null) {
                appendLine()
                appendLine(gray("Context: $context"))
            }

            if (suggestions.isNotEmpty()) {
                appendLine()
                appendLine(cyan("Suggested actions:"))
                suggestions.forEach { suggestion ->
                    appendLine(cyan("  - $suggestion"))
                }
            }
        }
    }

    /**
     * Formats transition error with guidance.
     *
     * Specialized formatter for transition-related errors.
     *
     * @param violations List of transition violations with ERROR severity
     * @return Formatted error guidance string
     */
    fun formatTransitionErrorWithGuidance(
        violations: List<TransitionViolation>
    ): String {
        val errorViolations = violations.filter { it.severity == ViolationSeverity.ERROR }

        if (errorViolations.isEmpty()) return ""

        return buildString {
            appendLine(red(bold("Transition blocked:")))
            appendLine()

            errorViolations.forEach { violation ->
                appendLine(red("  [X] ${violation.userFriendlyMessage}"))

                // Add guidance based on violation code
                val guidance = getTransitionGuidance(violation.code)
                if (guidance.isNotEmpty()) {
                    appendLine(gray("      $guidance"))
                }
            }
        }
    }

    /**
     * Gets actionable guidance for a specific violation code.
     *
     * @param code The violation code
     * @return Guidance string, empty if no specific guidance available
     */
    private fun getTransitionGuidance(code: String): String {
        return when (code) {
            "NO_PLAN" -> "Wait for plan generation or provide feedback to generate a new plan."
            "NO_RESULT_WARNING" -> "You can proceed, but validation may be limited without execution results."
            "NO_RESULT_ERROR" -> "Wait for execution to complete or check if there was an execution error."
            else -> ""
        }
    }

    /**
     * Formats a validation status summary.
     *
     * Shows a compact status line suitable for status displays.
     *
     * @param isValid Whether validation passed
     * @param warningCount Number of warnings
     * @param errorCount Number of errors
     * @return Formatted status line
     */
    fun formatStatusSummary(
        isValid: Boolean,
        warningCount: Int,
        errorCount: Int
    ): String {
        return buildString {
            if (isValid) {
                append(green("OK"))
            } else {
                append(red("FAILED"))
            }

            if (warningCount > 0) {
                append(" ")
                append(yellow("($warningCount warning${if (warningCount > 1) "s" else ""})"))
            }

            if (errorCount > 0) {
                append(" ")
                append(red("($errorCount error${if (errorCount > 1) "s" else ""})"))
            }
        }
    }

    /**
     * Creates a bullet point list of suggestions.
     *
     * @param suggestions List of suggestion strings
     * @return Formatted list with cyan color
     */
    fun formatSuggestions(suggestions: List<String>): String {
        if (suggestions.isEmpty()) return ""

        return buildString {
            appendLine(cyan("Suggestions:"))
            suggestions.forEach { suggestion ->
                appendLine(cyan("  - $suggestion"))
            }
        }
    }
}
