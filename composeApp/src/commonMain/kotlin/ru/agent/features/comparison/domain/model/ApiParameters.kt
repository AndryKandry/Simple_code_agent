package ru.agent.features.comparison.domain.model

/**
 * Parameters for API request configuration
 */
data class ApiParameters(
    val maxTokens: Int = 2000,
    val temperature: Double = 0.7,
    val topP: Double? = null,
    val stop: List<String>? = null,
    val frequencyPenalty: Double? = null,
    val presencePenalty: Double? = null,
    val systemPrompt: String? = null
) {
    companion object {
        /**
         * Preset for unrestricted API responses
         * - Higher token limit (2000)
         * - Higher temperature for creative responses
         * - No stop sequences
         * - No system prompt
         */
        val UNRESTRICTED = ApiParameters(
            maxTokens = 2000,
            temperature = 0.7,
            topP = null,
            stop = null,
            systemPrompt = null
        )

        /**
         * Preset for restricted API responses
         * - Lower token limit (500)
         * - Lower temperature for focused responses
         * - Stop sequences to control output
         * - System prompt for format guidance
         */
        val RESTRICTED = ApiParameters(
            maxTokens = 500,
            temperature = 0.3,
            topP = 0.9,
            stop = listOf("\n\n", "---", "DONE"),
            systemPrompt = """
                Отвечай кратко, в формате: 1-2 предложения.
                Не используй Markdown.
                Заверши ответ словом 'ГОТОВО'.
            """.trimIndent()
        )
    }
}
