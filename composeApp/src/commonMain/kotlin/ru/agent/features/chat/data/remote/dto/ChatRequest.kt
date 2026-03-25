package ru.agent.features.chat.data.remote.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Chat request for DeepSeek API.
 *
 * Supports:
 * - Regular chat messages
 * - Function calling (tools)
 * - Tool choice control (auto, required, none)
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ChatRequest(
    @EncodeDefault
    @SerialName("model")
    val model: String = "deepseek-chat",
    @SerialName("messages")
    val messages: List<MessageDto>,
    @EncodeDefault
    @SerialName("temperature")
    val temperature: Double = 0.7,
    @EncodeDefault
    @SerialName("max_tokens")
    val maxTokens: Int = 2000,
    // Function calling support
    @SerialName("tools")
    val tools: List<ToolDefinitionDto>? = null,
    /**
     * Controls which (if any) tool is called by the model.
     * - "auto": model decides whether to call a tool (default)
     * - "required": model MUST call at least one tool
     * - "none": model must NOT call any tools
     */
    @SerialName("tool_choice")
    val toolChoice: String? = null,
    /**
     * Whether to stream responses.
     * - false: return complete response (default, compatible with DeepSeek)
     * - true: stream responses (used by some Ollama endpoints)
     */
    @EncodeDefault
    @SerialName("stream")
    val stream: Boolean = false
) {
    companion object {
        /**
         * Create a simple chat request without tools.
         */
        fun simple(
            messages: List<MessageDto>,
            model: String = "deepseek-chat",
            temperature: Double = 0.7,
            maxTokens: Int = 2000
        ) = ChatRequest(
            model = model,
            messages = messages,
            temperature = temperature,
            maxTokens = maxTokens,
            tools = null,
            toolChoice = null
        )

        /**
         * Create a chat request with function calling tools.
         *
         * @param messages Chat messages
         * @param tools Available tools
         * @param toolChoice Control tool usage: "auto" (default), "required", "none"
         */
        fun withTools(
            messages: List<MessageDto>,
            tools: List<ToolDefinitionDto>,
            model: String = "deepseek-chat",
            temperature: Double = 0.7,
            maxTokens: Int = 2000,
            toolChoice: String = "auto"
        ) = ChatRequest(
            model = model,
            messages = messages,
            temperature = temperature,
            maxTokens = maxTokens,
            tools = tools,
            toolChoice = toolChoice
        )

        /**
         * Create a chat request that REQUIRES tool usage.
         * The model MUST call at least one tool.
         */
        fun withRequiredTools(
            messages: List<MessageDto>,
            tools: List<ToolDefinitionDto>,
            model: String = "deepseek-chat",
            temperature: Double = 0.7,
            maxTokens: Int = 2000
        ) = ChatRequest(
            model = model,
            messages = messages,
            temperature = temperature,
            maxTokens = maxTokens,
            tools = tools,
            toolChoice = "required"
        )
    }
}
