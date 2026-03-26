package ru.agent.features.rag.pipeline

import ru.agent.features.rag.chunker.ChunkMetadata
import ru.agent.features.rag.chunker.FixedSizeChunker
import ru.agent.features.rag.chunker.StructureBasedChunker
import ru.agent.features.rag.chunker.TextChunker
import ru.agent.features.rag.collector.FileContentReader
import ru.agent.features.rag.collector.GitFilesCollector
import ru.agent.features.rag.data.remote.OllamaEmbeddingClient
import ru.agent.features.rag.data.remote.OllamaException
import ru.agent.features.rag.domain.model.DocumentChunk
import ru.agent.features.rag.domain.model.EmbeddingVector
import ru.agent.features.rag.domain.model.IndexingResult
import ru.agent.features.rag.domain.model.IndexingStrategy
import ru.agent.features.rag.domain.repository.DocumentIndexRepository
import ru.agent.features.rag.domain.repository.EmbeddingRepository
import ru.agent.features.rag.domain.repository.IndexMetadataRepository
import java.nio.file.Path

/**
 * Main pipeline for indexing project files into the RAG system.
 *
 * Coordinates file collection, chunking, embedding generation, and storage.
 *
 * @property filesCollector Collector for git-tracked files
 * @property contentReader Reader for file contents
 * @property embeddingClient Client for generating embeddings via Ollama
 * @property chunkRepository Repository for storing document chunks
 * @property embeddingRepository Repository for storing embedding vectors
 * @property metadataRepository Repository for storing index metadata
 */
class IndexingPipeline(
    private val filesCollector: GitFilesCollector,
    private val contentReader: FileContentReader,
    private val embeddingClient: OllamaEmbeddingClient,
    private val chunkRepository: DocumentIndexRepository,
    private val embeddingRepository: EmbeddingRepository,
    private val metadataRepository: IndexMetadataRepository,
    private val statsCalculator: IndexStatsCalculator = IndexStatsCalculator()
) {

    /**
     * Indexes the project using the specified strategy.
     *
     * @param strategy Chunking strategy to use
     * @param rebuild If true, clears existing index before rebuilding
     * @param onProgress Callback for progress updates
     * @return Result of the indexing operation
     */
    suspend fun indexProject(
        strategy: IndexingStrategy,
        rebuild: Boolean = false,
        onProgress: (IndexingProgress) -> Unit = {}
    ): IndexingResult {
        val startTime = System.currentTimeMillis()

        return try {
            // Step 1: Check Ollama connectivity
            onProgress(IndexingProgress.CheckingOllama)
            if (!embeddingClient.checkConnection()) {
                return IndexingResult.Error("Cannot connect to Ollama server. Please ensure Ollama is running.")
            }

            // Step 2: Clear existing index if rebuild requested
            if (rebuild) {
                onProgress(IndexingProgress.ClearingIndex)
                chunkRepository.deleteAllChunks()
                embeddingRepository.deleteAllEmbeddings()
                metadataRepository.clear()
            }

            // Step 3: Collect files
            onProgress(IndexingProgress.CollectingFiles)
            val files = filesCollector.collectFiles()

            if (files.isEmpty()) {
                return IndexingResult.Error("No files found to index.")
            }

            // Step 4: Create chunker based on strategy
            val chunker = createChunker(strategy)

            // Step 5: Process files and create chunks
            val allChunks = mutableListOf<DocumentChunk>()
            val contents = contentReader.readFiles(files.map { it.path })

            files.forEachIndexed { index, fileInfo ->
                onProgress(
                    IndexingProgress.ProcessingFile(
                        path = fileInfo.relativePath,
                        current = index + 1,
                        total = files.size
                    )
                )

                val content = contents[fileInfo.path]
                if (!content.isNullOrBlank()) {
                    val metadata = ChunkMetadata(
                        source = fileInfo.relativePath,
                        fileName = fileInfo.relativePath.substringAfterLast("/"),
                        language = fileInfo.language
                    )
                    val chunks = chunker.chunk(content, metadata)
                    allChunks.addAll(chunks)
                }
            }

            if (allChunks.isEmpty()) {
                return IndexingResult.Error("No chunks created from files.")
            }

            // Step 6: Save chunks to database
            onProgress(IndexingProgress.SavingChunks(allChunks.size))
            chunkRepository.saveChunks(allChunks)

            // Step 7: Generate and save embeddings
            allChunks.forEachIndexed { index, chunk ->
                onProgress(
                    IndexingProgress.GeneratingEmbeddings(
                        chunkIndex = index + 1,
                        total = allChunks.size
                    )
                )

                try {
                    val embedding = embeddingClient.generateEmbedding(chunk.content)
                    embeddingRepository.saveEmbedding(chunk.chunkId, embedding)
                } catch (e: OllamaException) {
                    // Log error but continue with other chunks
                    println("Warning: Failed to generate embedding for chunk ${chunk.chunkId}: ${e.message}")
                }
            }

            // Step 8: Calculate and save statistics
            val duration = System.currentTimeMillis() - startTime
            val stats = statsCalculator.calculate(
                strategy = strategy,
                chunks = allChunks,
                totalFiles = files.size,
                duration = duration
            )
            metadataRepository.saveStats(stats)

            // Step 9: Report completion
            onProgress(IndexingProgress.Completed(stats))

            IndexingResult.Success(
                totalFiles = files.size,
                totalChunks = allChunks.size,
                duration = duration,
                stats = stats
            )
        } catch (e: Exception) {
            onProgress(IndexingProgress.Error(e.message ?: "Unknown error"))
            IndexingResult.Error(e.message ?: "Unknown error occurred during indexing")
        }
    }

    /**
     * Creates a chunker based on the specified strategy.
     */
    private fun createChunker(strategy: IndexingStrategy): TextChunker {
        return when (strategy) {
            IndexingStrategy.FIXED_SIZE -> FixedSizeChunker()
            IndexingStrategy.STRUCTURE_BASED -> StructureBasedChunker()
        }
    }

    /**
     * Returns the current index statistics.
     *
     * @return Current index stats, or null if no index exists
     */
    suspend fun getIndexStats(): ru.agent.features.rag.domain.model.IndexStats? {
        return metadataRepository.getStats()
    }

    /**
     * Checks if an index exists.
     *
     * @return true if an index exists
     */
    suspend fun hasIndex(): Boolean {
        return metadataRepository.hasIndex()
    }

    /**
     * Clears the entire index.
     */
    suspend fun clearIndex() {
        chunkRepository.deleteAllChunks()
        embeddingRepository.deleteAllEmbeddings()
        metadataRepository.clear()
    }

    companion object {
        /**
         * Factory method to create an IndexingPipeline for a project path.
         */
        fun create(
            projectPath: Path,
            embeddingClient: OllamaEmbeddingClient,
            chunkRepository: DocumentIndexRepository,
            embeddingRepository: EmbeddingRepository,
            metadataRepository: IndexMetadataRepository
        ): IndexingPipeline {
            return IndexingPipeline(
                filesCollector = GitFilesCollector(projectPath),
                contentReader = FileContentReader(),
                embeddingClient = embeddingClient,
                chunkRepository = chunkRepository,
                embeddingRepository = embeddingRepository,
                metadataRepository = metadataRepository
            )
        }
    }
}
