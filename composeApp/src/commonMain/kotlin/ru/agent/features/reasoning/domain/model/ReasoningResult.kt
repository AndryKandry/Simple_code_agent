package ru.agent.features.reasoning.domain.model

/**
 * Result of a single reasoning method execution
 */
data class ReasoningResult(
    val method: ReasoningMethod,
    val status: ReasoningStatus,
    val content: String?,
    val durationMs: Long,
    val error: String? = null
) {
    companion object {
        /**
         * Creates an idle result for a method
         */
        fun idle(method: ReasoningMethod) = ReasoningResult(
            method = method,
            status = ReasoningStatus.IDLE,
            content = null,
            durationMs = 0L,
            error = null
        )

        /**
         * Creates a loading result for a method
         */
        fun loading(method: ReasoningMethod) = ReasoningResult(
            method = method,
            status = ReasoningStatus.LOADING,
            content = null,
            durationMs = 0L,
            error = null
        )

        /**
         * Creates a success result
         */
        fun success(
            method: ReasoningMethod,
            content: String,
            durationMs: Long
        ) = ReasoningResult(
            method = method,
            status = ReasoningStatus.SUCCESS,
            content = content,
            durationMs = durationMs,
            error = null
        )

        /**
         * Creates an error result
         */
        fun error(
            method: ReasoningMethod,
            errorMessage: String,
            durationMs: Long = 0L
        ) = ReasoningResult(
            method = method,
            status = ReasoningStatus.ERROR,
            content = null,
            durationMs = durationMs,
            error = errorMessage
        )
    }
}
