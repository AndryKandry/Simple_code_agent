package ru.agent.features.rag.data.remote

/**
 * API configuration constants for Ollama server.
 */
object OllamaApi {
    /**
     * Base URL for Ollama server.
     */
    const val BASE_URL = "http://localhost:11434"

    /**
     * Endpoint for generating embeddings.
     */
    const val EMBEDDINGS_ENDPOINT = "/api/embeddings"

    /**
     * Endpoint for listing available models.
     */
    const val TAGS_ENDPOINT = "/api/tags"

    /**
     * Default embedding model.
     */
    const val DEFAULT_MODEL = "bge-m3:latest"

    /**
     * Default embedding dimension.
     */
    const val DEFAULT_DIMENSION = 1024

    /**
     * Request timeout in milliseconds.
     */
    const val TIMEOUT_MS = 60000L
}
