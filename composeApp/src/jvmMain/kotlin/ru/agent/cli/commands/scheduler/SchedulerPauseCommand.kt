package ru.agent.cli.commands.scheduler

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import ru.agent.features.scheduler.domain.model.TaskStatus
import ru.agent.features.scheduler.domain.repository.ScheduledTaskRepository

/**
 * Scheduler pause subcommand.
 *
 * Pauses a scheduled task (can be resumed later).
 *
 * Examples:
 *   agent scheduler pause task_abc123
 */
class SchedulerPauseCommand : CliktCommand(
    name = "pause",
    help = """
        Pause a scheduled task temporarily

        The task will not execute while paused. Use 'resume' to continue execution.

        Examples:
          agent scheduler pause task_abc123
    """.trimIndent()
) {
    private val taskId by argument(
        name = "task-id",
        help = "Task ID to pause"
    )

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

                when (task.status) {
                    TaskStatus.PAUSED -> {
                        terminal.println(yellow("Task is already paused: $taskId"))
                        return@runBlocking
                    }
                    TaskStatus.CANCELLED -> {
                        terminal.println(red("Cannot pause a cancelled task: $taskId"))
                        return@runBlocking
                    }
                    TaskStatus.RUNNING -> {
                        terminal.println(yellow("Warning: Task is currently running"))
                        terminal.println(gray("It will be paused after current execution completes"))
                    }
                    else -> { /* PENDING - can pause */ }
                }

                // Pause task
                val now = System.currentTimeMillis()
                repository.updateStatus(taskId, TaskStatus.PAUSED, now)

                terminal.println()
                terminal.println(green("Task paused successfully"))
                terminal.println("  ${cyan("ID:")} $taskId")
                terminal.println("  ${cyan("Name:")} ${task.name}")
                terminal.println()
                terminal.println(gray("Use 'agent scheduler resume $taskId' to resume"))

            } catch (e: Exception) {
                terminal.println()
                terminal.println(red("Error: ${e.message}"))
            }
        }
    }
}
