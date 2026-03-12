package ru.agent.features.scheduler.domain.model

import kotlinx.serialization.Serializable

/**
 * Sealed class representing task-specific data.
 */
@Serializable
sealed class TaskData {
    /**
     * Reminder task data.
     *
     * @property message The reminder message to display
     * @property priority Priority level (low, medium, high)
     */
    @Serializable
    data class Reminder(
        val message: String,
        val priority: String = "medium"
    ) : TaskData()

    /**
     * Shell command task data.
     *
     * @property command The shell command to execute
     * @property workingDir Working directory (optional)
     * @property timeoutMs Timeout in milliseconds
     */
    @Serializable
    data class ShellCommand(
        val command: String,
        val workingDir: String? = null,
        val timeoutMs: Long = 30000
    ) : TaskData()

    /**
     * MCP tool call task data.
     *
     * @property serverName MCP server name
     * @property toolName Tool name
     * @property arguments Tool arguments as JSON string
     */
    @Serializable
    data class McpTool(
        val serverName: String,
        val toolName: String,
        val arguments: String = "{}"
    ) : TaskData()
}
