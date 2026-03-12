package ru.agent.cli.commands.scheduler

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import ru.agent.features.scheduler.domain.model.TaskData
import ru.agent.features.scheduler.domain.model.TaskStatus
import ru.agent.features.scheduler.domain.model.TaskType
import ru.agent.features.scheduler.domain.repository.ScheduledTaskRepository
import java.util.*

/**
 * Scheduler create subcommand.
 *
 * Creates a new scheduled task interactively or via options.
 *
 * Examples:
 *   agent scheduler create
 *   agent scheduler create --name "Daily standup" --type reminder --cron "0 9 * * 1-5"
 *   agent scheduler create --name "Backup" --type shell --cron "0 2 * * *"
 */
class SchedulerCreateCommand : CliktCommand(
    name = "create",
    help = """
        Create a new scheduled task

        Can be used interactively (without options) or with command-line options.

        Task Types:
          - reminder: Simple reminders with messages
          - shell: Execute terminal commands
          - mcp: Call MCP server tools

        Cron Examples:
          "0 9 * * *"      - Every day at 9:00 AM
          "0 9 * * 1-5"    - Weekdays at 9:00 AM
          "*/15 * * * *"   - Every 15 minutes
          "0 0 1 * *"      - First day of month at midnight

        Examples:
          agent scheduler create
          agent scheduler create --name "Daily standup" --type reminder --cron "0 9 * * 1-5"
          agent scheduler create --name "Backup" --type shell --cron "0 2 * * *"
    """.trimIndent()
) {
    private val name by option("--name", "-n", help = "Task name")

    private val type by option("--type", "-t", help = "Task type (reminder, shell, mcp)")
        .convert {
            when (it.lowercase()) {
                "reminder" -> TaskType.REMINDER
                "shell" -> TaskType.SHELL_COMMAND
                "mcp" -> TaskType.MCP_TOOL
                else -> throw IllegalArgumentException("Invalid type: $it. Valid: reminder, shell, mcp")
            }
        }

    private val cron by option("--cron", "-c", help = "Cron expression for scheduling")

    private val description by option("--description", "-d", help = "Task description")

    private val tags by option("--tags", help = "Comma-separated list of tags")
        .split(",")

    private val terminal = Terminal()

    override fun run() {
        val repository: ScheduledTaskRepository by lazy {
            org.koin.java.KoinJavaComponent.getKoin().get()
        }

        runBlocking {
            try {
                // Check if interactive mode or CLI mode
                val isInteractive = name == null || type == null || cron == null

                val (taskName, taskType, taskCron, taskDescription, taskTags) = if (isInteractive) {
                    runInteractiveWizard()
                } else {
                    Tuple5(name!!, type!!, cron!!, description, tags ?: emptyList())
                }

                // Get task-specific data
                val taskData = collectTaskData(taskType, isInteractive)

                // Create task
                terminal.println()
                terminal.println(yellow("Creating scheduled task..."))

                val now = System.currentTimeMillis()
                val taskId = "task_${UUID.randomUUID().toString().take(8)}"

                // Calculate next run time (simplified - would normally use cron parser)
                val nextRun = calculateNextRun(taskCron)

                val task = ru.agent.features.scheduler.domain.model.ScheduledTask(
                    id = taskId,
                    name = taskName,
                    description = taskDescription,
                    cronExpression = taskCron,
                    taskType = taskType,
                    taskData = taskData,
                    status = TaskStatus.PENDING,
                    nextRunAt = nextRun,
                    lastRunAt = null,
                    createdAt = now,
                    updatedAt = now,
                    tags = taskTags
                )

                val created = repository.create(task)

                terminal.println()
                terminal.println(green("Task created successfully!"))
                terminal.println("  ${cyan("ID:")} ${created.id}")
                terminal.println("  ${cyan("Name:")} ${created.name}")
                terminal.println("  ${cyan("Type:")} ${created.taskType.name}")
                terminal.println("  ${cyan("Cron:")} ${created.cronExpression}")
                terminal.println("  ${cyan("Status:")} ${created.status.name}")
                terminal.println()
                terminal.println(gray("Use 'agent scheduler show ${created.id}' for details"))
                terminal.println(gray("Use 'agent scheduler list' to see all tasks"))

            } catch (e: Exception) {
                terminal.println()
                terminal.println(red("Error: ${e.message}"))
            }
        }
    }

    private data class Tuple5<A, B, C, D, E>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E
    )

    private fun runInteractiveWizard(): Tuple5<String, TaskType, String, String?, List<String>> {
        terminal.println()
        terminal.println(brightBlue("=== CREATE SCHEDULED TASK ==="))
        terminal.println()

        // Task type
        terminal.println(cyan("Select task type:"))
        terminal.println("  1) Reminder - Simple reminder with message")
        terminal.println("  2) Shell - Execute terminal command")
        terminal.println("  3) MCP - Call MCP server tool")
        print(yellow("Choice [1-3]: "))
        val typeChoice = readLine()?.trim() ?: "1"
        val taskType = when (typeChoice) {
            "1" -> TaskType.REMINDER
            "2" -> TaskType.SHELL_COMMAND
            "3" -> TaskType.MCP_TOOL
            else -> {
                terminal.println(gray("Invalid choice, using REMINDER"))
                TaskType.REMINDER
            }
        }

        // Task name
        print(cyan("Task name: "))
        val taskName = readLine()?.trim() ?: "Unnamed Task"

        // Cron expression
        terminal.println()
        terminal.println(cyan("Cron expression examples:"))
        terminal.println(gray("  \"0 9 * * *\"      - Every day at 9:00 AM"))
        terminal.println(gray("  \"0 9 * * 1-5\"    - Weekdays at 9:00 AM"))
        terminal.println(gray("  \"*/15 * * * *\"   - Every 15 minutes"))
        terminal.println(gray("  \"0 0 1 * *\"      - First day of month at midnight"))
        print(cyan("Cron expression: "))
        val taskCron = readLine()?.trim() ?: "0 9 * * *"

        // Description
        print(cyan("Description (optional): "))
        val taskDescription = readLine()?.trim()?.takeIf { it.isNotBlank() }

        // Tags
        print(cyan("Tags (comma-separated, optional): "))
        val taskTags = readLine()?.trim()
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        return Tuple5(taskName, taskType, taskCron, taskDescription, taskTags)
    }

    private fun collectTaskData(taskType: TaskType, isInteractive: Boolean): TaskData {
        return when (taskType) {
            TaskType.REMINDER -> {
                val message = if (isInteractive) {
                    print(cyan("Reminder message: "))
                    readLine()?.trim() ?: "No message"
                } else {
                    "Reminder: $name"
                }

                val priority = if (isInteractive) {
                    print(cyan("Priority (low/medium/high) [medium]: "))
                    readLine()?.trim()?.takeIf { it.isNotBlank() } ?: "medium"
                } else {
                    "medium"
                }

                TaskData.Reminder(message = message, priority = priority)
            }

            TaskType.SHELL_COMMAND -> {
                val command = if (isInteractive) {
                    print(cyan("Command to execute: "))
                    readLine()?.trim() ?: "echo 'Hello'"
                } else {
                    throw IllegalArgumentException("--command option required for shell tasks")
                }

                val workingDir = if (isInteractive) {
                    print(cyan("Working directory (optional): "))
                    readLine()?.trim()?.takeIf { it.isNotBlank() }
                } else {
                    null
                }

                val timeout = if (isInteractive) {
                    print(cyan("Timeout in seconds [30]: "))
                    readLine()?.trim()?.toLongOrNull()?.times(1000) ?: 30000L
                } else {
                    30000L
                }

                TaskData.ShellCommand(
                    command = command,
                    workingDir = workingDir,
                    timeoutMs = timeout
                )
            }

            TaskType.MCP_TOOL -> {
                val serverName = if (isInteractive) {
                    print(cyan("MCP server name: "))
                    readLine()?.trim() ?: "default"
                } else {
                    throw IllegalArgumentException("--server option required for MCP tasks")
                }

                val toolName = if (isInteractive) {
                    print(cyan("Tool name: "))
                    readLine()?.trim() ?: "unknown"
                } else {
                    throw IllegalArgumentException("--tool option required for MCP tasks")
                }

                val arguments = if (isInteractive) {
                    print(cyan("Tool arguments (JSON) [{}]: "))
                    readLine()?.trim()?.takeIf { it.isNotBlank() } ?: "{}"
                } else {
                    "{}"
                }

                TaskData.McpTool(
                    serverName = serverName,
                    toolName = toolName,
                    arguments = arguments
                )
            }
        }
    }

    private fun calculateNextRun(cronExpression: String): Long {
        // Simplified implementation - in real code would use cron parser
        // For now, just return 1 minute from now
        return System.currentTimeMillis() + 60_000
    }

    private fun print(message: String) {
        terminal.print(message)
    }
}
