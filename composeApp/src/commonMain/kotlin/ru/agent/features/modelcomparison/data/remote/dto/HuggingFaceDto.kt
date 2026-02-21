package ru.agent.features.modelcomparison.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Chat completion request for HuggingFace Inference Providers API
 * Uses OpenAI-compatible format
 *
 * API Documentation: https://huggingface.co/docs/inference-providers
 */
@Serializable
data class HuggingFaceChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerialName("max_tokens")
    val max_tokens: Int = 512,
    val temperature: Float = 0.7f,
    val stream: Boolean = false
)

/**
 * Chat message in OpenAI format
 */
@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

/**
 * Chat completion response from HuggingFace Inference Providers API
 * OpenAI-compatible format
 */
@Serializable
data class HuggingFaceChatResponse(
    val id: String? = null,
    val choices: List<ChatChoice>? = null,
    val usage: ChatUsage? = null,
    @SerialName("error")
    val errorDetails: ChatError? = null
) {
    val isSuccess: Boolean
        get() = errorDetails == null && !choices.isNullOrEmpty()

    val generated_text: String?
        get() = choices?.firstOrNull()?.message?.content

    val generatedTokens: Int
        get() = usage?.completion_tokens ?: (generated_text?.split("\\s+".toRegex())?.size ?: 0)

    /**
     * Error message as string for backward compatibility with repository
     */
    val error: String?
        get() = errorDetails?.toString()
}

/**
 * Chat completion choice
 */
@Serializable
data class ChatChoice(
    val index: Int = 0,
    val message: ChatMessage,
    @SerialName("finish_reason")
    val finish_reason: String? = null
)

/**
 * Token usage information
 */
@Serializable
data class ChatUsage(
    @SerialName("prompt_tokens")
    val prompt_tokens: Int = 0,
    @SerialName("completion_tokens")
    val completion_tokens: Int = 0,
    @SerialName("total_tokens")
    val total_tokens: Int = 0
)

/**
 * Error response from HuggingFace API
 */
@Serializable
data class ChatError(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null
) {
    override fun toString(): String = message ?: type ?: code ?: "Unknown error"
}

// Legacy DTOs for backward compatibility (can be removed later)

/**
 * @deprecated Use HuggingFaceChatRequest instead
 */
@Serializable
data class HuggingFaceTextGenRequest(
    val inputs: String,
    val parameters: HuggingFaceTextGenParams? = null
)

/**
 * @deprecated Use HuggingFaceChatRequest parameters instead
 */
@Serializable
data class HuggingFaceTextGenParams(
    @SerialName("max_new_tokens")
    val max_new_tokens: Int = 256,
    val temperature: Float = 0.7f,
    @SerialName("return_full_text")
    val return_full_text: Boolean = false,
    @SerialName("do_sample")
    val do_sample: Boolean = true,
    @SerialName("top_p")
    val top_p: Float = 0.9f,
    @SerialName("top_k")
    val top_k: Int = 50,
    @SerialName("repetition_penalty")
    val repetition_penalty: Float = 1.0f
)

/**
 * @deprecated Use HuggingFaceChatResponse instead
 */
@Serializable
data class HuggingFaceTextGenResponse(
    @SerialName("generated_text")
    val generated_text: String? = null,
    val error: String? = null,
    val details: HuggingFaceTextGenDetails? = null
) {
    val isSuccess: Boolean
        get() = error == null && !generated_text.isNullOrBlank()

    val generatedTokens: Int
        get() = details?.generated_tokens ?: 0
}

/**
 * @deprecated
 */
@Serializable
data class HuggingFaceTextGenDetails(
    @SerialName("finish_reason")
    val finish_reason: String? = null,
    @SerialName("generated_tokens")
    val generated_tokens: Int = 0,
    val seed: Long? = null
)

/**
 * @deprecated Use ChatError instead
 */
@Serializable
data class HuggingFaceError(
    val error: String? = null,
    val estimated_time: Double? = null,
    val model: String? = null
)
