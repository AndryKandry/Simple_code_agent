package ru.agent.features.modelcomparison.domain.model

/**
 * Response from a single model
 */
data class ModelResponse(
    val modelType: ModelType,
    val content: String,
    val tokenUsage: ModelTokenUsage,
    val durationMs: Long,
    val finishReason: String,
    val loading: Boolean = false,
    val error: String? = null
) {
    val isSuccess: Boolean
        get() = error == null && content.isNotBlank()

    val wordCount: Int
        get() = content.split("\\s+".toRegex()).count { it.isNotBlank() }

    val charCount: Int
        get() = content.length

    val estimatedCost: Double
        get() = modelType.inputCostPer1kTokens * (tokenUsage.promptTokens / 1000.0) +
                modelType.outputCostPer1kTokens * (tokenUsage.completionTokens / 1000.0)

    companion object {
        val EMPTY = ModelResponse(
            modelType = ModelType.WEAK,
            content = "",
            tokenUsage = ModelTokenUsage.EMPTY,
            durationMs = 0,
            finishReason = ""
        )

        fun loading(modelType: ModelType) = ModelResponse(
            modelType = modelType,
            content = "",
            tokenUsage = ModelTokenUsage.EMPTY,
            durationMs = 0,
            finishReason = "",
            loading = true
        )

        fun error(modelType: ModelType, message: String) = ModelResponse(
            modelType = modelType,
            content = "",
            tokenUsage = ModelTokenUsage.EMPTY,
            durationMs = 0,
            finishReason = "error",
            error = message
        )
    }
}

/**
 * Token usage statistics for model response
 */
data class ModelTokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
) {
    companion object {
        val EMPTY = ModelTokenUsage(
            promptTokens = 0,
            completionTokens = 0,
            totalTokens = 0
        )
    }
}
