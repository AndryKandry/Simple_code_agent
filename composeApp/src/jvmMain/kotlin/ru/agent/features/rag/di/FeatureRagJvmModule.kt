package ru.agent.features.rag.di

import org.koin.dsl.module
import ru.agent.features.rag.chunker.FixedSizeChunker
import ru.agent.features.rag.chunker.StructureBasedChunker
import ru.agent.features.rag.chunker.TextChunker
import ru.agent.features.rag.collector.FileContentReader
import ru.agent.features.rag.collector.GitFilesCollector
import ru.agent.features.rag.data.remote.OllamaApi
import ru.agent.features.rag.data.remote.OllamaEmbeddingClient
import ru.agent.features.rag.data.remote.OllamaQueryRewriterClient
import ru.agent.features.rag.data.remote.OllamaRerankerClient
import ru.agent.features.rag.domain.model.IndexingStrategy
import ru.agent.features.rag.domain.model.RagConfig
import ru.agent.features.rag.domain.repository.DocumentIndexRepository
import ru.agent.features.rag.domain.repository.EmbeddingRepository
import ru.agent.features.rag.domain.repository.IndexMetadataRepository
import ru.agent.features.rag.domain.service.RagMetricsService
import ru.agent.features.rag.domain.service.RagMetricsServiceImpl
import ru.agent.features.rag.domain.service.RagSearchService
import ru.agent.features.rag.domain.service.RagSearchServiceImpl
import ru.agent.features.rag.domain.service.RerankerService
import ru.agent.features.rag.domain.service.QueryRewriterService
import ru.agent.features.rag.pipeline.IndexStatsCalculator
import ru.agent.features.rag.pipeline.IndexingPipeline
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Koin module for RAG feature - JVM-specific components.
 *
 * Provides:
 * - OllamaEmbeddingClient for embedding generation
 * - OllamaRerankerClient for document reranking (hybrid: LLM + embeddings)
 * - OllamaQueryRewriterClient for query rewriting
 * - RagMetricsService for metrics collection
 * - Chunkers (FixedSizeChunker, StructureBasedChunker)
 * - GitFilesCollector for collecting git-tracked files
 * - FileContentReader for reading file contents
 * - IndexStatsCalculator for calculating indexing statistics
 * - IndexingPipeline for coordinating the indexing process
 */
val featureRagJvmModule = module {

    // === Ollama Clients ===
    single { OllamaEmbeddingClient() }
    single {
        OllamaRerankerClient(
            embeddingModel = OllamaApi.DEFAULT_MODEL, // bge-m3 for vector compatibility
            rerankerModel = "qwen2.5-coder:3b-instruct", // LLM for cross-encoder style
            useLlmReranking = true // Enable LLM-based reranking
        )
    }
    single { OllamaQueryRewriterClient() }

    // === Metrics Service ===
    single<RagMetricsService> { RagMetricsServiceImpl() }

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

    // === RAG Search Service ===
    // Default to BASELINE mode for backward compatibility
    // Users must explicitly enable ENHANCED mode if they have the required models:
    // - bge-reranker:latest for reranking
    // - deepseek-r1:1.5b for query rewriting
    single<RagSearchService> {
        RagSearchServiceImpl(
            embeddingClient = get(),
            chunkRepository = get<DocumentIndexRepository>(),
            embeddingRepository = get<EmbeddingRepository>(),
            config = RagConfig.BASELINE, // Default to baseline for backward compatibility
            rerankerService = get<OllamaRerankerClient>(),
            queryRewriterService = get<OllamaQueryRewriterClient>(),
            metricsService = get()
        )
    }
}
