package ru.agent.cli.commands.scheduler

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import ru.agent.features.scheduler.domain.model.TaskStatus
import ru.agent.features.scheduler.domain.repository.ScheduledTaskRepository

/**
 * Scheduler cancel subcommand.
 *
 * Cancels a scheduled task (cannot be resumed).
 *
 * Examples:
 *   agent scheduler cancel task_abc123
 *   agent scheduler cancel task_abc123 --force
 */
class SchedulerCancelCommand : CliktCommand(
    name = "cancel",
    help = """
        Cancel a scheduled task permanently

        Once cancelled, a task cannot be resumed. Use 'pause' if you want to
        temporarily stop a task.

        Examples:
          agent scheduler cancel task_abc123
          agent scheduler cancel task_abc123 --force
    """.trimIndent()
) {
    private val taskId by argument(
        name = "task-id",
        help = "Task ID to cancel"
    )

    private val force by option("--force", "-f", help = "Skip confirmation")
        .flag(default = false)

    private val terminal = Terminal()

    override fun run() {
        val repository: ScheduledTaskRepository by lazy {
            org.koin.java.KoinJavaComponent.getKoin().get()
        }

        runBlocking {
            try {
                val task = repository.getById(taskId)

                if (task == null) {
                    terminal.println(red("Task not found: $taskId"))
                    return@runBlocking
                }

                if (task.status == TaskStatus.CANCELLED) {
                    terminal.println(yellow("Task is already cancelled: $taskId"))
                    return@runBlocking
                }

                // Confirmation
                if (!force) {
                    terminal.println()
                    terminal.println(yellow("About to cancel task:"))
                    terminal.println("  ${cyan("ID:")} ${task.id}")
                    terminal.println("  ${cyan("Name:")} ${task.name}")
                    terminal.println("  ${cyan("Type:")} ${task.taskType.name}")
                    terminal.println()
                    print(yellow("Are you sure? [y/N]: "))

                    val confirmation = readLine()?.trim()?.lowercase()
                    if (confirmation != "y" && confirmation != "yes") {
                        terminal.println(gray("Cancelled."))
                        return@runBlocking
                    }
                }

                // Cancel task
                val now = System.currentTimeMillis()
                repository.updateStatus(taskId, TaskStatus.CANCELLED, now)

                terminal.println()
                terminal.println(green("Task cancelled successfully"))
                terminal.println("  ${cyan("ID:")} $taskId")
                terminal.println(gray("The task will no longer execute"))

            } catch (e: Exception) {
                terminal.println()
                terminal.println(red("Error: ${e.message}"))
            }
        }
    }

    private fun print(message: String) {
        terminal.print(message)
    }
}
