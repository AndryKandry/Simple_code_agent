package ru.agent.features.rag.data.repository

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.rag.data.local.dao.EmbeddingDao
import ru.agent.features.rag.data.local.entity.EmbeddingEntity
import ru.agent.features.rag.data.remote.OllamaApi
import ru.agent.features.rag.domain.model.EmbeddingVector
import ru.agent.features.rag.domain.repository.EmbeddingRepository

/**
 * Implementation of EmbeddingRepository using EmbeddingDao.
 *
 * @property dao Data access object for embeddings
 * @property json JSON serializer for storing embedding vectors
 */
class EmbeddingRepositoryImpl(
    private val dao: EmbeddingDao,
    private val json: Json = Json
) : EmbeddingRepository {

    /**
     * Generates unique ID for embedding entity.
     */
    private fun generateEmbeddingId(chunkId: String): String {
        return "emb_${chunkId}_${currentTimeMillis()}"
    }

    /**
     * Converts embedding vector to JSON string for storage.
     */
    private fun EmbeddingVector.toJsonString(): String {
        return json.encodeToString(this.values.toList())
    }

    /**
     * Parses JSON string back to embedding vector.
     */
    private fun String.toEmbeddingVector(): EmbeddingVector {
        val values: List<Float> = json.decodeFromString(this)
        return EmbeddingVector(values.toFloatArray())
    }

    override suspend fun saveEmbedding(chunkId: String, embedding: EmbeddingVector) {
        val entity = EmbeddingEntity(
            id = generateEmbeddingId(chunkId),
            chunkId = chunkId,
            embeddingJson = embedding.toJsonString(),
            dimension = embedding.dimension,
            model = OllamaApi.DEFAULT_MODEL,
            createdAt = currentTimeMillis()
        )
        dao.insert(entity)
    }

    override suspend fun getEmbeddingByChunkId(chunkId: String): EmbeddingVector? {
        return dao.getByChunkId(chunkId)?.embeddingJson?.toEmbeddingVector()
    }

    override suspend fun getAllEmbeddings(): Map<String, EmbeddingVector> {
        return dao.getAll().associate { entity ->
            entity.chunkId to entity.embeddingJson.toEmbeddingVector()
        }
    }

    override suspend fun deleteAllEmbeddings() {
        dao.deleteAll()
    }

    override suspend fun getEmbeddingCount(): Int {
        return dao.getCount()
    }
}
