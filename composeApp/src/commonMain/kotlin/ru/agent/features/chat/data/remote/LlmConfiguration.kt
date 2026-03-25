package ru.agent.features.chat.data.remote

/**
 * Configuration for LLM providers.
 *
 * @property provider The LLM provider to use
 * @property ollamaBaseUrl Base URL for Ollama server (default: localhost:11434)
 * @property ollamaModel Model name for Ollama (default: deepseek-r1:8b)
 */
data class LlmConfiguration(
    val provider: LlmProvider = LlmProvider.DEEPSEEK,
    val ollamaBaseUrl: String = OllamaApi.DEFAULT_BASE_URL,
    val ollamaModel: String = OllamaApi.DEFAULT_MODEL
) {
    companion object {
        /**
         * Create configuration from environment variables.
         *
         * Environment variables:
         * - LLM_PROVIDER: Provider name (deepseek, ollama) - default: deepseek
         * - OLLAMA_BASE_URL: Ollama server URL - default: http://localhost:11434
         * - OLLAMA_MODEL: Ollama model name - default: deepseek-r1:8b
         *
         * @return LlmConfiguration with values from environment or defaults
         */
        fun fromEnvironment(): LlmConfiguration {
            val providerName = getEnvVariable("LLM_PROVIDER")
            val ollamaUrl = getEnvVariable("OLLAMA_BASE_URL")
            val ollamaModel = getEnvVariable("OLLAMA_MODEL")

            return LlmConfiguration(
                provider = LlmProvider.fromString(providerName),
                ollamaBaseUrl = ollamaUrl ?: OllamaApi.DEFAULT_BASE_URL,
                ollamaModel = ollamaModel ?: OllamaApi.DEFAULT_MODEL
            )
        }
    }

    /**
     * Check if Ollama configuration is valid.
     */
    fun isOllamaConfigured(): Boolean {
        return provider == LlmProvider.OLLAMA &&
               ollamaBaseUrl.isNotBlank() &&
               ollamaModel.isNotBlank()
    }
}

/**
 * Platform-specific function to get environment variables.
 * Implemented separately for JVM, iOS, and Android.
 */
internal expect fun getEnvVariable(name: String): String?
