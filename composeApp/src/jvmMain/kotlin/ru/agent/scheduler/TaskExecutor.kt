package ru.agent.scheduler

import co.touchlab.kermit.Logger
import kotlinx.serialization.json.Json
import ru.agent.features.scheduler.domain.model.TaskData
import ru.agent.features.scheduler.domain.model.TaskExecution
import ru.agent.features.scheduler.domain.model.ExecutionStatus
import ru.agent.features.scheduler.domain.repository.TaskExecutionRepository
import ru.agent.mcp.McpManager
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.util.UUID

/**
 * Executes scheduled tasks of different types.
 *
 * Supported task types:
 * - Reminder: Displays reminder message (logged)
 * - ShellCommand: Executes shell commands via McpManager
 * - McpTool: Executes MCP tool calls via McpManager
 */
class TaskExecutor(
    private val executionRepository: TaskExecutionRepository,
    private val mcpManager: McpManager? = null
) {
    private val logger = Logger.withTag("TaskExecutor")
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Execute a task and record the result.
     * @param taskId Unique task identifier
     * @param taskData Task-specific data
     * @param timeoutMs Timeout in milliseconds (default 60 seconds)
     * @return TaskExecution with result
     */
    suspend fun execute(
        taskId: String,
        taskData: TaskData,
        timeoutMs: Long = 60000
    ): TaskExecution {
        val executionId = UUID.randomUUID().toString()
        val startedAt = System.currentTimeMillis()

        // Create initial execution record
        var execution = TaskExecution(
            id = executionId,
            taskId = taskId,
            startedAt = startedAt,
            completedAt = null,
            status = ExecutionStatus.RUNNING,
            result = null,
            error = null
        )

        // Save initial state
        execution = executionRepository.create(execution)

        logger.i { "Starting execution $executionId for task $taskId" }

        return try {
            // Execute based on task type
            val (status, result, error) = when (taskData) {
                is TaskData.Reminder -> executeReminder(taskData)
                is TaskData.ShellCommand -> executeShellCommand(taskData, timeoutMs)
                is TaskData.McpTool -> executeMcpTool(taskData, timeoutMs)
            }

            val completedAt = System.currentTimeMillis()

            // Update execution with result
            val finalExecution = execution.copy(
                completedAt = completedAt,
                status = status,
                result = result,
                error = error
            )

            executionRepository.update(finalExecution)

            logger.i { "Execution $executionId completed with status: $status" }

            finalExecution
        } catch (e: Exception) {
            logger.e(e) { "Execution $executionId failed with exception" }

            val completedAt = System.currentTimeMillis()
            val failedExecution = execution.copy(
                completedAt = completedAt,
                status = ExecutionStatus.FAILED,
                error = e.message ?: "Unknown error: ${e::class.simpleName}"
            )

            executionRepository.update(failedExecution)
            failedExecution
        }
    }

    /**
     * Execute reminder task.
     * Simply logs the reminder message.
     */
    private suspend fun executeReminder(data: TaskData.Reminder): Triple<ExecutionStatus, String?, String?> {
        val priorityEmoji = when (data.priority.lowercase()) {
            "high" -> "[!]"
            "low" -> "[-]"
            else -> "[*]" // medium
        }

        val message = "$priorityEmoji REMINDER: ${data.message}"

        logger.i { message }
        println("\n$message\n")

        return Triple(
            ExecutionStatus.SUCCESS,
            message,
            null
        )
    }

    /**
     * Execute shell command task.
     * Uses McpManager to execute the command safely.
     */
    private suspend fun executeShellCommand(
        data: TaskData.ShellCommand,
        timeoutMs: Long
    ): Triple<ExecutionStatus, String?, String?> {
        val manager = mcpManager
            ?: return Triple(
                ExecutionStatus.FAILED,
                null,
                "McpManager not available for shell command execution"
            )

        logger.d { "Executing shell command: ${data.command}" }

        val result = manager.executeCommand(
            command = data.command,
            timeout = data.timeoutMs.coerceAtMost(timeoutMs),
            workingDir = data.workingDir
        )

        return result.fold(
            onSuccess = { output ->
                logger.d { "Shell command output: ${output.take(200)}..." }
                Triple(
                    ExecutionStatus.SUCCESS,
                    output,
                    null
                )
            },
            onFailure = { error ->
                logger.e { "Shell command failed: ${error.message}" }
                Triple(
                    ExecutionStatus.FAILED,
                    null,
                    error.message ?: "Shell command execution failed"
                )
            }
        )
    }

    /**
     * Execute MCP tool task.
     * Uses McpManager to call the specified tool.
     */
    private suspend fun executeMcpTool(
        data: TaskData.McpTool,
        timeoutMs: Long
    ): Triple<ExecutionStatus, String?, String?> {
        val manager = mcpManager
            ?: return Triple(
                ExecutionStatus.FAILED,
                null,
                "McpManager not available for MCP tool execution"
            )

        logger.d { "Executing MCP tool: ${data.serverName}:${data.toolName}" }

        // Parse arguments from JSON string
        val arguments = try {
            if (data.arguments.isBlank()) {
                emptyMap()
            } else {
                @Suppress("UNCHECKED_CAST")
                json.decodeFromString<Map<String, Any>>(data.arguments)
            }
        } catch (e: Exception) {
            return Triple(
                ExecutionStatus.FAILED,
                null,
                "Failed to parse tool arguments: ${e.message}"
            )
        }

        // Build full tool name
        val fullToolName = "${data.serverName}:${data.toolName}"

        val result = manager.executeToolByFullName(fullToolName, arguments)

        return result.fold(
            onSuccess = { output ->
                logger.d { "MCP tool result: ${output.take(200)}..." }
                Triple(
                    ExecutionStatus.SUCCESS,
                    output,
                    null
                )
            },
            onFailure = { error ->
                logger.e { "MCP tool failed: ${error.message}" }
                Triple(
                    ExecutionStatus.FAILED,
                    null,
                    error.message ?: "MCP tool execution failed"
                )
            }
        )
    }
}
