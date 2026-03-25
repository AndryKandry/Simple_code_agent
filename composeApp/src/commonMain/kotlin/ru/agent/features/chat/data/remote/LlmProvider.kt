package ru.agent.features.chat.data.remote

/**
 * Enum representing available LLM providers.
 *
 * @property DEEPSEEK Cloud-based DeepSeek API
 * @property OLLAMA Local Ollama server
 */
enum class LlmProvider {
    DEEPSEEK,
    OLLAMA;

    /**
     * Get the lowercase name of the provider.
     */
    val lowercase: String
        get() = name.lowercase()

    companion object {
        /**
         * Parse provider from string (case-insensitive).
         *
         * @param value Provider name as string
         * @return LlmProvider or null if invalid
         */
        fun fromString(value: String?): LlmProvider {
            return when (value?.lowercase()) {
                "deepseek" -> DEEPSEEK
                "ollama" -> OLLAMA
                else -> DEEPSEEK  // Default to DeepSeek
            }
        }

        /**
         * Get list of valid provider names.
         */
        val validProviders: List<String>
            get() = entries.map { it.lowercase }
    }
}
