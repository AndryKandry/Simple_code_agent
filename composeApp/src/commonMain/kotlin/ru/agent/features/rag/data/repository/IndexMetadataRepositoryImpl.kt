package ru.agent.features.rag.data.repository

import ru.agent.features.rag.data.local.dao.IndexMetadataDao
import ru.agent.features.rag.data.local.entity.IndexMetadataEntity
import ru.agent.features.rag.domain.model.IndexStats
import ru.agent.features.rag.domain.repository.IndexMetadataRepository

/**
 * Implementation of IndexMetadataRepository using IndexMetadataDao.
 *
 * @property dao Data access object for index metadata
 */
class IndexMetadataRepositoryImpl(
    private val dao: IndexMetadataDao
) : IndexMetadataRepository {

    /**
     * Converts domain IndexStats to entity.
     */
    private fun IndexStats.toEntity(): IndexMetadataEntity = IndexMetadataEntity(
        id = "current",
        strategy = this.strategy.name,
        totalChunks = this.totalChunks,
        totalFiles = this.totalFiles,
        avgTokensPerChunk = this.avgTokensPerChunk,
        minTokens = this.minTokens,
        maxTokens = this.maxTokens,
        stdDevTokens = this.stdDevTokens,
        durationMs = this.durationMs,
        indexedAt = this.indexedAt,
        model = this.model
    )

    /**
     * Converts entity to domain IndexStats.
     */
    private fun IndexMetadataEntity.toDomain(): IndexStats = IndexStats(
        strategy = ru.agent.features.rag.domain.model.IndexingStrategy.valueOf(this.strategy),
        totalChunks = this.totalChunks,
        totalFiles = this.totalFiles,
        avgTokensPerChunk = this.avgTokensPerChunk,
        minTokens = this.minTokens,
        maxTokens = this.maxTokens,
        stdDevTokens = this.stdDevTokens,
        durationMs = this.durationMs,
        indexedAt = this.indexedAt,
        model = this.model
    )

    override suspend fun saveStats(stats: IndexStats) {
        dao.insert(stats.toEntity())
    }

    override suspend fun getStats(): IndexStats? {
        return dao.getCurrent()?.toDomain()
    }

    override suspend fun hasIndex(): Boolean {
        return dao.exists()
    }

    override suspend fun clear() {
        dao.deleteAll()
    }

    override suspend fun getLastIndexedAt(): Long? {
        return dao.getCurrent()?.indexedAt
    }
}
