package ru.agent.features.rag.pipeline

import ru.agent.features.rag.domain.model.DocumentChunk
import ru.agent.features.rag.domain.model.IndexStats
import ru.agent.features.rag.domain.model.IndexingStrategy
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Calculates statistics for the document indexing process.
 */
class IndexStatsCalculator {

    /**
     * Calculates index statistics from the indexing results.
     *
     * @param strategy The indexing strategy used
     * @param chunks List of document chunks created
     * @param totalFiles Total number of files processed
     * @param duration Duration of indexing in milliseconds
     * @param model The embedding model used
     * @return IndexStats with calculated statistics
     */
    fun calculate(
        strategy: IndexingStrategy,
        chunks: List<DocumentChunk>,
        totalFiles: Int,
        duration: Long,
        model: String = DEFAULT_MODEL
    ): IndexStats {
        if (chunks.isEmpty()) {
            return IndexStats(
                strategy = strategy,
                totalChunks = 0,
                totalFiles = totalFiles,
                avgTokensPerChunk = 0.0,
                minTokens = 0,
                maxTokens = 0,
                stdDevTokens = 0.0,
                durationMs = duration,
                indexedAt = System.currentTimeMillis(),
                model = model
            )
        }

        val tokenCounts = chunks.map { it.tokenCount }
        val avgTokens = tokenCounts.average()
        val minTokens = tokenCounts.minOrNull() ?: 0
        val maxTokens = tokenCounts.maxOrNull() ?: 0

        // Calculate standard deviation
        val stdDevTokens = if (tokenCounts.size > 1) {
            val variance = tokenCounts.map { (it - avgTokens).pow(2) }.average()
            sqrt(variance)
        } else {
            0.0
        }

        return IndexStats(
            strategy = strategy,
            totalChunks = chunks.size,
            totalFiles = totalFiles,
            avgTokensPerChunk = avgTokens,
            minTokens = minTokens,
            maxTokens = maxTokens,
            stdDevTokens = stdDevTokens,
            durationMs = duration,
            indexedAt = System.currentTimeMillis(),
            model = model
        )
    }

    companion object {
        const val DEFAULT_MODEL = "bge-m3:latest"
    }
}
