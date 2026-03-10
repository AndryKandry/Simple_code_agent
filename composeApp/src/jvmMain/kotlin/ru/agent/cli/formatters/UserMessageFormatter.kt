package ru.agent.cli.formatters

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import ru.agent.cli.visualization.domain.ProgressState
import ru.agent.features.invariant.domain.service.ValidationWarning
import ru.agent.features.task.domain.model.PlanStep
import ru.agent.features.task.domain.model.TaskStage
import ru.agent.features.task.domain.validator.TransitionViolation
import ru.agent.features.task.domain.validator.ViolationSeverity

/**
 * Formats system messages for user display.
 * Provides user-friendly, context-aware messages.
 */
object UserMessageFormatter {

    /**
     * Formats transition validation errors for display.
     */
    fun formatTransitionError(
        fromStage: TaskStage,
        toStage: TaskStage,
        violations: List<TransitionViolation>
    ): String {
        val errorViolations = violations.filter { it.severity == ViolationSeverity.ERROR }

        return buildString {
            appendLine(red(bold("Transition blocked:")))
            appendLine()
            errorViolations.forEach { violation ->
                appendLine(red("  - ${violation.userFriendlyMessage}"))
            }
        }
    }

    /**
     * Formats transition warnings (non-blocking violations) for display.
     *
     * @param warnings List of transition violations with WARNING severity
     * @return Formatted string with colored warnings
     */
    fun formatTransitionWarnings(
        warnings: List<TransitionViolation>
    ): String {
        if (warnings.isEmpty()) return ""

        return buildString {
            appendLine(yellow(bold("Transition Warnings:")))
            appendLine()
            warnings.forEach { warning ->
                appendLine(yellow("  [!] ${warning.userFriendlyMessage}"))
            }
        }
    }

    /**
     * Formats validation warnings for display.
     */
    fun formatValidationWarnings(
        warnings: List<ValidationWarning>
    ): String {
        if (warnings.isEmpty()) return ""

        return buildString {
            appendLine(yellow(bold("Warnings:")))
            warnings.forEach { warning ->
                appendLine(yellow("  - ${warning.userFriendlyMessage}"))
            }
        }
    }

    /**
     * Formats error message with actionable guidance.
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
     * Formats approval prompt for user.
     */
    fun formatApprovalPrompt(
        stage: TaskStage,
        taskName: String
    ): ApprovalPrompt {
        return when (stage) {
            TaskStage.PLANNING -> ApprovalPrompt(
                title = bold("Plan ready for review"),
                description = "Task: $taskName",
                options = listOf(
                    ApprovalOption(
                        keyword = "approve",
                        aliases = listOf("ok", "yes", "+"),
                        description = "Start execution"
                    ),
                    ApprovalOption(
                        keyword = "<feedback>",
                        aliases = listOf(),
                        description = "Provide feedback to revise the plan"
                    )
                ),
                defaultAction = "approve",
                examples = listOf("Looks good!", "add error handling", "simplify step 2")
            )

            TaskStage.VALIDATION -> ApprovalPrompt(
                title = bold("Result ready for review"),
                description = "Task: $taskName",
                options = listOf(
                    ApprovalOption(
                        keyword = "approve",
                        aliases = listOf("ok", "yes", "+"),
                        description = "Complete task"
                    ),
                    ApprovalOption(
                        keyword = "<feedback>",
                        aliases = listOf(),
                        description = "Request improvements"
                    )
                ),
                defaultAction = "approve",
                examples = listOf("Perfect!", "missing edge cases", "add more tests")
            )

            else -> ApprovalPrompt(
                title = "",
                description = "",
                options = emptyList(),
                defaultAction = "",
                examples = emptyList()
            )
        }
    }

    /**
     * Formats progress update for plan execution.
     */
    fun formatProgressUpdate(
        step: PlanStep,
        totalSteps: Int,
        status: StepStatus
    ): String {
        return when (status) {
            StepStatus.STARTED -> gray("Step ${step.number}/$totalSteps: ${step.description} started")
            StepStatus.COMPLETED -> green("Step ${step.number} completed")
            StepStatus.FAILED -> red("Step ${step.number} failed: ${step.description}")
            StepStatus.IN_PROGRESS -> cyan("Step ${step.number}/$totalSteps: ${step.description}")
        }
    }

    /**
     * Formats full approval guidance message.
     */
    fun formatApprovalGuidance(
        stage: TaskStage,
        taskName: String
    ): String {
        val prompt = formatApprovalPrompt(stage, taskName)

        return buildString {
            appendLine(cyan(prompt.title))
            if (prompt.description.isNotBlank()) {
                appendLine(gray(prompt.description))
            }
            appendLine()
            appendLine(bold("Available actions:"))
            prompt.options.forEach { option ->
                val aliases = if (option.aliases.isNotEmpty()) {
                    gray(" (${option.aliases.joinToString(", ")})")
                } else ""
                appendLine(green("  ${option.keyword}$aliases - ${option.description}"))
            }
            if (prompt.examples.isNotEmpty()) {
                appendLine()
                appendLine(gray("Examples: ${prompt.examples.take(3).joinToString(", ")}"))
            }
        }
    }

    /**
     * Formats plan steps for display.
     */
    fun formatPlanSteps(steps: List<PlanStep>, fallbackPlan: String?): String {
        return if (steps.isNotEmpty()) {
            steps.joinToString("\n") { "  ${it.number}. ${it.description}" }
        } else {
            fallbackPlan ?: "No detailed plan available"
        }
    }

    /**
     * Formats a list of suggestions for the user.
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

    /**
     * Format interrupt message.
     */
    fun formatInterrupt(
        state: ProgressState.Interrupted,
        saved: Boolean
    ): String {
        val savedText = if (saved) {
            green(" [State saved]")
        } else {
            gray(" [State not saved]")
        }

        return buildString {
            appendLine(yellow(bold("Interrupted")))
            appendLine(yellow(state.message))
            append(savedText)

            if (state.canResume) {
                appendLine()
                append(gray("  Tip: You can resume this operation"))
            }
        }
    }
}

/**
 * Approval prompt configuration.
 */
data class ApprovalPrompt(
    val title: String,
    val description: String,
    val options: List<ApprovalOption>,
    val defaultAction: String,
    val examples: List<String>
)

/**
 * Single approval option.
 */
data class ApprovalOption(
    val keyword: String,
    val aliases: List<String>,
    val description: String
)

/**
 * Status of a step execution.
 */
enum class StepStatus {
    STARTED,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}
