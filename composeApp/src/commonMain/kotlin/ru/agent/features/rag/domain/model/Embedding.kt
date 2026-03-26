package ru.agent.features.rag.domain.model

/**
 * Domain model for a document chunk embedding.
 *
 * @property id Unique identifier for the embedding
 * @property chunkId ID of the associated document chunk
 * @property embeddingVector The embedding vector
 * @property model Name of the embedding model used
 * @property createdAt Timestamp when the embedding was created
 */
data class Embedding(
    val id: String,
    val chunkId: String,
    val embeddingVector: EmbeddingVector,
    val model: String,
    val createdAt: Long
)
