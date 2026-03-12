package ru.agent.features.scheduler.domain.model

/**
 * Type of scheduled task.
 */
enum class TaskType {
    /**
     * Simple reminder/notification task.
     */
    REMINDER,

    /**
     * Shell command execution task.
     */
    SHELL_COMMAND,

    /**
     * MCP tool call task.
     */
    MCP_TOOL
}
