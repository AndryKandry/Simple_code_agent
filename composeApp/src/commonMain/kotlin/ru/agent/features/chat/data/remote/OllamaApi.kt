package ru.agent.features.chat.data.remote

/**
 * Ollama API configuration constants.
 *
 * Ollama is a local LLM server that runs on localhost.
 * It provides OpenAI-compatible API endpoints.
 *
 * Documentation: https://github.com/ollama/ollama/blob/main/docs/api.md
 */
object OllamaApi {
    /**
     * Default base URL for Ollama server.
     * Ollama runs on localhost by default.
     */
    const val DEFAULT_BASE_URL = "http://localhost:11434"

    /**
     * Default model to use with Ollama.
     * deepseek-r1:8b is a good balance of quality and performance.
     */
    const val DEFAULT_MODEL = "deepseek-r1:8b"

    /**
     * Chat endpoint path.
     * Full URL: {baseUrl}/api/chat
     */
    const val CHAT_ENDPOINT = "/api/chat"

    /**
     * Request timeout in milliseconds.
     * Ollama can take longer than cloud APIs for local inference.
     */
    const val TIMEOUT = 300_000L // 5 minutes

    /**
     * Default max tokens for generation.
     * Ollama doesn't enforce this strictly, but it's useful for context management.
     */
    const val MAX_TOKENS = 2000

    /**
     * Default temperature for generation.
     */
    const val TEMPERATURE = 0.7
}
