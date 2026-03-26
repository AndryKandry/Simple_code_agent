package ru.agent.features.rag.data.local.mapper

import ru.agent.features.rag.data.local.entity.DocumentChunkEntity
import ru.agent.features.rag.data.local.entity.EmbeddingEntity
import ru.agent.features.rag.data.local.entity.IndexMetadataEntity
import ru.agent.features.rag.domain.model.DocumentChunk
import ru.agent.features.rag.domain.model.Embedding
import ru.agent.features.rag.domain.model.EmbeddingVector
import ru.agent.features.rag.domain.model.IndexMetadata

/**
 * Mapper for converting between Entity (data layer) and Domain models in RAG system.
 */
object RagMapper {

    // === Document Chunk ===

    /**
     * Convert DocumentChunkEntity to DocumentChunk domain model.
     */
    fun DocumentChunkEntity.toDomain(): DocumentChunk {
        return DocumentChunk(
            chunkId = chunkId,
            content = content,
            source = source,
            fileName = fileName,
            language = language,
            startLine = startLine,
            endLine = endLine,
            section = section,
            tokenCount = tokenCount
        )
    }

    /**
     * Convert DocumentChunk domain model to DocumentChunkEntity.
     */
    fun DocumentChunk.toEntity(currentTimeMillis: Long): DocumentChunkEntity {
        return DocumentChunkEntity(
            chunkId = chunkId,
            content = content,
            source = source,
            fileName = fileName,
            language = language,
            startLine = startLine,
            endLine = endLine,
            section = section,
            tokenCount = tokenCount,
            createdAt = currentTimeMillis
        )
    }

    /**
     * Convert list of DocumentChunkEntity to list of DocumentChunk.
     */
    fun List<DocumentChunkEntity>.toChunkDomain(): List<DocumentChunk> {
        return this.map { it.toDomain() }
    }

    /**
     * Convert list of DocumentChunk to list of DocumentChunkEntity.
     */
    fun List<DocumentChunk>.toChunkEntity(currentTimeMillis: Long): List<DocumentChunkEntity> {
        return this.map { it.toEntity(currentTimeMillis) }
    }

    // === Embedding ===

    /**
     * Convert EmbeddingEntity to Embedding domain model.
     */
    fun EmbeddingEntity.toDomain(): Embedding {
        return Embedding(
            id = id,
            chunkId = chunkId,
            embeddingVector = embeddingJson.toEmbeddingVector(),
            model = model,
            createdAt = createdAt
        )
    }

    /**
     * Convert Embedding domain model to EmbeddingEntity.
     */
    fun Embedding.toEntity(): EmbeddingEntity {
        return EmbeddingEntity(
            id = id,
            chunkId = chunkId,
            embeddingJson = embeddingVector.toJson(),
            dimension = embeddingVector.dimension,
            model = model,
            createdAt = createdAt
        )
    }

    /**
     * Convert list of EmbeddingEntity to list of Embedding.
     */
    fun List<EmbeddingEntity>.toEmbeddingDomain(): List<Embedding> {
        return this.map { it.toDomain() }
    }

    /**
     * Convert list of Embedding to list of EmbeddingEntity.
     */
    fun List<Embedding>.toEmbeddingEntity(): List<EmbeddingEntity> {
        return this.map { it.toEntity() }
    }

    // === Index Metadata ===

    /**
     * Convert IndexMetadataEntity to IndexMetadata domain model.
     */
    fun IndexMetadataEntity.toDomain(): IndexMetadata {
        return IndexMetadata(
            strategy = strategy,
            totalChunks = totalChunks,
            totalFiles = totalFiles,
            avgTokensPerChunk = avgTokensPerChunk,
            minTokens = minTokens,
            maxTokens = maxTokens,
            stdDevTokens = stdDevTokens,
            durationMs = durationMs,
            indexedAt = indexedAt,
            model = model
        )
    }

    /**
     * Convert IndexMetadata domain model to IndexMetadataEntity.
     */
    fun IndexMetadata.toEntity(): IndexMetadataEntity {
        return IndexMetadataEntity(
            id = "current",
            strategy = strategy,
            totalChunks = totalChunks,
            totalFiles = totalFiles,
            avgTokensPerChunk = avgTokensPerChunk,
            minTokens = minTokens,
            maxTokens = maxTokens,
            stdDevTokens = stdDevTokens,
            durationMs = durationMs,
            indexedAt = indexedAt,
            model = model
        )
    }

    // === Helper methods for JSON serialization ===

    /**
     * Parses JSON array of numbers to FloatArray (unified with EmbeddingVector).
     */
    private fun String.toEmbeddingVector(): EmbeddingVector {
        if (isBlank()) return EmbeddingVector(FloatArray(0))

        val values = removeSurrounding("[", "]")
            .split(",")
            .mapNotNull { part ->
                part.trim().toFloatOrNull()
            }
            .toFloatArray()
        return EmbeddingVector(values)
    }

    /**
     * Serializes FloatArray to JSON array (unified with EmbeddingVector).
     */
    private fun EmbeddingVector.toJson(): String {
        return "[" + values.joinToString(",") { it.toString() } + "]"
    }
}
