package ru.agent.features.rag.di

import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.agent.core.database.AppDatabase
import ru.agent.features.rag.data.local.dao.DocumentChunkDao
import ru.agent.features.rag.data.local.dao.EmbeddingDao
import ru.agent.features.rag.data.local.dao.IndexMetadataDao
import ru.agent.features.rag.data.repository.DocumentIndexRepositoryImpl
import ru.agent.features.rag.data.repository.EmbeddingRepositoryImpl
import ru.agent.features.rag.data.repository.IndexMetadataRepositoryImpl
import ru.agent.features.rag.domain.repository.DocumentIndexRepository
import ru.agent.features.rag.domain.repository.EmbeddingRepository
import ru.agent.features.rag.domain.repository.IndexMetadataRepository

/**
 * Koin module for RAG feature - platform-independent components.
 *
 * Provides:
 * - Json serializer for embedding storage
 * - DAOs from AppDatabase (DocumentChunkDao, EmbeddingDao, IndexMetadataDao)
 * - Repositories (DocumentIndexRepository, EmbeddingRepository, IndexMetadataRepository)
 */
val featureRagModule = module {

    // === JSON Serializer ===
    single<Json> {
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }

    // === DAOs from AppDatabase ===
    single<DocumentChunkDao> { get<AppDatabase>().getDocumentChunkDao() }
    single<EmbeddingDao> { get<AppDatabase>().getEmbeddingDao() }
    single<IndexMetadataDao> { get<AppDatabase>().getIndexMetadataDao() }

    // === Repositories ===
    singleOf(::DocumentIndexRepositoryImpl) bind DocumentIndexRepository::class
    singleOf(::EmbeddingRepositoryImpl) bind EmbeddingRepository::class
    singleOf(::IndexMetadataRepositoryImpl) bind IndexMetadataRepository::class
}
