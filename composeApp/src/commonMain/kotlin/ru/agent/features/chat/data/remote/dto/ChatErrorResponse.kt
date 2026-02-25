package ru.agent.features.chat.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Error response from DeepSeek API.
 *
 * Example:
 * {
 *   "error": {
 *     "message": "This model's maximum context length is 131072 tokens...",
 *     "type": "invalid_request_error",
 *     "param": null,
 *     "code": "invalid_request_error"
 *   }
 * }
 */
@Serializable
data class ChatErrorResponse(
    @SerialName("error")
    val error: ApiError
)

@Serializable
data class ApiError(
    @SerialName("message")
    val message: String,
    @SerialName("type")
    val type: String? = null,
    @SerialName("param")
    val param: String? = null,
    @SerialName("code")
    val code: String? = null
) {
    /**
     * Check if this is a context length exceeded error
     */
    fun isContextLengthExceeded(): Boolean {
        return code == "invalid_request_error" &&
                message.contains("context length", ignoreCase = true)
    }

    /**
     * Get a user-friendly error message
     */
    fun toUserMessage(): String {
        return when {
            isContextLengthExceeded() -> {
                // Extract token info from message if available
                val requestedMatch = Regex("""requested (\d+) tokens""").find(message)
                val maxMatch = Regex("""maximum context length is (\d+) tokens""").find(message)

                if (requestedMatch != null && maxMatch != null) {
                    val requested = requestedMatch.groupValues[1]
                    val max = maxMatch.groupValues[1]
                    "Context limit exceeded: your message uses $requested tokens, but the maximum is $max tokens. Please start a new chat or reduce message length."
                } else {
                    "Context limit exceeded. Please start a new chat session to continue."
                }
            }
            else -> message
        }
    }
}
