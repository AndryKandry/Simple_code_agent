package ru.agent.features.chat.tools

import co.touchlab.kermit.Logger
import kotlinx.serialization.json.*
import ru.agent.features.chat.data.remote.dto.FunctionDefinitionDto
import ru.agent.features.chat.data.remote.dto.ToolCallDto
import ru.agent.features.chat.data.remote.dto.ToolDefinitionDto
import ru.agent.features.chat.domain.tools.ToolExecutor
import ru.agent.mcp.McpManager
import ru.agent.mcp.ToolInfo

/**
 * JVM implementation of ToolExecutor using McpManager.
 *
 * Bridges MCP tools to DeepSeek API function calling format.
 * Handles tool name sanitization (replacing ':' with '_') for API compatibility.
 *
 * @property mcpManager MCP manager for accessing built-in and external tools
 */
class ToolExecutorImpl(
    private val mcpManager: McpManager
) : ToolExecutor {

    private val logger = Logger.withTag("ToolExecutor")

    /**
     * Get all available tools in DeepSeek API format.
     *
     * @return List of tool definitions compatible with DeepSeek function calling
     */
    override suspend fun getToolsForApi(): List<ToolDefinitionDto> {
        return mcpManager.getAllAvailableTools().map { toolInfo ->
            ToolDefinitionDto(
                type = "function",
                function = FunctionDefinitionDto(
                    name = sanitizeToolName(toolInfo.fullName),
                    description = toolInfo.description,
                    parameters = getInputSchema(toolInfo)
                )
            )
        }
    }

    /**
     * Execute a tool call from the AI model.
     *
     * @param toolCall Tool call request from DeepSeek
     * @return Result containing the tool output or error
     */
    override suspend fun executeToolCall(toolCall: ToolCallDto): Result<String> {
        val toolName = desanitizeToolName(toolCall.function.name)
        val arguments = parseArguments(toolCall.function.arguments)

        logger.d { "Executing tool: $toolName with ${arguments.size} arguments" }

        val nonNullArguments = arguments.filterValues { it != null }.mapValues { it.value!! }
        return mcpManager.executeToolByFullName(toolName, nonNullArguments)
    }

    // === Private Helpers ===

    /**
     * Sanitize tool name by replacing ':' with '_'.
     * DeepSeek API doesn't support ':' in function names.
     *
     * @param fullName Full tool name in format "server:tool"
     * @return Sanitized name in format "server_tool"
     */
    private fun sanitizeToolName(fullName: String): String {
        return fullName.replace(":", "_")
    }

    /**
     * Reverse sanitization of tool name.
     * Converts "server_tool" back to "server:tool" format.
     *
     * @param sanitizedName Sanitized tool name in format "server_tool"
     * @return Original name in format "server:tool"
     */
    private fun desanitizeToolName(sanitizedName: String): String {
        val underscoreIndex = sanitizedName.indexOf('_')
        return if (underscoreIndex > 0) {
            sanitizedName.replaceFirst("_", ":")
        } else {
            sanitizedName
        }
    }

    /**
     * Parse JSON arguments string into a map.
     * Handles null values gracefully by converting them to null in the map.
     *
     * @param argumentsJson JSON string with arguments
     * @return Map of argument names to values (null values preserved as null)
     */
    private fun parseArguments(argumentsJson: String): Map<String, Any?> {
        return try {
            val jsonElement = Json.parseToJsonElement(argumentsJson)
            jsonElement.jsonObject.mapValues { (_, value) ->
                extractValue(value)
            }
        } catch (e: Exception) {
            logger.w { "Failed to parse arguments JSON: ${e.message}" }
            emptyMap()
        }
    }

    /**
     * Extract a value from JSON element, handling nulls gracefully.
     * Returns null for JsonNull instead of using unsafe cast.
     *
     * @param element JSON element to extract from
     * @return Extracted value (can be null for JsonNull)
     */
    private fun extractValue(element: JsonElement): Any? {
        return when (element) {
            is JsonPrimitive -> when {
                element.isString -> element.content
                element.booleanOrNull != null -> element.boolean
                element.longOrNull != null -> element.long
                element.doubleOrNull != null -> element.double
                else -> element.content
            }
            is JsonArray -> element.map { extractValue(it) }
            is JsonObject -> element.mapValues { extractValue(it.value) }
            JsonNull -> null  // Safe null handling - no unsafe cast
        }
    }

    /**
     * Get input schema for a tool based on its server and name.
     *
     * @param toolInfo Tool information
     * @return JSON schema for tool parameters
     */
    private fun getInputSchema(toolInfo: ToolInfo): JsonObject {
        val parts = toolInfo.fullName.split(":")
        if (parts.size != 2) {
            logger.w { "Invalid tool name format: ${toolInfo.fullName}, expected 'server:tool'" }
            return getDefaultSchema()
        }

        val (serverName, toolName) = parts

        return when (serverName) {
            "filesystem" -> getFilesystemToolSchema(toolName)
            "terminal" -> getTerminalToolSchema(toolName)
            "scheduler" -> getSchedulerToolSchema(toolName)
            else -> {
                logger.d { "Using default schema for unknown server: $serverName" }
                getDefaultSchema()
            }
        }
    }

    /**
     * Get JSON schema for filesystem tools.
     *
     * @param toolName Name of the filesystem tool
     * @return JSON schema with parameters
     */
    private fun getFilesystemToolSchema(toolName: String): JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            when (toolName) {
                "read_file", "list_directory", "search_files",
                "delete_file", "create_directory", "file_exists" -> {
                    put("path", buildJsonObject {
                        put("type", "string")
                        put("description", "FULL ABSOLUTE path to file or directory (e.g., /Users/user/project/file.txt). MUST be absolute path starting from root.")
                    })
                    if (toolName == "search_files") {
                        put("pattern", buildJsonObject {
                            put("type", "string")
                            put("description", "Glob pattern to match files")
                            put("default", "*")
                        })
                    }
                    if (toolName == "delete_file") {
                        put("confirm", buildJsonObject {
                            put("type", "boolean")
                            put("description", "Must be true to confirm deletion")
                            put("default", false)
                        })
                    }
                }
                "write_file" -> {
                    put("path", buildJsonObject {
                        put("type", "string")
                        put("description", "FULL ABSOLUTE path to file (e.g., /Users/user/project/file.txt). MUST be absolute path starting from root.")
                    })
                    put("content", buildJsonObject {
                        put("type", "string")
                        put("description", "Content to write to file")
                    })
                }
                "copy_file" -> {
                    put("source", buildJsonObject {
                        put("type", "string")
                        put("description", "FULL ABSOLUTE path to source file. MUST be absolute path.")
                    })
                    put("destination", buildJsonObject {
                        put("type", "string")
                        put("description", "FULL ABSOLUTE path to destination. MUST be absolute path.")
                    })
                }
            }
        })
        put("required", buildJsonArray {
            when (toolName) {
                "read_file", "list_directory", "delete_file",
                "create_directory", "file_exists" -> add("path")
                "search_files" -> { add("path"); add("pattern") }
                "write_file" -> { add("path"); add("content") }
                "copy_file" -> { add("source"); add("destination") }
            }
        })
    }

    /**
     * Get JSON schema for terminal tools.
     *
     * @param toolName Name of the terminal tool
     * @return JSON schema with parameters
     */
    private fun getTerminalToolSchema(toolName: String): JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            when (toolName) {
                "execute_command" -> {
                    put("command", buildJsonObject {
                        put("type", "string")
                        put("description", "Command to execute (must be in whitelist)")
                    })
                    put("timeout", buildJsonObject {
                        put("type", "number")
                        put("description", "Timeout in milliseconds")
                        put("default", 30000)
                    })
                    put("working_dir", buildJsonObject {
                        put("type", "string")
                        put("description", "Working directory (optional)")
                    })
                }
                "run_shell_script" -> {
                    put("script_path", buildJsonObject {
                        put("type", "string")
                        put("description", "Path to shell script")
                    })
                    put("args", buildJsonObject {
                        put("type", "array")
                        put("description", "Arguments to pass to script")
                        put("items", buildJsonObject { put("type", "string") })
                    })
                    put("timeout", buildJsonObject {
                        put("type", "number")
                        put("description", "Timeout in milliseconds")
                        put("default", 60000)
                    })
                }
                "check_command_available" -> {
                    put("command", buildJsonObject {
                        put("type", "string")
                        put("description", "Command to check")
                    })
                }
            }
        })
        put("required", buildJsonArray {
            when (toolName) {
                "execute_command" -> add("command")
                "run_shell_script" -> add("script_path")
                "check_command_available" -> add("command")
            }
        })
    }

    /**
     * Get JSON schema for scheduler tools.
     *
     * @param toolName Name of the scheduler tool
     * @return JSON schema with parameters
     */
    private fun getSchedulerToolSchema(toolName: String): JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            when (toolName) {
                // === Creation tools ===
                "schedule_reminder" -> {
                    put("message", buildJsonObject {
                        put("type", "string")
                        put("description", "Reminder message text to display")
                    })
                    put("cron", buildJsonObject {
                        put("type", "string")
                        put("description", "Cron expression for scheduling (e.g., '* * * * *' for every minute, '0 9 * * *' for daily at 9am)")
                    })
                    put("name", buildJsonObject {
                        put("type", "string")
                        put("description", "Optional name for the reminder task")
                    })
                    put("priority", buildJsonObject {
                        put("type", "string")
                        put("description", "Task priority: LOW, NORMAL, HIGH")
                        put("default", "NORMAL")
                        put("enum", buildJsonArray {
                            add("LOW"); add("NORMAL"); add("HIGH")
                        })
                    })
                    put("tags", buildJsonObject {
                        put("type", "array")
                        put("description", "Optional tags for categorization")
                        put("items", buildJsonObject { put("type", "string") })
                    })
                }
                "schedule_command" -> {
                    put("command", buildJsonObject {
                        put("type", "string")
                        put("description", "Shell command to execute")
                    })
                    put("cron", buildJsonObject {
                        put("type", "string")
                        put("description", "Cron expression for scheduling")
                    })
                    put("name", buildJsonObject {
                        put("type", "string")
                        put("description", "Optional name for the command task")
                    })
                    put("working_dir", buildJsonObject {
                        put("type", "string")
                        put("description", "Working directory for command execution")
                    })
                    put("timeout", buildJsonObject {
                        put("type", "number")
                        put("description", "Timeout in milliseconds")
                        put("default", 30000)
                    })
                    put("tags", buildJsonObject {
                        put("type", "array")
                        put("description", "Optional tags for categorization")
                        put("items", buildJsonObject { put("type", "string") })
                    })
                }
                "schedule_mcp_tool" -> {
                    put("server", buildJsonObject {
                        put("type", "string")
                        put("description", "MCP server name (e.g., 'filesystem', 'terminal')")
                    })
                    put("tool", buildJsonObject {
                        put("type", "string")
                        put("description", "Tool name on the MCP server")
                    })
                    put("cron", buildJsonObject {
                        put("type", "string")
                        put("description", "Cron expression for scheduling")
                    })
                    put("arguments", buildJsonObject {
                        put("type", "object")
                        put("description", "Arguments to pass to the tool")
                    })
                    put("name", buildJsonObject {
                        put("type", "string")
                        put("description", "Optional name for the task")
                    })
                    put("tags", buildJsonObject {
                        put("type", "array")
                        put("description", "Optional tags for categorization")
                        put("items", buildJsonObject { put("type", "string") })
                    })
                }

                // === Query tools ===
                "list_scheduled_tasks" -> {
                    put("type", buildJsonObject {
                        put("type", "string")
                        put("description", "Filter by task type: REMINDER, COMMAND, MCP_TOOL")
                        put("enum", buildJsonArray {
                            add("REMINDER"); add("COMMAND"); add("MCP_TOOL")
                        })
                    })
                    put("status", buildJsonObject {
                        put("type", "string")
                        put("description", "Filter by status: SCHEDULED, PAUSED, COMPLETED, CANCELLED, FAILED")
                        put("enum", buildJsonArray {
                            add("SCHEDULED"); add("PAUSED"); add("COMPLETED"); add("CANCELLED"); add("FAILED")
                        })
                    })
                }
                "get_task" -> {
                    put("task_id", buildJsonObject {
                        put("type", "string")
                        put("description", "Task ID to retrieve")
                    })
                }

                // === Management tools ===
                "cancel_task" -> {
                    put("task_id", buildJsonObject {
                        put("type", "string")
                        put("description", "Task ID to cancel")
                    })
                }
                "pause_task" -> {
                    put("task_id", buildJsonObject {
                        put("type", "string")
                        put("description", "Task ID to pause")
                    })
                }
                "resume_task" -> {
                    put("task_id", buildJsonObject {
                        put("type", "string")
                        put("description", "Task ID to resume")
                    })
                }
                "get_task_history" -> {
                    put("task_id", buildJsonObject {
                        put("type", "string")
                        put("description", "Task ID to get history for")
                    })
                    put("limit", buildJsonObject {
                        put("type", "number")
                        put("description", "Maximum number of history entries to return")
                        put("default", 10)
                    })
                }
            }
        })
        put("required", buildJsonArray {
            when (toolName) {
                "schedule_reminder" -> { add("message"); add("cron") }
                "schedule_command" -> { add("command"); add("cron") }
                "schedule_mcp_tool" -> { add("server"); add("tool"); add("cron") }
                "get_task", "cancel_task", "pause_task", "resume_task" -> add("task_id")
                "get_task_history" -> add("task_id")
                // list_scheduled_tasks has no required params
                else -> { }
            }
        })
    }

    /**
     * Get default empty JSON schema for unknown tools.
     *
     * @return Empty object schema
     */
    private fun getDefaultSchema(): JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject { })
    }
}
