package ru.agent.features.chat.data.remote

import co.touchlab.kermit.Logger

/**
 * Factory for creating LLM API clients based on configuration.
 *
 * This factory enables dynamic switching between different LLM providers
 * without changing application code.
 *
 * @property deepSeekClient DeepSeek API client
 * @property ollamaClient Ollama API client
 * @property config LLM configuration
 */
class LlmClientFactory(
    private val deepSeekClient: DeepSeekApiClient,
    private val ollamaClient: OllamaApiClient,
    private val config: LlmConfiguration
) {
    private val logger = Logger.withTag("LlmClientFactory")

    /**
     * Create an LLM API client based on the current configuration.
     *
     * @return LlmApiClient instance
     */
    fun createClient(): LlmApiClient {
        val client = when (config.provider) {
            LlmProvider.DEEPSEEK -> {
                logger.i { "Creating DeepSeek API client" }
                deepSeekClient
            }
            LlmProvider.OLLAMA -> {
                logger.i { "Creating Ollama API client (model: ${config.ollamaModel}, url: ${config.ollamaBaseUrl})" }

                if (!config.isOllamaConfigured()) {
                    logger.w { "Ollama configuration is incomplete" }
                }

                ollamaClient
            }
        }

        logger.i { "Using LLM provider: ${client.getProviderName()}" }
        return client
    }

    /**
     * Create an LLM API client for a specific provider.
     *
     * @param provider LLM provider
     * @return LlmApiClient instance
     */
    fun createClient(provider: LlmProvider): LlmApiClient {
        val configForProvider = when (provider) {
            LlmProvider.DEEPSEEK -> config.copy(provider = LlmProvider.DEEPSEEK)
            LlmProvider.OLLAMA -> config.copy(provider = LlmProvider.OLLAMA)
        }

        return LlmClientFactory(deepSeekClient, ollamaClient, configForProvider).createClient()
    }
}
