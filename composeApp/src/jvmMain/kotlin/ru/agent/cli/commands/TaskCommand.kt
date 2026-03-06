package ru.agent.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import ru.agent.cli.formatters.OutputFormatter
import ru.agent.features.task.domain.usecase.*

/**
 * Task command group.
 *
 * Usage:
 * ```
 * agent task list
 * agent task status <task-id>
 * agent task create "Task name"
 * agent task cancel <task-id>
 * ```
 */
class TaskCommand : CliktCommand(
    name = "task",
    help = "Manage tasks"
) {
    init {
        subcommands(
            TaskListCommand(),
            TaskStatusCommand(),
            TaskCreateCommand(),
            TaskCancelCommand(),
            TaskPauseCommand(),
            TaskResumeCommand()
        )
    }

    override fun run() {
        if (currentContext.invokedSubcommand == null) {
            echo("Please specify a subcommand: list, status, create, cancel, pause, resume")
        }
    }
}

/**
 * Task list subcommand.
 */
class TaskListCommand : CliktCommand(
    name = "list",
    help = "List all tasks"
) {
    private val terminal = Terminal()

    override fun run() {
        terminal.println("Task listing not implemented yet.")
        terminal.println("Tasks are stored in database and can be queried via /task status <id>")
    }
}

/**
 * Task status subcommand.
 */
class TaskStatusCommand : CliktCommand(
    name = "status",
    help = "Show task status"
) {
    private val taskId by argument(help = "Task ID")

    private val terminal = Terminal()

    override fun run() {
        val getTaskStateUseCase: GetTaskStateUseCase by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }

        runBlocking {
            withTimeout(5000) {
                val task = getTaskStateUseCase(taskId)

                if (task != null) {
                    terminal.println(OutputFormatter.formatTask(task))
                } else {
                    terminal.println("Task not found: $taskId")
                }
            }
        }
    }
}

/**
 * Task create subcommand.
 */
class TaskCreateCommand : CliktCommand(
    name = "create",
    help = "Create a new task"
) {
    private val taskName by argument(help = "Task name")

    private val terminal = Terminal()

    override fun run() {
        val createTaskUseCase: CreateTaskUseCase by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }

        runBlocking {
            withTimeout(5000) {
                val task = createTaskUseCase(
                    sessionId = "default",
                    taskName = taskName,
                    taskDescription = null
                )
                terminal.println("Task created: ${task.taskId}")
                terminal.println("Use 'agent task status ${task.taskId}' to check progress.")
            }
        }
    }
}

/**
 * Task cancel subcommand.
 */
class TaskCancelCommand : CliktCommand(
    name = "cancel",
    help = "Cancel a task"
) {
    private val taskId by argument(help = "Task ID")

    private val terminal = Terminal()

    override fun run() {
        val cancelTaskUseCase: CancelTaskUseCase by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }

        runBlocking {
            withTimeout(5000) {
                cancelTaskUseCase(taskId)
                terminal.println("Task cancelled: $taskId")
            }
        }
    }
}

/**
 * Task pause subcommand.
 */
class TaskPauseCommand : CliktCommand(
    name = "pause",
    help = "Pause a task"
) {
    private val taskId by argument(help = "Task ID")

    private val terminal = Terminal()

    override fun run() {
        val pauseTaskUseCase: PauseTaskUseCase by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }

        runBlocking {
            withTimeout(5000) {
                pauseTaskUseCase(taskId)
                terminal.println("Task paused: $taskId")
            }
        }
    }
}

/**
 * Task resume subcommand.
 */
class TaskResumeCommand : CliktCommand(
    name = "resume",
    help = "Resume a paused task"
) {
    private val taskId by argument(help = "Task ID")

    private val terminal = Terminal()

    override fun run() {
        val resumeTaskUseCase: ResumeTaskUseCase by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }

        runBlocking {
            withTimeout(5000) {
                resumeTaskUseCase(taskId)
                terminal.println("Task resumed: $taskId")
            }
        }
    }
}
