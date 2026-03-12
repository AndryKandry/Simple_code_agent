package ru.agent.cli.commands.scheduler

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
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
 * Scheduler logs subcommand.
 *
 * Shows recent execution logs for all tasks.
 *
 * Examples:
 *   agent scheduler logs
 *   agent scheduler logs --limit 50
 *   agent scheduler logs --status failed
 *   agent scheduler logs --json
 */
class SchedulerLogsCommand : CliktCommand(
    name = "logs",
    help = """
        Show recent execution logs for all scheduled tasks

        Examples:
          agent scheduler logs
          agent scheduler logs --limit 50
          agent scheduler logs --status failed
          agent scheduler logs --status success --json
    """.trimIndent()
) {
    private val limit by option("--limit", "-l", help = "Maximum number of logs to show")
        .int()
        .default(20)

    private val status by option("--status", "-s", help = "Filter by status (success, failed, timeout)")
        .convert {
            when (it.lowercase()) {
                "success" -> ExecutionStatus.SUCCESS
                "failed" -> ExecutionStatus.FAILED
                "timeout" -> ExecutionStatus.TIMEOUT
                "running" -> ExecutionStatus.RUNNING
                else -> throw IllegalArgumentException("Invalid status: $it. Valid: success, failed, timeout, running")
            }
        }

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
                // Get recent executions
                val allExecutions = executionRepository.getRecent(100)

                // Apply filters
                val filteredExecutions = allExecutions
                    .filter { execution ->
                        status == null || execution.status == status
                    }
                    .sortedByDescending { it.startedAt }
                    .take(limit)

                if (filteredExecutions.isEmpty()) {
                    terminal.println()
                    terminal.println(yellow("No execution logs found"))
                    return@runBlocking
                }

                // Get task names map
                val tasks = taskRepository.getAll().associateBy { it.id }

                // Output
                when {
                    json -> outputJson(filteredExecutions, tasks)
                    else -> outputTable(filteredExecutions, tasks)
                }

                // Statistics
                terminal.println()
                terminal.println(gray("Showing ${filteredExecutions.size} recent executions"))
                if (status != null) {
                    terminal.println(gray("Filter: ${status!!.name}"))
                }

            } catch (e: Exception) {
                terminal.println()
                terminal.println(red("Error: ${e.message}"))
            }
        }
    }

    private fun outputJson(
        executions: List<ru.agent.features.scheduler.domain.model.TaskExecution>,
        tasks: Map<String, ru.agent.features.scheduler.domain.model.ScheduledTask>
    ) {
        terminal.println("[")

        executions.forEachIndexed { index, exec ->
            val task = tasks[exec.taskId]
            terminal.println("  {")
            terminal.println("    \"id\": \"${exec.id}\",")
            terminal.println("    \"taskId\": \"${exec.taskId}\",")
            terminal.println("    \"taskName\": ${task?.name?.let { "\"${escapeJson(it)}\"" } ?: "null"},")
            terminal.println("    \"status\": \"${exec.status.name}\",")
            terminal.println("    \"startedAt\": ${exec.startedAt},")
            terminal.println("    \"completedAt\": ${exec.completedAt},")
            terminal.println("    \"result\": ${exec.result?.let { "\"${escapeJson(it)}\"" } ?: "null"},")
            terminal.println("    \"error\": ${exec.error?.let { "\"${escapeJson(it)}\"" } ?: "null"}")
            terminal.println(if (index < executions.size - 1) "  }," else "  }")
        }

        terminal.println("]")
    }

    private fun outputTable(
        executions: List<ru.agent.features.scheduler.domain.model.TaskExecution>,
        tasks: Map<String, ru.agent.features.scheduler.domain.model.ScheduledTask>
    ) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())

        terminal.println()
        terminal.println(brightBlue("=== RECENT EXECUTION LOGS ==="))
        terminal.println()

        // Table header
        terminal.println(
            String.format(
                "%-18s %-15s %-10s %-20s %s",
                "EXEC ID",
                "TASK",
                "STATUS",
                "TIME",
                "RESULT"
            )
        )
        terminal.println(gray("-".repeat(100)))

        // Table rows
        executions.forEach { exec ->
            val task = tasks[exec.taskId]
            val taskName = task?.name?.take(15) ?: exec.taskId.take(15)

            val statusText = when (exec.status) {
                ExecutionStatus.RUNNING -> yellow("RUNNING")
                ExecutionStatus.SUCCESS -> green("SUCCESS")
                ExecutionStatus.FAILED -> red("FAILED")
                ExecutionStatus.TIMEOUT -> magenta("TIMEOUT")
            }

            val timeText = formatter.format(Instant.ofEpochMilli(exec.startedAt))

            val resultText = when {
                exec.error != null -> red(exec.error.take(40))
                exec.result != null -> exec.result.take(40)
                else -> gray("N/A")
            }

            terminal.println(
                String.format(
                    "%-18s %-15s %-10s %-20s %s",
                    exec.id.take(18),
                    taskName,
                    statusText,
                    timeText,
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
