package ru.agent.cli.commands.scheduler

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import ru.agent.features.scheduler.domain.model.TaskStatus
import ru.agent.features.scheduler.domain.model.TaskType
import ru.agent.features.scheduler.domain.repository.ScheduledTaskRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Scheduler list subcommand.
 *
 * Shows list of all scheduled tasks with filtering and formatting options.
 *
 * Examples:
 *   agent scheduler list
 *   agent scheduler list --type reminder
 *   agent scheduler list --status pending
 *   agent scheduler list --json
 */
class SchedulerListCommand : CliktCommand(
    name = "list",
    help = """
        Show all scheduled tasks with optional filtering

        Examples:
          agent scheduler list
          agent scheduler list --type reminder
          agent scheduler list --status pending
          agent scheduler list --json
          agent scheduler list -t shell -s paused
    """.trimIndent()
) {
    private val json by option("--json", help = "Output as JSON")
        .flag(default = false)

    private val type by option("--type", "-t", help = "Filter by type (reminder, shell, mcp)")
        .convert {
            when (it.lowercase()) {
                "reminder" -> TaskType.REMINDER
                "shell" -> TaskType.SHELL_COMMAND
                "mcp" -> TaskType.MCP_TOOL
                else -> throw IllegalArgumentException("Invalid type: $it. Valid: reminder, shell, mcp")
            }
        }

    private val status by option("--status", "-s", help = "Filter by status (pending, paused, cancelled)")
        .convert {
            when (it.lowercase()) {
                "pending" -> TaskStatus.PENDING
                "paused" -> TaskStatus.PAUSED
                "cancelled" -> TaskStatus.CANCELLED
                "running" -> TaskStatus.RUNNING
                else -> throw IllegalArgumentException("Invalid status: $it. Valid: pending, paused, cancelled, running")
            }
        }

    private val terminal = Terminal()

    override fun run() {
        val repository: ScheduledTaskRepository by lazy {
            org.koin.java.KoinJavaComponent.getKoin().get()
        }

        runBlocking {
            try {
                val allTasks = repository.getAll()

                // Apply filters
                val filteredTasks = allTasks
                    .filter { task ->
                        (type == null || task.taskType == type) &&
                        (status == null || task.status == status)
                    }
                    .sortedByDescending { it.createdAt }

                if (filteredTasks.isEmpty()) {
                    terminal.println(yellow("No scheduled tasks found"))
                    return@runBlocking
                }

                // Output
                when {
                    json -> outputJson(filteredTasks)
                    else -> outputTable(filteredTasks)
                }

                // Statistics
                terminal.println()
                terminal.println(gray("Total: ${allTasks.size}, Filtered: ${filteredTasks.size}"))

            } catch (e: Exception) {
                terminal.println(red("Error: ${e.message}"))
            }
        }
    }

    private fun outputJson(tasks: List<ru.agent.features.scheduler.domain.model.ScheduledTask>) {
        terminal.println("[")
        tasks.forEachIndexed { index, task ->
            terminal.println("  {")
            terminal.println("    \"id\": \"${escapeJson(task.id)}\",")
            terminal.println("    \"name\": \"${escapeJson(task.name)}\",")
            terminal.println("    \"type\": \"${task.taskType.name}\",")
            terminal.println("    \"status\": \"${task.status.name}\",")
            terminal.println("    \"cron\": \"${escapeJson(task.cronExpression)}\",")
            terminal.println("    \"nextRun\": ${task.nextRunAt},")
            terminal.println("    \"lastRun\": ${task.lastRunAt}")
            terminal.println(if (index < tasks.size - 1) "  }," else "  }")
        }
        terminal.println("]")
    }

    private fun outputTable(tasks: List<ru.agent.features.scheduler.domain.model.ScheduledTask>) {
        terminal.println()
        terminal.println(brightBlue("=== SCHEDULED TASKS ==="))
        terminal.println()

        // Table header
        terminal.println(
            String.format(
                "%-18s %-15s %-10s %-10s %-20s %s",
                "ID",
                "NAME",
                "TYPE",
                "STATUS",
                "NEXT RUN",
                "CRON"
            )
        )
        terminal.println(gray("-".repeat(100)))

        // Table rows
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())

        tasks.forEach { task ->
            val statusText = when (task.status) {
                TaskStatus.PENDING -> green("PENDING")
                TaskStatus.RUNNING -> yellow("RUNNING")
                TaskStatus.PAUSED -> blue("PAUSED")
                TaskStatus.CANCELLED -> red("CANCELLED")
            }

            val typeText = when (task.taskType) {
                TaskType.REMINDER -> cyan("REMINDER")
                TaskType.SHELL_COMMAND -> magenta("SHELL")
                TaskType.MCP_TOOL -> white("MCP")
            }

            val nextRunText = task.nextRunAt?.let {
                formatter.format(Instant.ofEpochMilli(it))
            } ?: gray("N/A")

            terminal.println(
                String.format(
                    "%-18s %-15s %-10s %-10s %-20s %s",
                    task.id.take(18),
                    task.name.take(15),
                    typeText,
                    statusText,
                    nextRunText,
                    task.cronExpression
                )
            )
        }
    }

    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
