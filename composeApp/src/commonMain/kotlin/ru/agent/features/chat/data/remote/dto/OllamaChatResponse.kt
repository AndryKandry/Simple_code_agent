package ru.agent.features.chat.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ollama API chat response.
 *
 * Ollama uses a different response format than OpenAI/DeepSeek:
 * - Has "message" object instead of "choices" array
 * - Has "done" flag instead of "finish_reason"
 * - Has timing information (total_duration, load_duration, etc.)
 * - Has "created_at" timestamp
 */
@Serializable
data class OllamaChatResponse(
    @SerialName("model")
    val model: String,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("message")
    val message: OllamaMessage,

    @SerialName("done")
    val done: Boolean? = null,

    @SerialName("done_reason")
    val doneReason: String? = null,

    // Timing information (optional)
    @SerialName("total_duration")
    val totalDuration: Long? = null,

    @SerialName("load_duration")
    val loadDuration: Long? = null,

    @SerialName("prompt_eval_count")
    val promptEvalCount: Int? = null,

    @SerialName("prompt_eval_duration")
    val promptEvalDuration: Long? = null,

    @SerialName("eval_count")
    val evalCount: Int? = null,

    @SerialName("eval_duration")
    val evalDuration: Long? = null
)

/**
 * Message in Ollama response format.
 */
@Serializable
data class OllamaMessage(
    @SerialName("role")
    val role: String,

    @SerialName("content")
    val content: String,

    // Tool calls (if function calling is used)
    @SerialName("tool_calls")
    val toolCalls: List<ToolCallDto>? = null
)

/**
 * Extension function to convert Ollama response to standard ChatResponse format.
 */
fun OllamaChatResponse.toChatResponse(): ChatResponse {
    val finishReason = when {
        doneReason == "stop" -> "stop"
        doneReason == "length" -> "length"
        doneReason != null -> doneReason
        done == true -> "stop"
        else -> null
    }

    val choice = Choice(
        index = 0,
        message = MessageDto(
            role = message.role,
            content = message.content,
            toolCalls = message.toolCalls
        ),
        finishReason = finishReason
    )

    val usage = Usage(
        promptTokens = promptEvalCount ?: 0,
        completionTokens = evalCount ?: 0,
        totalTokens = (promptEvalCount ?: 0) + (evalCount ?: 0)
    )

    return ChatResponse(
        id = createdAt ?: getCurrentTimestamp(),
        choices = listOf(choice),
        usage = usage
    )
}

/**
 * Get current timestamp as string.
 * This is implemented separately for each platform.
 */
internal expect fun getCurrentTimestamp(): String
