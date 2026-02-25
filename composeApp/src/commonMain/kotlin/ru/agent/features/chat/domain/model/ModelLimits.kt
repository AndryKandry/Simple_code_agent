package ru.agent.features.chat.domain.model

/**
 * Model context limits configuration.
 *
 * Contains token limits for different LLM models supported by the application.
 */
object ModelLimits {

    /**
     * DeepSeek V3 context window size.
     * Reference: https://api-docs.deepseek.com/
     */
    const val DEEPSEEK_V3_CONTEXT = 64_000

    /**
     * Current active model context limit.
     * Default is DeepSeek V3.
     */
    const val DEFAULT_CONTEXT_LIMIT = DEEPSEEK_V3_CONTEXT

    /**
     * Warning threshold percentage (yellow indicator).
     */
    const val WARNING_THRESHOLD_PERCENT = 80

    /**
     * Critical threshold percentage (red indicator).
     */
    const val CRITICAL_THRESHOLD_PERCENT = 95

    /**
     * Token thresholds for quick reference.
     */
    object Thresholds {
        /**
         * Token count for 80% of DeepSeek V3 context.
         */
        val WarningTokens: Int
            get() = (DEEPSEEK_V3_CONTEXT * WARNING_THRESHOLD_PERCENT / 100)

        /**
         * Token count for 95% of DeepSeek V3 context.
         */
        val CriticalTokens: Int
            get() = (DEEPSEEK_V3_CONTEXT * CRITICAL_THRESHOLD_PERCENT / 100)
    }

    /**
     * Get usage level based on token count.
     *
     * @param tokens Current token count
     * @param maxTokens Maximum tokens for the model
     * @return UsageLevel enum
     */
    fun getUsageLevel(tokens: Int, maxTokens: Int = DEFAULT_CONTEXT_LIMIT): UsageLevel {
        val percentage = if (maxTokens > 0) {
            (tokens.toFloat() / maxTokens) * 100f
        } else {
            0f
        }

        return when {
            percentage < WARNING_THRESHOLD_PERCENT -> UsageLevel.Normal
            percentage < CRITICAL_THRESHOLD_PERCENT -> UsageLevel.Warning
            else -> UsageLevel.Critical
        }
    }

    /**
     * Format token count for display (e.g., "1.2K", "64K").
     */
    fun formatTokenCount(tokens: Int): String {
        return when {
            tokens >= 1_000_000 -> "%.1fM".format(tokens / 1_000_000.0)
            tokens >= 1_000 -> "%.1fK".format(tokens / 1_000.0)
            else -> tokens.toString()
        }
    }
}

/**
 * Token usage level.
 */
enum class UsageLevel {
    Normal,    // 0-79% - green
    Warning,   // 80-94% - yellow
    Critical   // 95%+ - red
}
