package ru.agent.features.chat.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Chat response from DeepSeek API.
 *
 * Supports:
 * - Regular text responses
 * - Function calling (tool_calls)
 */
@Serializable
data class ChatResponse(
    @SerialName("id")
    val id: String,
    @SerialName("choices")
    val choices: List<Choice>,
    @SerialName("usage")
    val usage: Usage
) {
    /**
     * Check if response contains tool calls.
     */
    fun hasToolCalls(): Boolean {
        return choices.any { it.hasToolCalls() }
    }

    /**
     * Get all tool calls from all choices.
     */
    fun getAllToolCalls(): List<ToolCallDto> {
        return choices.flatMap { it.message.toolCalls ?: emptyList() }
    }
}

@Serializable
data class Choice(
    @SerialName("index")
    val index: Int,
    @SerialName("message")
    val message: MessageDto,
    @SerialName("finish_reason")
    val finishReason: String? = null
) {
    /**
     * Check if this choice contains tool calls.
     */
    fun hasToolCalls(): Boolean {
        return !message.toolCalls.isNullOrEmpty()
    }
}

@Serializable
data class Usage(
    @SerialName("prompt_tokens")
    val promptTokens: Int,
    @SerialName("completion_tokens")
    val completionTokens: Int,
    @SerialName("total_tokens")
    val totalTokens: Int
)
