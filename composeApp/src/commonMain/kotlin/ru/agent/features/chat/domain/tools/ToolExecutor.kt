package ru.agent.features.chat.domain.tools

import ru.agent.features.chat.data.remote.dto.ToolCallDto

/**
 * Interface for executing tools.
 *
 * Implemented in platform-specific code (JVM) to access MCP infrastructure.
 */
interface ToolExecutor {
    /**
     * Get all available tools in DeepSeek API format.
     */
    suspend fun getToolsForApi(): List<ru.agent.features.chat.data.remote.dto.ToolDefinitionDto>

    /**
     * Execute a tool call from the AI model.
     */
    suspend fun executeToolCall(toolCall: ToolCallDto): Result<String>

    /**
     * Execute multiple tool calls.
     */
    suspend fun executeToolCalls(toolCalls: List<ToolCallDto>): Map<String, Result<String>> {
        return toolCalls.associate { toolCall ->
            toolCall.id to executeToolCall(toolCall)
        }
    }
}
