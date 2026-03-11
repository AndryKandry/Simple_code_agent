package ru.agent.features.chat.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Message DTO for DeepSeek API.
 *
 * Supports:
 * - Regular messages (user, assistant, system)
 * - Tool call messages from assistant
 * - Tool result messages
 */
@Serializable
data class MessageDto(
    @SerialName("role")
    val role: String,
    @SerialName("content")
    val content: String? = null,
    // For tool calls from assistant
    @SerialName("tool_calls")
    val toolCalls: List<ToolCallDto>? = null,
    // For tool result messages
    @SerialName("tool_call_id")
    val toolCallId: String? = null,
    @SerialName("name")
    val name: String? = null
) {
    companion object {
        fun user(content: String) = MessageDto(
            role = "user",
            content = content
        )

        fun assistant(content: String) = MessageDto(
            role = "assistant",
            content = content
        )

        fun system(content: String) = MessageDto(
            role = "system",
            content = content
        )

        fun toolCall(toolCalls: List<ToolCallDto>) = MessageDto(
            role = "assistant",
            content = null,
            toolCalls = toolCalls
        )

        fun toolResult(toolCallId: String, name: String, content: String) = MessageDto(
            role = "tool",
            content = content,
            toolCallId = toolCallId,
            name = name
        )
    }
}
