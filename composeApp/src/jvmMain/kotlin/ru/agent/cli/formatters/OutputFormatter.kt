package ru.agent.cli.formatters

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
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
 */
object OutputFormatter {

    /**
     * Format chat message for display.
     */
    fun formatMessage(message: Message): String {
        val sender = when (message.senderType) {
            ru.agent.features.chat.domain.model.SenderType.USER -> cyan("You")
            ru.agent.features.chat.domain.model.SenderType.ASSISTANT -> green("Agent")
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
        val bar = "█".repeat(filled) + "░".repeat(empty)
        return "$bar $percentage%"
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
}
