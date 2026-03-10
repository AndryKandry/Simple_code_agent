package ru.agent.cli.formatters

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import ru.agent.cli.visualization.TerminalCapabilities
import ru.agent.cli.visualization.domain.ProgressState
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.memory.domain.model.*
import ru.agent.features.task.domain.model.TaskState
import ru.agent.features.task.domain.model.TaskStage

/**
 * Output formatter for CLI display.
 *
 * Provides formatted output for various data types:
 * - Messages (with timestamps and sender identification)
 * - Profiles (structured user information)
 * - Tasks (progress bars and status)
 * - Memory (context visualization)
 * - Progress visualization with Unicode/ASCII fallback
 */
object OutputFormatter {

    // Terminal capabilities for graceful degradation
    private val capabilities = TerminalCapabilities.getCapabilities()
    private val progressChars = capabilities.getProgressChars()
    private val statusIcons = capabilities.getStatusIcons()

    /**
     * Format chat message for display.
     */
    fun formatMessage(message: Message): String {
        val sender = when (message.senderType) {
            ru.agent.features.chat.domain.model.SenderType.USER -> cyan("You")
            ru.agent.features.chat.domain.model.SenderType.ASSISTANT -> green("Agent")
            ru.agent.features.chat.domain.model.SenderType.SYSTEM -> yellow("System")
        }

        val timestamp = gray(formatTimestamp(message.timestamp))
        val content = message.content

        return buildString {
            append(bold("$sender $timestamp"))
            append("\n")
            append(content)
        }
    }

    /**
     * Format user profile for display.
     */
    fun formatProfile(profile: UserProfile): String {
        return buildString {
            append(bold(green("\n=== User Profile ===\n")))
            append("  Name: ${profile.name}\n")
            append("  Role: ${profile.role}\n")
            append("  Context: ${profile.context}\n")
            append("\n")
            append(bold("Preferences:\n"))
            append("  Language: ${profile.preferences.preferredLanguage}\n")
            append("  Theme: ${profile.preferences.theme}\n")
            append("  Verbosity: ${profile.preferences.responseVerbosity}\n")
            append("\n")
            append(bold("Statistics:\n"))
            append("  Total Messages: ${profile.interactionStats.totalMessages}\n")
            append("  Tasks Completed: ${profile.interactionStats.totalTasksCompleted}\n")
            append("  Sessions: ${profile.interactionStats.totalSessions}\n")
        }
    }

    /**
     * Format task state for display.
     */
    fun formatTask(task: TaskState): String {
        return buildString {
            append(bold(blue("\n=== Task: ${task.taskName} ===\n")))
            append("  ID: ${task.taskId}\n")
            append("  Stage: ${formatStage(task.taskStage)}\n")
            append("  Progress: ${formatProgressBar(task.planProgressPercentage())}\n")
            append("  Status: ${if (task.isPaused) yellow("Paused") else green("Active")}\n")

            if (task.plan != null) {
                append("\n")
                append(bold("Plan:\n"))
                append(task.plan)
                append("\n")
            }

            if (task.planSteps.isNotEmpty()) {
                append("\n")
                append(bold("Steps:\n"))
                task.planSteps.forEach { step ->
                    val check = if (step.isCompleted) green("[x]") else gray("[ ]")
                    append("  $check ${step.number}. ${step.description}\n")
                }
            }
        }
    }

