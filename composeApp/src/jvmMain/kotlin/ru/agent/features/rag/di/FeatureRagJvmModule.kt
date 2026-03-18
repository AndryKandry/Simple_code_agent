package ru.agent.features.rag.di

import org.koin.dsl.module
import ru.agent.features.rag.chunker.FixedSizeChunker
import ru.agent.features.rag.chunker.StructureBasedChunker
import ru.agent.features.rag.chunker.TextChunker
import ru.agent.features.rag.collector.FileContentReader
import ru.agent.features.rag.collector.GitFilesCollector
import ru.agent.features.rag.data.remote.OllamaEmbeddingClient
import ru.agent.features.rag.domain.model.IndexingStrategy
import ru.agent.features.rag.domain.repository.DocumentIndexRepository
import ru.agent.features.rag.domain.repository.EmbeddingRepository
import ru.agent.features.rag.domain.repository.IndexMetadataRepository
import ru.agent.features.rag.pipeline.IndexStatsCalculator
import ru.agent.features.rag.pipeline.IndexingPipeline
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Koin module for RAG feature - JVM-specific components.
 *
 * Provides:
 * - OllamaEmbeddingClient for embedding generation
 * - Chunkers (FixedSizeChunker, StructureBasedChunker)
 * - GitFilesCollector for collecting git-tracked files
 * - FileContentReader for reading file contents
 * - IndexStatsCalculator for calculating indexing statistics
 * - IndexingPipeline for coordinating the indexing process
 */
val featureRagJvmModule = module {

    // === Ollama Client ===
    single { OllamaEmbeddingClient() }

    // === Chunkers ===
    single { FixedSizeChunker() }
    single { StructureBasedChunker() }

    // Factory for chunker selection based on strategy
    factory { (strategy: IndexingStrategy) ->
        when (strategy) {
            IndexingStrategy.FIXED_SIZE -> get<FixedSizeChunker>()
            IndexingStrategy.STRUCTURE_BASED -> get<StructureBasedChunker>()
        } as TextChunker
    }

    // === Git Files Collector ===
    single { GitFilesCollector(Path(System.getProperty("user.dir"))) }

    // === File Content Reader ===
    single { FileContentReader() }

    // === Stats Calculator ===
    single { IndexStatsCalculator() }

    // === Indexing Pipeline ===
    single {
        IndexingPipeline(
            filesCollector = get(),
            contentReader = get(),
            embeddingClient = get(),
            chunkRepository = get<DocumentIndexRepository>(),
            embeddingRepository = get<EmbeddingRepository>(),
            metadataRepository = get<IndexMetadataRepository>(),
            statsCalculator = get()
        )
    }
}
