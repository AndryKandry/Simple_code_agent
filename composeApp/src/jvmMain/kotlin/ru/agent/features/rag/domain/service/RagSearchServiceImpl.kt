package ru.agent.features.rag.domain.service

import co.touchlab.kermit.Logger
import ru.agent.features.rag.data.remote.OllamaEmbeddingClient
import ru.agent.features.rag.domain.model.*
import ru.agent.features.rag.domain.repository.DocumentIndexRepository
import ru.agent.features.rag.domain.repository.EmbeddingRepository

/**
 * JVM implementation of RAG search service.
 *
 * Supports two modes:
 * - BASELINE: Simple cosine similarity search
 * - ENHANCED: Query rewriting + reranking pipeline
 *
 * Uses Ollama for embedding generation and local database for chunk storage.
 */
class RagSearchServiceImpl(
    private val embeddingClient: OllamaEmbeddingClient,
    private val chunkRepository: DocumentIndexRepository,
    private val embeddingRepository: EmbeddingRepository,
    private val config: RagConfig = RagConfig(),
    private val rerankerService: RerankerService? = null,
    private val queryRewriterService: QueryRewriterService? = null,
    private val metricsService: RagMetricsService? = null
) : RagSearchService {

    private val logger = Logger.withTag("RagSearchService")

    override suspend fun search(query: String, config: RagConfig?): List<ChunkScore> {
        val activeConfig = config ?: this.config

        if (query.isBlank()) {
            logger.w { "Empty query provided" }
            return emptyList()
        }

        val pipelineStartTime = System.currentTimeMillis()
        val pipelineMode = activeConfig.getPipelineMode()

        logger.i { "Starting RAG search [${pipelineMode.name}] for query: ${query.take(100)}..." }

        // === STEP 1: Query Rewriting (if enabled) ===
        var currentQuery = query
        var queryRewriteMetrics: QueryRewriteMetrics? = null

        if (activeConfig.enableQueryRewriting && queryRewriterService != null) {
            val rewriteStartTime = System.currentTimeMillis()

            currentQuery = try {
                val rewriteResponse = queryRewriterService.rewrite(
                    QueryRewriteRequest(query = query)
                )

                queryRewriteMetrics = QueryRewriteMetrics(
                    originalQuery = query,
                    rewrittenQuery = rewriteResponse.rewrittenQuery,
                    durationMs = System.currentTimeMillis() - rewriteStartTime,
                    success = true
                )

                logger.i { "Query rewritten: ${rewriteResponse.rewrittenQuery}" }
                rewriteResponse.rewrittenQuery
            } catch (e: Exception) {
                logger.w { "Query rewriting failed, using original query: ${e.message}" }

                queryRewriteMetrics = QueryRewriteMetrics(
                    originalQuery = query,
                    rewrittenQuery = query,
                    durationMs = System.currentTimeMillis() - rewriteStartTime,
                    success = false
                )

                query // Fallback to original
            }
        }

        // === STEP 2: Generate Embedding ===
        val embeddingStartTime = System.currentTimeMillis()
        val queryEmbedding: EmbeddingVector = try {
            embeddingClient.generateEmbedding(currentQuery)
        } catch (e: Exception) {
            logger.e { "Failed to generate query embedding: ${e.message}" }
            return emptyList()
        }

        val embeddingMetrics = EmbeddingMetrics(
            durationMs = System.currentTimeMillis() - embeddingStartTime,
            vectorDimension = queryEmbedding.dimension,
            success = true
        )

        logger.d { "Query embedding generated: ${queryEmbedding.dimension} dimensions in ${embeddingMetrics.durationMs}ms" }

        // === STEP 3: Initial Search (get all candidates) ===
        val searchStartTime = System.currentTimeMillis()

        val allEmbeddings: Map<String, EmbeddingVector> = embeddingRepository.getAllEmbeddings()
        if (allEmbeddings.isEmpty()) {
            logger.w { "No embeddings found in repository" }
            return emptyList()
        }

        val allChunks: List<DocumentChunk> = chunkRepository.getAllChunks()
        if (allChunks.isEmpty()) {
            logger.w { "No chunks found in repository" }
            return emptyList()
        }

        val scoredChunks = calculateSimilarities(
            queryEmbedding = queryEmbedding,
            embeddings = allEmbeddings,
            chunks = allChunks
        )

        val searchMetrics = SearchMetrics(
            durationMs = System.currentTimeMillis() - searchStartTime,
            candidateCount = scoredChunks.size,
            topSimilarityScore = scoredChunks.maxOfOrNull { it.similarity } ?: 0f,
            avgSimilarityScore = if (scoredChunks.isNotEmpty()) scoredChunks.map { it.similarity }.average().toFloat() else 0f
        )

        logger.d { "Initial search: ${scoredChunks.size} candidates in ${searchMetrics.durationMs}ms" }

        // === STEP 4: Filtering ===
        val filterStartTime = System.currentTimeMillis()

        // Take topKBeforeFilter candidates
        val topCandidates = scoredChunks
            .sortedByDescending { it.similarity }
            .take(activeConfig.topKBeforeFilter)

        // Filter by similarity threshold
        val filteredChunks = topCandidates
            .filter { it.similarity >= activeConfig.similarityThreshold }

        val filterMetrics = FilterMetrics(
            durationMs = System.currentTimeMillis() - filterStartTime,
            inputCount = topCandidates.size,
            outputCount = filteredChunks.size,
            threshold = activeConfig.similarityThreshold
        )

        logger.d { "Filtered ${filteredChunks.size} chunks above threshold ${activeConfig.similarityThreshold}" }

        // === STEP 5: Reranking (if enabled) ===
        var rerankMetrics: RerankMetrics? = null
        val finalCandidates: List<ChunkScore> = if (activeConfig.enableReranking && rerankerService != null && filteredChunks.isNotEmpty()) {
            val rerankStartTime = System.currentTimeMillis()

            try {
                val rerankRequest = RerankRequest(
                    query = currentQuery,
                    documents = filteredChunks.map { chunk ->
                        RerankDocument(
                            id = chunk.chunkId,
                            content = chunk.content
                        )
                    }
                )

                val rerankResponse = rerankerService.rerank(rerankRequest)

                // Create reranked results with updated similarity scores
                val rerankedMap = rerankResponse.results.associateBy { it.id }

                val rerankedChunks = filteredChunks.map { chunk ->
                    val rerankResult = rerankedMap[chunk.chunkId]
                    if (rerankResult != null) {
                        chunk.copy(similarity = rerankResult.relevanceScore)
                    } else {
                        chunk
                    }
                }.sortedByDescending { it.similarity }

                rerankMetrics = RerankMetrics(
                    durationMs = System.currentTimeMillis() - rerankStartTime,
                    inputCount = filteredChunks.size,
                    outputCount = rerankedChunks.size,
                    topRelevanceScore = rerankedChunks.maxOfOrNull { it.similarity } ?: 0f,
                    avgRelevanceScore = if (rerankedChunks.isNotEmpty()) rerankedChunks.map { it.similarity }.average().toFloat() else 0f,
                    success = true,
                    fallbackUsed = false
                )

                logger.i { "Reranking completed in ${rerankMetrics.durationMs}ms" }
                rerankedChunks
            } catch (e: Exception) {
                logger.w { "Reranking failed, using cosine similarity fallback: ${e.message}" }

                rerankMetrics = RerankMetrics(
                    durationMs = System.currentTimeMillis() - rerankStartTime,
                    inputCount = filteredChunks.size,
                    outputCount = filteredChunks.size,
                    topRelevanceScore = filteredChunks.maxOfOrNull { it.similarity } ?: 0f,
                    avgRelevanceScore = if (filteredChunks.isNotEmpty()) filteredChunks.map { it.similarity }.average().toFloat() else 0f,
                    success = false,
                    fallbackUsed = true
                )

                filteredChunks // Fallback to cosine similarity
            }
        } else {
            filteredChunks
        }

        // === STEP 6: Final Selection ===
        val results = finalCandidates
            .sortedByDescending { it.similarity }
            .take(activeConfig.topKAfterFilter)
            .mapIndexed { index, chunk -> chunk.copy(rank = index + 1) }

        val totalDuration = System.currentTimeMillis() - pipelineStartTime

        logger.i { "RAG search completed: ${results.size} results in ${totalDuration}ms" }

        if (activeConfig.verbose) {
            results.forEach { chunk ->
                logger.d { "  [${chunk.rank}] ${chunk.fileName}: similarity=${chunk.similarity}" }
            }
        }

        // === STEP 7: Record Metrics ===
        if (activeConfig.enableMetrics && metricsService != null) {
            val metrics = RagMetrics(
                query = query,
                pipelineMode = pipelineMode.name,
                queryRewriteMetrics = queryRewriteMetrics,
                embeddingMetrics = embeddingMetrics,
                searchMetrics = searchMetrics,
                filterMetrics = filterMetrics,
                rerankMetrics = rerankMetrics,
                totalDurationMs = totalDuration,
                resultCount = results.size
            )

            metricsService.recordMetrics(metrics)
        }

        return results
    }

    override suspend fun isAvailable(): Boolean {
        return try {
            val connectionOk = embeddingClient.checkConnection()
            val hasEmbeddings = embeddingRepository.getEmbeddingCount() > 0
            val hasChunks = chunkRepository.getChunkCount() > 0

            logger.d { "RAG availability: connection=$connectionOk, embeddings=$hasEmbeddings, chunks=$hasChunks" }

            connectionOk && hasEmbeddings && hasChunks
        } catch (e: Exception) {
            logger.e { "Failed to check RAG availability: ${e.message}" }
            false
        }
    }

    override suspend fun getStats(): RagStats {
        return try {
            val chunkCount = chunkRepository.getChunkCount()
            val embeddingCount = embeddingRepository.getEmbeddingCount()

            RagStats(
                totalChunks = chunkCount,
                totalEmbeddings = embeddingCount,
                isAvailable = chunkCount > 0 && embeddingCount > 0
            )
        } catch (e: Exception) {
            logger.e { "Failed to get RAG stats: ${e.message}" }
            RagStats(totalChunks = 0, totalEmbeddings = 0, isAvailable = false)
        }
    }

    // === Private helper methods ===

    /**
     * Calculate cosine similarity between query embedding and all chunk embeddings.
     */
    private fun calculateSimilarities(
        queryEmbedding: EmbeddingVector,
        embeddings: Map<String, EmbeddingVector>,
        chunks: List<DocumentChunk>
    ): List<ChunkScore> {
        val chunkMap = chunks.associateBy { it.chunkId }

        return embeddings.mapNotNull { (chunkId, embedding) ->
            val chunk = chunkMap[chunkId] ?: return@mapNotNull null

            val similarity = queryEmbedding.cosineSimilarity(embedding)

            ChunkScore(
                chunkId = chunkId,
                content = chunk.content,
                source = chunk.source,
                fileName = chunk.fileName,
                similarity = similarity,
                rank = 0,
                startLine = chunk.startLine,
                endLine = chunk.endLine,
                language = chunk.language
            )
        }
    }
}