    /**
     * Format memory context for display.
     */
    fun formatMemory(context: MemoryContext): String {
        return buildString {
            append(bold(magenta("\n=== Memory Context ===\n")))

            // Working Memory
            if (context.workingMemory != null) {
                append(bold("Working Memory:\n"))
                append("  Execution State: ${context.workingMemory.executionState}\n")
                append("  Active Task: ${context.workingMemory.taskInfo?.description ?: "None"}\n")
                if (context.workingMemory.taskInfo != null) {
                    append("  Task Status: ${context.workingMemory.taskInfo.status}\n")
                    append("  Task Progress: ${(context.workingMemory.taskInfo.progress * 100).toInt()}%\n")
                }
                append("\n")
            }

            // Short-term Memory
            if (context.shortTermMemory != null) {
                append(bold("Recent Context (${context.shortTermMemory.messages.size} items):\n"))
                context.shortTermMemory.messages.takeLast(3).forEach { message ->
                    val preview = message.content.take(50)
                    val suffix = if (message.content.length > 50) "..." else ""
                    append("  - [$${message.senderType}]: $preview$suffix\n")
                }
                append("\n")
            }

            // Long-term Memory
            if (context.userProfile != null) {
                append(bold("User Profile: ${context.userProfile.name}\n"))
            }
        }
    }

    /**
     * Format task stage with color coding.
     */
    private fun formatStage(stage: TaskStage): String {
        return when (stage) {
            TaskStage.PLANNING -> yellow(stage.name)
            TaskStage.EXECUTION -> blue(stage.name)
            TaskStage.VALIDATION -> magenta(stage.name)
            TaskStage.DONE -> green(stage.name)
        }
    }

    /**
     * Format progress bar.
     */
    private fun formatProgressBar(percentage: Int): String {
        val filled = (percentage / 10)
        val empty = 10 - filled
        val bar = progressChars.filled.repeat(filled) + progressChars.empty.repeat(empty)
        return "$bar $percentage%"
    }

