package ru.agent.mcp.server

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import ru.agent.features.scheduler.domain.model.*
import ru.agent.features.scheduler.domain.repository.ScheduledTaskRepository
import ru.agent.features.scheduler.domain.repository.TaskExecutionRepository
import java.util.*

/**
 * MCP Server for scheduler operations.
 *
 * Provides tools for:
 * - schedule_reminder: Create a reminder task
 * - schedule_command: Create a shell command task
 * - schedule_mcp_tool: Create an MCP tool call task
 * - list_scheduled_tasks: List all scheduled tasks
 * - get_task: Get task information
 * - cancel_task: Cancel a task
 * - pause_task: Pause a task
 * - resume_task: Resume a paused task
 * - get_task_history: Get execution history for a task
 *
 * All tools return text results with detailed information.
 */
class SchedulerMcpServer(
    private val taskRepository: ScheduledTaskRepository,
    private val executionRepository: TaskExecutionRepository
) {
    private val server = Server(
        serverInfo = Implementation(
            name = "scheduler-mcp-server",
            version = "1.0.0"
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true),
            ),
        )
    )

    init {
        registerTools()
    }

    private fun registerTools() {
        // === Task Creation Tools ===

        // schedule_reminder
        server.addTool(
            name = "schedule_reminder",
            description = "Create a scheduled reminder with a message",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("message", buildJsonObject {
                        put("type", "string")
                        put("description", "The reminder message")
                    })
                    put("cron", buildJsonObject {
                        put("type", "string")
                        put("description", "Cron expression (e.g., '0 9 * * 1-5' for weekdays at 9 AM)")
                    })
                    put("name", buildJsonObject {
                        put("type", "string")
                        put("description", "Task name (optional)")
                    })
                    put("priority", buildJsonObject {
                        put("type", "string")
                        put("description", "Priority level: low, medium, high (default: medium)")
                        put("enum", JsonArray(listOf(JsonPrimitive("low"), JsonPrimitive("medium"), JsonPrimitive("high"))))
                    })
                    put("tags", buildJsonObject {
                        put("type", "array")
                        put("description", "List of tags for organization")
                        put("items", buildJsonObject { put("type", "string") })
                    })
                },
                required = listOf("message", "cron")
            )
        ) { request ->
            val message = request.arguments?.get("message")?.jsonPrimitive?.content ?: ""
            val cron = request.arguments?.get("cron")?.jsonPrimitive?.content ?: ""
            val name = request.arguments?.get("name")?.jsonPrimitive?.contentOrNull
            val priority = request.arguments?.get("priority")?.jsonPrimitive?.contentOrNull ?: "medium"
            val tags = request.arguments?.get("tags")?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()

            val result = createReminder(message, cron, name, priority, tags)
            CallToolResult(content = listOf(TextContent(text = result)))
        }

        // schedule_command
        server.addTool(
            name = "schedule_command",
            description = "Create a scheduled shell command task",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("command", buildJsonObject {
                        put("type", "string")
                        put("description", "The shell command to execute")
                    })
                    put("cron", buildJsonObject {
                        put("type", "string")
                        put("description", "Cron expression for scheduling")
                    })
                    put("name", buildJsonObject {
                        put("type", "string")
                        put("description", "Task name (optional)")
                    })
                    put("working_dir", buildJsonObject {
                        put("type", "string")
                        put("description", "Working directory for command execution (optional)")
                    })
                    put("timeout", buildJsonObject {
                        put("type", "number")
                        put("description", "Timeout in milliseconds (default: 30000)")
                    })
                    put("tags", buildJsonObject {
                        put("type", "array")
                        put("description", "List of tags for organization")
                        put("items", buildJsonObject { put("type", "string") })
                    })
                },
                required = listOf("command", "cron")
            )
        ) { request ->
            val command = request.arguments?.get("command")?.jsonPrimitive?.content ?: ""
            val cron = request.arguments?.get("cron")?.jsonPrimitive?.content ?: ""
            val name = request.arguments?.get("name")?.jsonPrimitive?.contentOrNull
            val workingDir = request.arguments?.get("working_dir")?.jsonPrimitive?.contentOrNull
            val timeout = request.arguments?.get("timeout")?.jsonPrimitive?.longOrNull ?: 30000L
            val tags = request.arguments?.get("tags")?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()

            val result = createCommand(command, cron, name, workingDir, timeout, tags)
            CallToolResult(content = listOf(TextContent(text = result)))
        }

        // schedule_mcp_tool
        server.addTool(
            name = "schedule_mcp_tool",
            description = "Create a scheduled MCP tool call task",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("server", buildJsonObject {
                        put("type", "string")
                        put("description", "MCP server name")
                    })
                    put("tool", buildJsonObject {
                        put("type", "string")
                        put("description", "Tool name to call")
                    })
                    put("cron", buildJsonObject {
                        put("type", "string")
                        put("description", "Cron expression for scheduling")
                    })
                    put("arguments", buildJsonObject {
                        put("type", "string")
                        put("description", "JSON string with tool arguments (default: {})")
                    })
                    put("name", buildJsonObject {
                        put("type", "string")
                        put("description", "Task name (optional)")
                    })
                    put("tags", buildJsonObject {
                        put("type", "array")
                        put("description", "List of tags for organization")
                        put("items", buildJsonObject { put("type", "string") })
                    })
                },
                required = listOf("server", "tool", "cron")
            )
        ) { request ->
            val serverName = request.arguments?.get("server")?.jsonPrimitive?.content ?: ""
            val toolName = request.arguments?.get("tool")?.jsonPrimitive?.content ?: ""
            val cron = request.arguments?.get("cron")?.jsonPrimitive?.content ?: ""
            val arguments = request.arguments?.get("arguments")?.jsonPrimitive?.contentOrNull ?: "{}"
            val name = request.arguments?.get("name")?.jsonPrimitive?.contentOrNull
            val tags = request.arguments?.get("tags")?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()

            val result = createMcpTool(serverName, toolName, cron, arguments, name, tags)
            CallToolResult(content = listOf(TextContent(text = result)))
        }

        // === Task Query Tools ===

        // list_scheduled_tasks
        server.addTool(
            name = "list_scheduled_tasks",
            description = "List all scheduled tasks with optional filters",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("type", buildJsonObject {
                        put("type", "string")
                        put("description", "Filter by task type: reminder, shell, mcp")
                        put("enum", JsonArray(listOf(JsonPrimitive("reminder"), JsonPrimitive("shell"), JsonPrimitive("mcp"))))
                    })
                    put("status", buildJsonObject {
                        put("type", "string")
                        put("description", "Filter by status: pending, paused, cancelled")
                        put("enum", JsonArray(listOf(JsonPrimitive("pending"), JsonPrimitive("paused"), JsonPrimitive("cancelled"))))
                    })
                },
                required = emptyList()
            )
        ) { request ->
            val typeFilter = request.arguments?.get("type")?.jsonPrimitive?.contentOrNull
            val statusFilter = request.arguments?.get("status")?.jsonPrimitive?.contentOrNull

            val result = runBlocking { listTasks(typeFilter, statusFilter) }
            CallToolResult(content = listOf(TextContent(text = result)))
        }

        // get_task
        server.addTool(
            name = "get_task",
            description = "Get detailed information about a specific task",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("task_id", buildJsonObject {
                        put("type", "string")
                        put("description", "The task ID")
                    })
                },
                required = listOf("task_id")
            )
        ) { request ->
            val taskId = request.arguments?.get("task_id")?.jsonPrimitive?.content ?: ""
            val result = runBlocking { getTask(taskId) }
            CallToolResult(content = listOf(TextContent(text = result)))
        }

        // === Task Management Tools ===

        // cancel_task
        server.addTool(
            name = "cancel_task",
            description = "Cancel a scheduled task permanently",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("task_id", buildJsonObject {
                        put("type", "string")
                        put("description", "The task ID to cancel")
                    })
                },
                required = listOf("task_id")
            )
        ) { request ->
            val taskId = request.arguments?.get("task_id")?.jsonPrimitive?.content ?: ""
            val result = runBlocking { cancelTask(taskId) }
            CallToolResult(content = listOf(TextContent(text = result)))
        }

        // delete_task
        server.addTool(
            name = "delete_task",
            description = "Delete a task permanently from the database",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("task_id", buildJsonObject {
                        put("type", "string")
                        put("description", "The task ID to delete")
                    })
                },
                required = listOf("task_id")
            )
        ) { request ->
            val taskId = request.arguments?.get("task_id")?.jsonPrimitive?.content ?: ""
            val result = runBlocking { deleteTask(taskId) }
            CallToolResult(content = listOf(TextContent(text = result)))
        }

        // pause_task
        server.addTool(
            name = "pause_task",
            description = "Pause a scheduled task (can be resumed later)",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("task_id", buildJsonObject {
                        put("type", "string")
                        put("description", "The task ID to pause")
                    })
                },
                required = listOf("task_id")
            )
        ) { request ->
            val taskId = request.arguments?.get("task_id")?.jsonPrimitive?.content ?: ""
            val result = runBlocking { pauseTask(taskId) }
            CallToolResult(content = listOf(TextContent(text = result)))
        }

        // resume_task
        server.addTool(
            name = "resume_task",
            description = "Resume a paused task",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("task_id", buildJsonObject {
                        put("type", "string")
                        put("description", "The task ID to resume")
                    })
                },
                required = listOf("task_id")
            )
        ) { request ->
            val taskId = request.arguments?.get("task_id")?.jsonPrimitive?.content ?: ""
            val result = runBlocking { resumeTask(taskId) }
            CallToolResult(content = listOf(TextContent(text = result)))
        }

        // get_task_history
        server.addTool(
            name = "get_task_history",
            description = "Get execution history for a specific task",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("task_id", buildJsonObject {
                        put("type", "string")
                        put("description", "The task ID")
                    })
                    put("limit", buildJsonObject {
                        put("type", "number")
                        put("description", "Maximum number of records to return (default: 10)")
                    })
                },
                required = listOf("task_id")
            )
        ) { request ->
            val taskId = request.arguments?.get("task_id")?.jsonPrimitive?.content ?: ""
            val limit = request.arguments?.get("limit")?.jsonPrimitive?.intOrNull ?: 10
            val result = runBlocking { getTaskHistory(taskId, limit) }
            CallToolResult(content = listOf(TextContent(text = result)))
        }
    }

    // === Tool Implementations ===

    private fun createReminder(
        message: String,
        cron: String,
        name: String?,
        priority: String,
        tags: List<String>
    ): String {
        if (message.isBlank()) {
            return "Error: Message is required"
        }
        if (cron.isBlank()) {
            return "Error: Cron expression is required"
        }

        val now = System.currentTimeMillis()
        val taskId = "reminder_${UUID.randomUUID().toString().take(8)}"

        val task = ScheduledTask(
            id = taskId,
            name = name ?: "Reminder: ${message.take(50)}",
            description = message,
            cronExpression = cron,
            taskType = TaskType.REMINDER,
            taskData = TaskData.Reminder(message = message, priority = priority),
            status = TaskStatus.PENDING,
            nextRunAt = calculateNextRun(cron),
            lastRunAt = null,
            createdAt = now,
            updatedAt = now,
            tags = tags
        )

        return runBlocking {
            try {
                val created = taskRepository.create(task)
                formatTaskCreated(created)
            } catch (e: Exception) {
                "Error creating reminder: ${e.message}"
            }
        }
    }

    private fun createCommand(
        command: String,
        cron: String,
        name: String?,
        workingDir: String?,
        timeout: Long,
        tags: List<String>
    ): String {
        if (command.isBlank()) {
            return "Error: Command is required"
        }
        if (cron.isBlank()) {
            return "Error: Cron expression is required"
        }

        val now = System.currentTimeMillis()
        val taskId = "cmd_${UUID.randomUUID().toString().take(8)}"

        val task = ScheduledTask(
            id = taskId,
            name = name ?: "Command: ${command.take(30)}",
            description = "Execute: $command",
            cronExpression = cron,
            taskType = TaskType.SHELL_COMMAND,
            taskData = TaskData.ShellCommand(
                command = command,
                workingDir = workingDir,
                timeoutMs = timeout
            ),
            status = TaskStatus.PENDING,
            nextRunAt = calculateNextRun(cron),
            lastRunAt = null,
            createdAt = now,
            updatedAt = now,
            tags = tags
        )

        return runBlocking {
            try {
                val created = taskRepository.create(task)
                formatTaskCreated(created)
            } catch (e: Exception) {
                "Error creating command task: ${e.message}"
            }
        }
    }

    private fun createMcpTool(
        serverName: String,
        toolName: String,
        cron: String,
        arguments: String,
        name: String?,
        tags: List<String>
    ): String {
        if (serverName.isBlank()) {
            return "Error: Server name is required"
        }
        if (toolName.isBlank()) {
            return "Error: Tool name is required"
        }
        if (cron.isBlank()) {
            return "Error: Cron expression is required"
        }

        // Validate JSON arguments
        try {
            if (arguments.isNotBlank()) {
                Json.parseToJsonElement(arguments)
            }
        } catch (e: Exception) {
            return "Error: Invalid JSON arguments: ${e.message}"
        }

        val now = System.currentTimeMillis()
        val taskId = "mcp_${UUID.randomUUID().toString().take(8)}"

        val task = ScheduledTask(
            id = taskId,
            name = name ?: "MCP: $serverName/$toolName",
            description = "Call $serverName.$toolName",
            cronExpression = cron,
            taskType = TaskType.MCP_TOOL,
            taskData = TaskData.McpTool(
                serverName = serverName,
                toolName = toolName,
                arguments = arguments
            ),
            status = TaskStatus.PENDING,
            nextRunAt = calculateNextRun(cron),
            lastRunAt = null,
            createdAt = now,
            updatedAt = now,
            tags = tags
        )

        return runBlocking {
            try {
                val created = taskRepository.create(task)
                formatTaskCreated(created)
            } catch (e: Exception) {
                "Error creating MCP tool task: ${e.message}"
            }
        }
    }

    private suspend fun listTasks(typeFilter: String?, statusFilter: String?): String {
        try {
            val tasks = when {
                typeFilter != null -> {
                    val taskType = when (typeFilter.lowercase()) {
                        "reminder" -> TaskType.REMINDER
                        "shell" -> TaskType.SHELL_COMMAND
                        "mcp" -> TaskType.MCP_TOOL
                        else -> return "Error: Invalid type filter. Use: reminder, shell, mcp"
                    }
                    taskRepository.getByType(taskType)
                }
                statusFilter != null -> {
                    val status = when (statusFilter.lowercase()) {
                        "pending" -> TaskStatus.PENDING
                        "paused" -> TaskStatus.PAUSED
                        "cancelled" -> TaskStatus.CANCELLED
                        else -> return "Error: Invalid status filter. Use: pending, paused, cancelled"
                    }
                    taskRepository.getByStatus(status)
                }
                else -> taskRepository.getAll()
            }

            if (tasks.isEmpty()) {
                return "No scheduled tasks found"
            }

            return buildString {
                appendLine("Scheduled Tasks (${tasks.size} total)")
                appendLine("=".repeat(50))
                tasks.forEach { task ->
                    appendLine()
                    appendLine("ID: ${task.id}")
                    appendLine("Name: ${task.name}")
                    appendLine("Type: ${task.taskType.name}")
                    appendLine("Status: ${task.status.name}")
                    appendLine("Cron: ${task.cronExpression}")
                    task.nextRunAt?.let {
                        appendLine("Next Run: ${formatTimestamp(it)}")
                    }
                    if (task.tags.isNotEmpty()) {
                        appendLine("Tags: ${task.tags.joinToString(", ")}")
                    }
                    appendLine("-".repeat(40))
                }
            }
        } catch (e: Exception) {
            return "Error listing tasks: ${e.message}"
        }
    }

    private suspend fun getTask(taskId: String): String {
        if (taskId.isBlank()) {
            return "Error: Task ID is required"
        }

        return try {
            val task = taskRepository.getById(taskId)
                ?: return "Error: Task not found: $taskId"

            buildString {
                appendLine("Task Details")
                appendLine("=".repeat(50))
                appendLine("ID: ${task.id}")
                appendLine("Name: ${task.name}")
                task.description?.let { appendLine("Description: $it") }
                appendLine("Type: ${task.taskType.name}")
                appendLine("Status: ${task.status.name}")
                appendLine("Cron: ${task.cronExpression}")
                appendLine()
                appendLine("Timestamps:")
                appendLine("  Created: ${formatTimestamp(task.createdAt)}")
                appendLine("  Updated: ${formatTimestamp(task.updatedAt)}")
                task.nextRunAt?.let { appendLine("  Next Run: ${formatTimestamp(it)}") }
                    ?: appendLine("  Next Run: (not scheduled)")
                task.lastRunAt?.let { appendLine("  Last Run: ${formatTimestamp(it)}") }
                    ?: appendLine("  Last Run: (never)")

                if (task.tags.isNotEmpty()) {
                    appendLine()
                    appendLine("Tags: ${task.tags.joinToString(", ")}")
                }

                appendLine()
                appendLine("Task Data:")
                when (val data = task.taskData) {
                    is TaskData.Reminder -> {
                        appendLine("  Message: ${data.message}")
                        appendLine("  Priority: ${data.priority}")
                    }
                    is TaskData.ShellCommand -> {
                        appendLine("  Command: ${data.command}")
                        data.workingDir?.let { appendLine("  Working Dir: $it") }
                        appendLine("  Timeout: ${data.timeoutMs}ms")
                    }
                    is TaskData.McpTool -> {
                        appendLine("  Server: ${data.serverName}")
                        appendLine("  Tool: ${data.toolName}")
                        appendLine("  Arguments: ${data.arguments}")
                    }
                }
            }
        } catch (e: Exception) {
            "Error getting task: ${e.message}"
        }
    }

    private suspend fun cancelTask(taskId: String): String {
        if (taskId.isBlank()) {
            return "Error: Task ID is required"
        }

        return try {
            val task = taskRepository.getById(taskId)
                ?: return "Error: Task not found: $taskId"

            if (task.status == TaskStatus.CANCELLED) {
                return "Task is already cancelled: $taskId"
            }

            taskRepository.updateStatus(taskId, TaskStatus.CANCELLED, System.currentTimeMillis())

            "Task cancelled successfully\nID: $taskId\nName: ${task.name}"
        } catch (e: Exception) {
            "Error cancelling task: ${e.message}"
        }
    }

    private suspend fun deleteTask(taskId: String): String {
        if (taskId.isBlank()) {
            return "Error: Task ID is required"
        }

        return try {
            val task = taskRepository.getById(taskId)
                ?: return "Error: Task not found: $taskId"

            val taskName = task.name
            taskRepository.delete(taskId)

            "Task deleted permanently\nID: $taskId\nName: $taskName"
        } catch (e: Exception) {
            "Error deleting task: ${e.message}"
        }
    }

    private suspend fun pauseTask(taskId: String): String {
        if (taskId.isBlank()) {
            return "Error: Task ID is required"
        }

        return try {
            val task = taskRepository.getById(taskId)
                ?: return "Error: Task not found: $taskId"

            if (task.status != TaskStatus.PENDING) {
                return "Error: Can only pause PENDING tasks. Current status: ${task.status.name}"
            }

            taskRepository.updateStatus(taskId, TaskStatus.PAUSED, System.currentTimeMillis())

            "Task paused successfully\nID: $taskId\nName: ${task.name}\nUse resume_task to resume"
        } catch (e: Exception) {
            "Error pausing task: ${e.message}"
        }
    }

    private suspend fun resumeTask(taskId: String): String {
        if (taskId.isBlank()) {
            return "Error: Task ID is required"
        }

        return try {
            val task = taskRepository.getById(taskId)
                ?: return "Error: Task not found: $taskId"

            if (task.status != TaskStatus.PAUSED) {
                return "Error: Can only resume PAUSED tasks. Current status: ${task.status.name}"
            }

            val nextRun = calculateNextRun(task.cronExpression)
            taskRepository.updateStatus(taskId, TaskStatus.PENDING, System.currentTimeMillis())
            if (nextRun != null) {
                taskRepository.updateRunTimestamps(taskId, nextRun, task.lastRunAt ?: 0, System.currentTimeMillis())
            }

            "Task resumed successfully\nID: $taskId\nName: ${task.name}\nNext Run: ${nextRun?.let { formatTimestamp(it) } ?: "N/A"}"
        } catch (e: Exception) {
            "Error resuming task: ${e.message}"
        }
    }

    private suspend fun getTaskHistory(taskId: String, limit: Int): String {
        if (taskId.isBlank()) {
            return "Error: Task ID is required"
        }

        return try {
            val task = taskRepository.getById(taskId)
                ?: return "Error: Task not found: $taskId"

            val executions = executionRepository.getByTaskId(taskId)
                .sortedByDescending { it.startedAt }
                .take(limit)

            if (executions.isEmpty()) {
                return "No execution history found for task: $taskId"
            }

            buildString {
                appendLine("Task Execution History")
                appendLine("=".repeat(50))
                appendLine("Task: ${task.name} ($taskId)")
                appendLine("Total Executions: ${executions.size} (showing last ${minOf(limit, executions.size)})")
                appendLine()

                executions.forEachIndexed { index, execution ->
                    appendLine("--- Execution #${index + 1} ---")
                    appendLine("ID: ${execution.id}")
                    appendLine("Status: ${execution.status.name}")
                    appendLine("Started: ${formatTimestamp(execution.startedAt)}")
                    execution.completedAt?.let {
                        appendLine("Completed: ${formatTimestamp(it)}")
                        val duration = it - execution.startedAt
                        appendLine("Duration: ${formatDuration(duration)}")
                    }
                    execution.result?.let { appendLine("Result: $it") }
                    execution.error?.let { appendLine("Error: $it") }
                    appendLine()
                }
            }
        } catch (e: Exception) {
            "Error getting task history: ${e.message}"
        }
    }

    // === Helper Methods ===

    private fun calculateNextRun(cronExpression: String): Long? {
        // Simplified implementation - in real code would use cron parser
        // For now, just return 1 minute from now
        return System.currentTimeMillis() + 60_000
    }

    private fun formatTaskCreated(task: ScheduledTask): String {
        return buildString {
            appendLine("Task created successfully!")
            appendLine("=".repeat(50))
            appendLine("ID: ${task.id}")
            appendLine("Name: ${task.name}")
            appendLine("Type: ${task.taskType.name}")
            appendLine("Status: ${task.status.name}")
            appendLine("Cron: ${task.cronExpression}")
            task.nextRunAt?.let {
                appendLine("Next Run: ${formatTimestamp(it)}")
            }
            if (task.tags.isNotEmpty()) {
                appendLine("Tags: ${task.tags.joinToString(", ")}")
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        return format.format(date)
    }

    private fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m ${seconds % 60}s"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }

    // === Direct API (non-MCP) ===

    /**
     * Create reminder directly (without MCP protocol)
     */
    fun createReminderSync(
        message: String,
        cron: String,
        name: String?,
        priority: String,
        tags: List<String>
    ): Result<String> {
        return try {
            Result.success(createReminder(message, cron, name, priority, tags))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create command task directly
     */
    fun createCommandSync(
        command: String,
        cron: String,
        name: String?,
        workingDir: String?,
        timeout: Long,
        tags: List<String>
    ): Result<String> {
        return try {
            Result.success(createCommand(command, cron, name, workingDir, timeout, tags))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create MCP tool task directly
     */
    fun createMcpToolSync(
        serverName: String,
        toolName: String,
        cron: String,
        arguments: String,
        name: String?,
        tags: List<String>
    ): Result<String> {
        return try {
            Result.success(createMcpTool(serverName, toolName, cron, arguments, name, tags))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * List tasks directly
     */
    suspend fun listTasksSync(typeFilter: String?, statusFilter: String?): Result<String> {
        return try {
            Result.success(listTasks(typeFilter, statusFilter))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get task directly
     */
    suspend fun getTaskSync(taskId: String): Result<String> {
        return try {
            Result.success(getTask(taskId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cancel task directly
     */
    suspend fun cancelTaskSync(taskId: String): Result<String> {
        return try {
            Result.success(cancelTask(taskId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete task directly
     */
    suspend fun deleteTaskSync(taskId: String): Result<String> {
        return try {
            Result.success(deleteTask(taskId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pause task directly
     */
    suspend fun pauseTaskSync(taskId: String): Result<String> {
        return try {
            Result.success(pauseTask(taskId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resume task directly
     */
    suspend fun resumeTaskSync(taskId: String): Result<String> {
        return try {
            Result.success(resumeTask(taskId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get task history directly
     */
    suspend fun getHistorySync(taskId: String, limit: Int): Result<String> {
        return try {
            Result.success(getTaskHistory(taskId, limit))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get list of available tools.
     */
    fun getAvailableTools(): List<String> = listOf(
        "schedule_reminder",
        "schedule_command",
        "schedule_mcp_tool",
        "list_scheduled_tasks",
        "get_task",
        "cancel_task",
        "delete_task",
        "pause_task",
        "resume_task",
        "get_task_history"
    )

    /**
     * Start the MCP server using STDIO transport.
     */
    fun start() = runBlocking {
        val transport = StdioServerTransport(
            inputStream = System.`in`.asSource().buffered(),
            outputStream = System.out.asSink().buffered()
        )
        server.connect(transport)
        // Keep running until closed
        transport.onClose {
            // Connection closed
        }
    }
}

fun main() {
    SchedulerMcpServer(
        taskRepository = org.koin.java.KoinJavaComponent.getKoin().get(),
        executionRepository = org.koin.java.KoinJavaComponent.getKoin().get()
    ).start()
}
