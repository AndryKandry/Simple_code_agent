package ru.agent.features.rag.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request to generate embedding from Ollama API.
 *
 * @property model Name of the model to use for embedding generation
 * @property prompt Text to generate embedding for
 */
@Serializable
data class EmbeddingRequest(
    val model: String,
    val prompt: String
)

/**
 * Response from Ollama embeddings endpoint.
 *
 * @property embedding The embedding vector as list of floats
 */
@Serializable
data class EmbeddingResponse(
    val embedding: List<Float>
)

/**
 * Response from Ollama tags endpoint listing available models.
 *
 * @property models List of available models
 */
@Serializable
data class OllamaTagsResponse(
    val models: List<OllamaModel>
)

/**
 * Information about an Ollama model.
 *
 * @property name Model name
 * @property modifiedAt Last modification timestamp
 * @property size Model size in bytes
 */
@Serializable
data class OllamaModel(
    val name: String,
    @SerialName("modified_at")
    val modifiedAt: String? = null,
    val size: Long? = null
)
