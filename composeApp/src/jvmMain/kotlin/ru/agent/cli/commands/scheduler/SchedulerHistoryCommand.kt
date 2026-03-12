package ru.agent.cli.commands.scheduler

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import ru.agent.features.scheduler.domain.model.ExecutionStatus
import ru.agent.features.scheduler.domain.repository.ScheduledTaskRepository
import ru.agent.features.scheduler.domain.repository.TaskExecutionRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Scheduler history subcommand.
 *
 * Shows execution history for a specific task.
 *
 * Examples:
 *   agent scheduler history task_abc123
 *   agent scheduler history task_abc123 --limit 20
 *   agent scheduler history task_abc123 --json
 */
class SchedulerHistoryCommand : CliktCommand(
    name = "history",
    help = """
        Show execution history for a scheduled task

        Examples:
          agent scheduler history task_abc123
          agent scheduler history task_abc123 --limit 20
          agent scheduler history task_abc123 --json
    """.trimIndent()
) {
    private val taskId by argument(
        name = "task-id",
        help = "Task ID to show history for"
    )

    private val limit by option("--limit", "-l", help = "Maximum number of executions to show")
        .int()
        .default(10)

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

                // Get executions
                val executions = executionRepository.getByTaskId(taskId)
                    .sortedByDescending { it.startedAt }
                    .take(limit)

                if (executions.isEmpty()) {
                    terminal.println()
                    terminal.println(yellow("No execution history found for task: $taskId"))
                    terminal.println(gray("Task: ${task.name}"))
                    return@runBlocking
                }

                // Output
                when {
                    json -> outputJson(task, executions)
                    else -> outputTable(task, executions)
                }

                // Statistics
                val successCount = executions.count { it.status == ExecutionStatus.SUCCESS }
                val failedCount = executions.count { it.status == ExecutionStatus.FAILED }
                val timeoutCount = executions.count { it.status == ExecutionStatus.TIMEOUT }

                terminal.println()
                terminal.println(gray("Showing ${executions.size} of ${executionRepository.countByTaskId(taskId)} executions"))
                terminal.println(gray("Success: ${green(successCount.toString())}, Failed: ${red(failedCount.toString())}, Timeout: ${magenta(timeoutCount.toString())}"))

            } catch (e: Exception) {
                terminal.println()
                terminal.println(red("Error: ${e.message}"))
            }
        }
    }

    private fun outputJson(
        task: ru.agent.features.scheduler.domain.model.ScheduledTask,
        executions: List<ru.agent.features.scheduler.domain.model.TaskExecution>
    ) {
        terminal.println("{")
        terminal.println("  \"taskId\": \"${task.id}\",")
        terminal.println("  \"taskName\": \"${escapeJson(task.name)}\",")
        terminal.println("  \"executions\": [")

        executions.forEachIndexed { index, exec ->
            terminal.println("    {")
            terminal.println("      \"id\": \"${exec.id}\",")
            terminal.println("      \"status\": \"${exec.status.name}\",")
            terminal.println("      \"startedAt\": ${exec.startedAt},")
            terminal.println("      \"completedAt\": ${exec.completedAt},")
            terminal.println("      \"result\": ${exec.result?.let { "\"${escapeJson(it)}\"" } ?: "null"},")
            terminal.println("      \"error\": ${exec.error?.let { "\"${escapeJson(it)}\"" } ?: "null"}")
            terminal.println(if (index < executions.size - 1) "    }," else "    }")
        }

        terminal.println("  ]")
        terminal.println("}")
    }

    private fun outputTable(
        task: ru.agent.features.scheduler.domain.model.ScheduledTask,
        executions: List<ru.agent.features.scheduler.domain.model.TaskExecution>
    ) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())

        terminal.println()
        terminal.println(brightBlue("=== EXECUTION HISTORY ==="))
        terminal.println()
        terminal.println("${cyan("Task:")} ${task.name} (${task.id})")
        terminal.println()

        // Table header
        terminal.println(
            String.format(
                "%-18s %-10s %-20s %-20s %s",
                "ID",
                "STATUS",
                "STARTED",
                "COMPLETED",
                "RESULT"
            )
        )
        terminal.println(gray("-".repeat(100)))

        // Table rows
        executions.forEach { exec ->
            val statusText = when (exec.status) {
                ExecutionStatus.RUNNING -> yellow("RUNNING")
                ExecutionStatus.SUCCESS -> green("SUCCESS")
                ExecutionStatus.FAILED -> red("FAILED")
                ExecutionStatus.TIMEOUT -> magenta("TIMEOUT")
            }

            val startedText = formatter.format(Instant.ofEpochMilli(exec.startedAt))
            val completedText = exec.completedAt?.let {
                formatter.format(Instant.ofEpochMilli(it))
            } ?: gray("N/A")

            val resultText = when {
                exec.error != null -> red(exec.error.take(50))
                exec.result != null -> exec.result.take(50)
                else -> gray("N/A")
            }

            terminal.println(
                String.format(
                    "%-18s %-10s %-20s %-20s %s",
                    exec.id.take(18),
                    statusText,
                    startedText,
                    completedText,
                    resultText
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
