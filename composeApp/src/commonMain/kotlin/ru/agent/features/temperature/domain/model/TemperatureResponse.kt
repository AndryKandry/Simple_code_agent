package ru.agent.features.temperature.domain.model

/**
 * Single response data for a specific temperature level
 */
data class TemperatureResponse(
    val level: TemperatureLevel,
    val content: String,
    val tokenUsage: TokenUsage,
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

    companion object {
        val EMPTY = TemperatureResponse(
            level = TemperatureLevel.LOW,
            content = "",
            tokenUsage = TokenUsage.EMPTY,
            durationMs = 0,
            finishReason = ""
        )

        fun loading(level: TemperatureLevel) = TemperatureResponse(
            level = level,
            content = "",
            tokenUsage = TokenUsage.EMPTY,
            durationMs = 0,
            finishReason = "",
            loading = true
        )

        fun error(level: TemperatureLevel, message: String) = TemperatureResponse(
            level = level,
            content = "",
            tokenUsage = TokenUsage.EMPTY,
            durationMs = 0,
            finishReason = "error",
            error = message
        )
    }
}
