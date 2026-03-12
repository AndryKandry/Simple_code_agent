package ru.agent.cli.commands.scheduler

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import ru.agent.features.scheduler.domain.model.TaskData
import ru.agent.features.scheduler.domain.model.TaskStatus
import ru.agent.features.scheduler.domain.model.TaskType
import ru.agent.features.scheduler.domain.repository.ScheduledTaskRepository
import ru.agent.features.scheduler.domain.repository.TaskExecutionRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Scheduler show subcommand.
 *
 * Shows detailed information about a specific scheduled task.
 *
 * Examples:
 *   agent scheduler show task_abc123
 *   agent scheduler show task_abc123 --json
 */
class SchedulerShowCommand : CliktCommand(
    name = "show",
    help = """
        Show detailed information about a scheduled task

        Examples:
          agent scheduler show task_abc123
          agent scheduler show task_abc123 --json
    """.trimIndent()
) {
    private val taskId by argument(
        name = "task-id",
        help = "Task ID to show"
    )

    private val json by option("--json", help = "Output as JSON")
        .flag(default = false)

    private val terminal = Terminal()

    override fun run() {
        val taskRepository: ScheduledTaskRepository by lazy {
            org.koin.java.KoinJavaComponent.getKoin().get()
        }
        val executionRepository: TaskExecutionRepository by lazy {
            org.koin.java.KoinJavaComponent.getKoin().get()
        }

        runBlocking {
            try {
                val task = taskRepository.getById(taskId)

                if (task == null) {
                    terminal.println(red("Task not found: $taskId"))
                    return@runBlocking
                }

                // Get recent executions
                val executions = executionRepository.getByTaskId(taskId).take(5)

                // Output
                when {
                    json -> outputJson(task, executions)
                    else -> outputDetailed(task, executions)
                }

            } catch (e: Exception) {
                terminal.println(red("Error: ${e.message}"))
            }
        }
    }

    private fun outputJson(
        task: ru.agent.features.scheduler.domain.model.ScheduledTask,
        executions: List<ru.agent.features.scheduler.domain.model.TaskExecution>
    ) {
        terminal.println("{")
        terminal.println("  \"id\": \"${escapeJson(task.id)}\",")
        terminal.println("  \"name\": \"${escapeJson(task.name)}\",")
        terminal.println("  \"description\": ${task.description?.let { "\"${escapeJson(it)}\"" } ?: "null"},")
        terminal.println("  \"type\": \"${task.taskType.name}\",")
        terminal.println("  \"status\": \"${task.status.name}\",")
        terminal.println("  \"cron\": \"${escapeJson(task.cronExpression)}\",")
        terminal.println("  \"nextRun\": ${task.nextRunAt},")
        terminal.println("  \"lastRun\": ${task.lastRunAt},")
        terminal.println("  \"createdAt\": ${task.createdAt},")
        terminal.println("  \"tags\": ${task.tags.map { "\"$it\"" }},")

        // Task data
        terminal.println("  \"data\": {")
        when (val data = task.taskData) {
            is TaskData.Reminder -> {
                terminal.println("    \"message\": \"${escapeJson(data.message)}\",")
                terminal.println("    \"priority\": \"${data.priority}\"")
            }
            is TaskData.ShellCommand -> {
                terminal.println("    \"command\": \"${escapeJson(data.command)}\",")
                terminal.println("    \"workingDir\": ${data.workingDir?.let { "\"$it\"" } ?: "null"},")
                terminal.println("    \"timeoutMs\": ${data.timeoutMs}")
            }
            is TaskData.McpTool -> {
                terminal.println("    \"server\": \"${escapeJson(data.serverName)}\",")
                terminal.println("    \"tool\": \"${escapeJson(data.toolName)}\",")
                terminal.println("    \"arguments\": \"${escapeJson(data.arguments)}\"")
            }
        }
        terminal.println("  },")

        // Recent executions
        terminal.println("  \"recentExecutions\": [")
        executions.forEachIndexed { index, exec ->
            terminal.println("    {")
            terminal.println("      \"id\": \"${exec.id}\",")
            terminal.println("      \"status\": \"${exec.status.name}\",")
            terminal.println("      \"startedAt\": ${exec.startedAt},")
            terminal.println("      \"completedAt\": ${exec.completedAt}")
            terminal.println(if (index < executions.size - 1) "    }," else "    }")
        }
        terminal.println("  ]")
        terminal.println("}")
    }

    private fun outputDetailed(
        task: ru.agent.features.scheduler.domain.model.ScheduledTask,
        executions: List<ru.agent.features.scheduler.domain.model.TaskExecution>
    ) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())

        terminal.println()
        terminal.println(brightBlue("=== TASK DETAILS ==="))
        terminal.println()

        // Basic info
        terminal.println("${cyan("ID:")} ${task.id}")
        terminal.println("${cyan("Name:")} ${task.name}")
        task.description?.let {
            terminal.println("${cyan("Description:")} $it")
        }

        // Status
        val statusText = when (task.status) {
            TaskStatus.PENDING -> green("PENDING")
            TaskStatus.RUNNING -> yellow("RUNNING")
            TaskStatus.PAUSED -> blue("PAUSED")
            TaskStatus.CANCELLED -> red("CANCELLED")
        }
        terminal.println("${cyan("Status:")} $statusText")

        // Type
        val typeText = when (task.taskType) {
            TaskType.REMINDER -> "REMINDER"
            TaskType.SHELL_COMMAND -> "SHELL_COMMAND"
            TaskType.MCP_TOOL -> "MCP_TOOL"
        }
        terminal.println("${cyan("Type:")} $typeText")

        // Schedule
        terminal.println("${cyan("Cron:")} ${task.cronExpression}")
        task.nextRunAt?.let {
            terminal.println("${cyan("Next Run:")} ${formatter.format(Instant.ofEpochMilli(it))}")
        }
        task.lastRunAt?.let {
            terminal.println("${cyan("Last Run:")} ${formatter.format(Instant.ofEpochMilli(it))}")
        }

        // Task data
        terminal.println()
        terminal.println(brightBlue("=== TASK DATA ==="))
        when (val data = task.taskData) {
            is TaskData.Reminder -> {
                terminal.println("${cyan("Message:")} ${data.message}")
                terminal.println("${cyan("Priority:")} ${data.priority}")
            }
            is TaskData.ShellCommand -> {
                terminal.println("${cyan("Command:")} ${data.command}")
                data.workingDir?.let {
                    terminal.println("${cyan("Working Dir:")} $it")
                }
                terminal.println("${cyan("Timeout:")} ${data.timeoutMs}ms")
            }
            is TaskData.McpTool -> {
                terminal.println("${cyan("Server:")} ${data.serverName}")
                terminal.println("${cyan("Tool:")} ${data.toolName}")
                terminal.println("${cyan("Arguments:")} ${data.arguments}")
            }
        }

        // Tags
        if (task.tags.isNotEmpty()) {
            terminal.println()
            terminal.println("${cyan("Tags:")} ${task.tags.joinToString(", ")}")
        }

        // Timestamps
        terminal.println()
        terminal.println(gray("Created: ${formatter.format(Instant.ofEpochMilli(task.createdAt))}"))
        terminal.println(gray("Updated: ${formatter.format(Instant.ofEpochMilli(task.updatedAt))}"))

        // Recent executions
        if (executions.isNotEmpty()) {
            terminal.println()
            terminal.println(brightBlue("=== RECENT EXECUTIONS ==="))
            terminal.println()

            executions.forEach { exec ->
                val execStatus = when (exec.status) {
                    ru.agent.features.scheduler.domain.model.ExecutionStatus.RUNNING -> yellow("RUNNING")
                    ru.agent.features.scheduler.domain.model.ExecutionStatus.SUCCESS -> green("SUCCESS")
                    ru.agent.features.scheduler.domain.model.ExecutionStatus.FAILED -> red("FAILED")
                    ru.agent.features.scheduler.domain.model.ExecutionStatus.TIMEOUT -> magenta("TIMEOUT")
                }

                terminal.println("${exec.id.take(15)} $execStatus")
                terminal.println(gray("  Started: ${formatter.format(Instant.ofEpochMilli(exec.startedAt))}"))
                exec.completedAt?.let {
                    terminal.println(gray("  Completed: ${formatter.format(Instant.ofEpochMilli(it))}"))
                }
                exec.result?.let {
                    terminal.println(gray("  Result: ${it.take(100)}"))
                }
                exec.error?.let {
                    terminal.println(red("  Error: ${it.take(100)}"))
                }
                terminal.println()
            }
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
