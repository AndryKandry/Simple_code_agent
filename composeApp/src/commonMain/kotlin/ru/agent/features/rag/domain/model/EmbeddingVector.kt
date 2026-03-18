package ru.agent.features.rag.domain.model

import kotlin.math.sqrt

/**
 * Represents an embedding vector for semantic similarity search.
 *
 * @property values Float array containing the embedding values
 */
data class EmbeddingVector(
    val values: FloatArray
) {
    /**
     * Dimension of the embedding vector.
     */
    val dimension: Int
        get() = values.size

    /**
     * Calculates cosine similarity between this vector and another.
     *
     * @param other The other embedding vector to compare with
     * @return Cosine similarity value between -1 and 1
     * @throws IllegalArgumentException if vector dimensions don't match
     */
    fun cosineSimilarity(other: EmbeddingVector): Float {
        require(values.size == other.values.size) { "Vector dimensions must match" }

        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        values.indices.forEach { i ->
            dotProduct += values[i] * other.values[i]
            normA += values[i] * values[i]
            normB += other.values[i] * other.values[i]
        }

        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator > 0) dotProduct / denominator else 0f
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EmbeddingVector) return false
        return values.contentEquals(other.values)
    }

    override fun hashCode(): Int = values.contentHashCode()
}