    /**
     * Format enhanced progress bar with step info and sub-progress.
     *
     * @param state Progress state
     * @param showSubProgress Whether to show sub-progress
     */
    fun formatProgressBar(
        state: ProgressState.InProgress,
        showSubProgress: Boolean = true
    ): String {
        val percentage = state.getClampedPercentage()
        val stepInfo = state.getStepInfo()

        // Build progress bar
        val barWidth = 20
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
     * Format real-time progress display.
     *
     * @param message Progress message
     * @param percentage Completion percentage
     * @param currentStep Current step number
     * @param totalSteps Total number of steps
     */
    fun formatRealTimeProgress(
        message: String,
        percentage: Int,
        currentStep: Int = 0,
        totalSteps: Int = 0
    ): String {
        val state = ProgressState.InProgress(
            message = message,
            currentStep = currentStep,
            totalSteps = totalSteps,
            percentage = percentage
        )
        return formatProgressBar(state, showSubProgress = false)
    }

    /**
     * Format interrupted progress with save indicator.
     *
     * @param message Interruption message
     * @param saved Whether state was saved
     * @param canResume Whether operation can be resumed
     */
    fun formatProgressInterrupted(
        message: String = "Operation interrupted",
        saved: Boolean = false,
        canResume: Boolean = false
    ): String {
        val savedText = if (saved) {
            green(" [State saved]")
        } else {
            gray(" [State not saved]")
        }

        return buildString {
            append(yellow("${statusIcons.warning} $message"))
            append(savedText)

            if (canResume) {
                append("\n")
                append(gray("  Tip: You can resume this operation"))
            }
        }
    }

    /**
     * Format timestamp to readable string.
     */
    private fun formatTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60_000 -> "just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            else -> "${diff / 86400_000}d ago"
        }
    }

    /**
     * Format plan approval prompt for CLI.
     */
    fun formatPlanApproval(task: TaskState): String {
        return buildString {
            append(bold(yellow("\n=== Plan Ready ===\n")))
            append("Task: ${task.taskName}\n\n")

            if (task.planSteps.isNotEmpty()) {
                append(bold("Steps:\n"))
                task.planSteps.forEach { step ->
                    append("  ${step.number}. ${step.description}\n")
                }
            } else if (task.plan != null) {
                append(bold("Plan:\n"))
                append(task.plan)
                append("\n")
            }

            append("\n")
            append(gray("Type 'approve' or 'ok' to start execution, or provide feedback to modify.\n"))
        }
    }

    /**
     * Format result approval prompt for CLI.
     */
    fun formatResultApproval(task: TaskState): String {
        return buildString {
            append(bold(magenta("\n=== Result Ready ===\n")))
            append("Task: ${task.taskName}\n")

            if (!task.validationResult.isNullOrEmpty()) {
                append("\n")
                append(bold("Validation:\n"))
                append(task.validationResult.take(300))
                if (task.validationResult.length > 300) append("...")
                append("\n")
            }

            if (!task.executionResult.isNullOrEmpty()) {
                append("\n")
                append(bold("Result Preview:\n"))
                append(task.executionResult.take(300))
                if (task.executionResult.length > 300) append("...")
                append("\n")
            }

            append("\n")
            append(gray("Type 'approve' or 'ok' to complete, or provide feedback to retry.\n"))
        }
    }

    /**
     * Format task progress for CLI status display.
     */
    fun formatTaskProgress(task: TaskState): String {
        return buildString {
            append(bold(blue("Task: ${task.taskName} ")))
            append(gray("[${task.taskStage.name}]"))
            append("\n")

            append("Progress: ${formatProgressBar(task.planProgressPercentage())}\n")

            if (task.planSteps.isNotEmpty()) {
                append("Steps:\n")
                task.planSteps.forEach { step ->
                    val check = if (step.isCompleted) green("✓") else gray("○")
                    append("  $check ${step.number}. ${step.description}\n")
                }
            }

            if (task.waitingForUserInput) {
                append("\n")
                append(yellow("⚠ ${task.expectedAction}\n"))
            }
        }
    }

    /**
     * Format task status bar for prompt.
     */
    fun formatTaskStatusBar(task: TaskState): String {
        val stageIcon = if (capabilities.unicode) {
            when (task.taskStage) {
                TaskStage.PLANNING -> "📋"
                TaskStage.EXECUTION -> "⚡"
                TaskStage.VALIDATION -> "✓"
                TaskStage.DONE -> "✅"
            }
        } else {
            when (task.taskStage) {
                TaskStage.PLANNING -> "[PLAN]"
                TaskStage.EXECUTION -> "[EXEC]"
                TaskStage.VALIDATION -> "[VAL]"
                TaskStage.DONE -> "[DONE]"
            }
        }
        val progress = task.planProgressPercentage()
        val waiting = if (task.waitingForUserInput) {
            if (capabilities.unicode) " ⏳" else " [WAIT]"
        } else {
            ""
        }

        return "$stageIcon ${task.taskName.take(20)}${if (task.taskName.length > 20) "..." else ""} [$progress%]$waiting"
    }

    /**
     * Format enhanced prompt with stage icon, progress, and warnings.
     *
     * @param task Current task
     * @param hasWarnings Whether there are warnings
     */
    fun formatEnhancedPrompt(task: TaskState?, hasWarnings: Boolean = false): String {
        if (task == null) {
            return "${cyan("agent")}${gray(">")} "
        }

        return when {
            task.waitingForUserInput -> {
                val stageColor = when (task.taskStage) {
                    TaskStage.PLANNING -> yellow("plan")
                    TaskStage.VALIDATION -> magenta("approve")
                    else -> cyan("task")
                }
                "${bold(stageColor)}${gray(">")} "
            }
            !task.isCompleted() -> {
                val warningIndicator = if (hasWarnings) {
                    yellow(" ⚠")
                } else {
                    ""
                }
                val progress = task.planProgressPercentage()
                "${bold(blue("task"))}${gray("[$progress%$warningIndicator]")}${gray(">")} "
            }
            else -> "${cyan("agent")}${gray(">")} "
        }
    }

    /**
     * Format validation warnings for display.
     */
    fun formatWarnings(warnings: List<ru.agent.features.invariant.domain.service.ValidationWarning>): String {
        return UserMessageFormatter.formatValidationWarnings(warnings)
    }
}
