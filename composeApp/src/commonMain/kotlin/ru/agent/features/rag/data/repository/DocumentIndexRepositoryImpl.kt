package ru.agent.features.rag.data.repository

import ru.agent.features.rag.data.local.dao.DocumentChunkDao
import ru.agent.features.rag.data.local.entity.DocumentChunkEntity
import ru.agent.features.rag.domain.model.DocumentChunk
import ru.agent.features.rag.domain.repository.DocumentIndexRepository

/**
 * Implementation of DocumentIndexRepository using DocumentChunkDao.
 *
 * @property dao Data access object for document chunks
 */
class DocumentIndexRepositoryImpl(
    private val dao: DocumentChunkDao
) : DocumentIndexRepository {

    /**
     * Converts domain model to entity.
     */
    private fun DocumentChunk.toEntity(): DocumentChunkEntity = DocumentChunkEntity(
        chunkId = this.chunkId,
        content = this.content,
        source = this.source,
        fileName = this.fileName,
        language = this.language,
        startLine = this.startLine,
        endLine = this.endLine,
        section = this.section,
        tokenCount = this.tokenCount,
        createdAt = System.currentTimeMillis()
    )

    /**
     * Converts entity to domain model.
     */
    private fun DocumentChunkEntity.toDomain(): DocumentChunk = DocumentChunk(
        chunkId = this.chunkId,
        content = this.content,
        source = this.source,
        fileName = this.fileName,
        language = this.language,
        startLine = this.startLine,
        endLine = this.endLine,
        section = this.section,
        tokenCount = this.tokenCount
    )

    override suspend fun saveChunk(chunk: DocumentChunk) {
        dao.insert(chunk.toEntity())
    }

    override suspend fun saveChunks(chunks: List<DocumentChunk>) {
        dao.insertAll(chunks.map { it.toEntity() })
    }

    override suspend fun getChunkById(chunkId: String): DocumentChunk? {
        return dao.getById(chunkId)?.toDomain()
    }

    override suspend fun getChunksBySource(source: String): List<DocumentChunk> {
        return dao.getBySource(source).map { it.toDomain() }
    }

    override suspend fun getAllChunks(): List<DocumentChunk> {
        return dao.getAll().map { it.toDomain() }
    }

    override suspend fun deleteAllChunks() {
        dao.deleteAll()
    }

    override suspend fun deleteChunksBySource(source: String) {
        dao.deleteBySource(source)
    }

    override suspend fun getChunkCount(): Int {
        return dao.getCount()
    }

    override suspend fun getChunkCountByLanguage(): Map<String, Int> {
        return dao.getCountGroupByLanguage()
            .associate { it.language to it.count }
    }
}
