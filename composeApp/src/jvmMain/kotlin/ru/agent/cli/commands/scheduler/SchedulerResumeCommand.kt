package ru.agent.cli.commands.scheduler

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import ru.agent.features.scheduler.domain.model.TaskStatus
import ru.agent.features.scheduler.domain.repository.ScheduledTaskRepository

/**
 * Scheduler resume subcommand.
 *
 * Resumes a paused scheduled task.
 *
 * Examples:
 *   agent scheduler resume task_abc123
 */
class SchedulerResumeCommand : CliktCommand(
    name = "resume",
    help = """
        Resume a paused scheduled task

        The task will continue executing according to its schedule.

        Examples:
          agent scheduler resume task_abc123
    """.trimIndent()
) {
    private val taskId by argument(
        name = "task-id",
        help = "Task ID to resume"
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
                    TaskStatus.PENDING -> {
                        terminal.println(yellow("Task is already active: $taskId"))
                        return@runBlocking
                    }
                    TaskStatus.CANCELLED -> {
                        terminal.println(red("Cannot resume a cancelled task: $taskId"))
                        terminal.println(gray("Cancelled tasks are permanently stopped"))
                        return@runBlocking
                    }
                    TaskStatus.RUNNING -> {
                        terminal.println(yellow("Task is currently running: $taskId"))
                        return@runBlocking
                    }
                    else -> { /* PAUSED - can resume */ }
                }

                // Resume task
                val now = System.currentTimeMillis()

                // Calculate next run time (simplified)
                val nextRun = now + 60_000 // 1 minute from now

                repository.update(task.copy(
                    status = TaskStatus.PENDING,
                    nextRunAt = nextRun,
                    updatedAt = now
                ))

                terminal.println()
                terminal.println(green("Task resumed successfully"))
                terminal.println("  ${cyan("ID:")} $taskId")
                terminal.println("  ${cyan("Name:")} ${task.name}")
                terminal.println("  ${cyan("Status:")} PENDING")
                terminal.println()
                terminal.println(gray("The task will execute according to schedule"))

            } catch (e: Exception) {
                terminal.println()
                terminal.println(red("Error: ${e.message}"))
            }
        }
    }
}
