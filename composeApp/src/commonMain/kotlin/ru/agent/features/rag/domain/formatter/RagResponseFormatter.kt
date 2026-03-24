package ru.agent.features.rag.domain.formatter

import ru.agent.features.rag.domain.model.ChunkScore
import ru.agent.features.rag.domain.model.Citation
import ru.agent.features.rag.domain.model.RagResponse
import ru.agent.features.rag.domain.model.SourceInfo

/**
 * Formatter for RAG responses with sources and citations.
 *
 * Provides standardized formatting for RAG responses including:
 * - Source attribution with file paths, line numbers, and similarity scores
 * - Direct citations (quotes) from retrieved chunks
 * - "Don't know" mode when no relevant context is found
 */
object RagResponseFormatter {

    /**
     * Format a complete RAG response as Markdown.
     *
     * @param response The RAG response to format
     * @param includeAnswer Whether to include the answer section (default: true)
     * @param includeCitations Whether to include citations section (default: true)
     * @param maxCitations Maximum number of citations to include (default: 5)
     * @return Formatted Markdown string
     */
    fun format(
        response: RagResponse,
        includeAnswer: Boolean = true,
        includeCitations: Boolean = true,
        maxCitations: Int = 5
    ): String {
        if (!response.hasRelevantContext) {
            return response.answer
        }

        return buildString {
            // Answer section
            if (includeAnswer && response.answer.isNotBlank()) {
                appendLine("## Ответ")
                appendLine()
                appendLine(response.answer)
                appendLine()
            }

            // Sources section
            if (response.sources.isNotEmpty()) {
                appendLine("## Источники")
                appendLine()
                response.sources.forEachIndexed { index, source ->
                    appendLine(formatSource(index + 1, source))
                }
                appendLine()
            }

            // Citations section
            if (includeCitations && response.citations.isNotEmpty()) {
                appendLine("## Цитаты")
                appendLine()
                response.citations.take(maxCitations).forEach { citation ->
                    appendLine(citation.toFormattedString())
                    appendLine()
                }
            }
        }.trimEnd()
    }

    /**
     * Format a single source entry.
     *
     * @param number Source number (1-based)
     * @param source Source information
     * @return Formatted source string
     */
    fun formatSource(number: Int, source: SourceInfo): String {
        val location = source.getLocationString()

        val section = source.section?.let { " [$it]" } ?: ""
        val similarity = (source.similarity * 100).toInt()

        return "$number. $location$section (similarity: $similarity%)"
    }

    /**
     * Create a RagResponse from a list of ChunkScore results.
     *
     * @param chunks Retrieved chunks with similarity scores
     * @param relevanceThreshold Minimum similarity threshold for relevance
     * @param answer The answer generated based on the context (optional)
     * @param maxCitations Maximum number of citations to include
     * @return RagResponse with sources and citations
     */
    fun createResponse(
        chunks: List<ChunkScore>,
        relevanceThreshold: Float,
        answer: String = "",
        maxCitations: Int = 5
    ): RagResponse {
        if (chunks.isEmpty()) {
            return RagResponse.dontKnow("")
        }

        val maxSimilarity = chunks.maxOfOrNull { it.similarity } ?: 0f
        val hasRelevantContext = maxSimilarity >= relevanceThreshold

        if (!hasRelevantContext) {
            return RagResponse.dontKnow("")
        }

        val sources = chunks.map { SourceInfo.fromChunkScore(it) }
        val citations = chunks
            .sortedByDescending { it.similarity }
            .take(maxCitations)
            .map { Citation.fromChunkScore(it) }

        return RagResponse(
            answer = answer,
            sources = sources,
            citations = citations,
            hasRelevantContext = true,
            maxSimilarity = maxSimilarity,
            totalChunksRetrieved = chunks.size
        )
    }

    /**
     * Append sources and citations to an existing answer.
     *
     * @param answer The original answer from the LLM
     * @param chunks Retrieved chunks with similarity scores
     * @param relevanceThreshold Minimum similarity threshold
     * @param maxCitations Maximum number of citations to include
     * @return Formatted answer with sources and citations appended
     */
    fun appendSourcesAndCitations(
        answer: String,
        chunks: List<ChunkScore>,
        relevanceThreshold: Float,
        maxCitations: Int = 5
    ): String {
        if (chunks.isEmpty()) {
            return answer
        }

        val response = createResponse(
            chunks = chunks,
            relevanceThreshold = relevanceThreshold,
            answer = "",
            maxCitations = maxCitations
        )

        if (!response.hasRelevantContext) {
            return answer
        }

        return buildString {
            append(answer)

            // Append sources
            if (response.sources.isNotEmpty()) {
                appendLine()
                appendLine()
                appendLine("## Источники")
                appendLine()
                response.sources.forEachIndexed { index, source ->
                    appendLine(formatSource(index + 1, source))
                }
            }

            // Append citations
            if (response.citations.isNotEmpty()) {
                appendLine()
                appendLine("## Цитаты")
                appendLine()
                response.citations.forEach { citation ->
                    appendLine("> ${citation.text}")
                    appendLine("> (source: ${citation.source})")
                    appendLine()
                }
            }
        }.trimEnd()
    }

    /**
     * Format a short source summary for inline display.
     *
     * @param chunks Retrieved chunks
     * @param maxSources Maximum number of sources to show
     * @return Short summary string
     */
    fun formatShortSummary(chunks: List<ChunkScore>, maxSources: Int = 3): String {
        if (chunks.isEmpty()) return ""

        return chunks
            .take(maxSources)
            .joinToString(", ") { chunk ->
                "${chunk.fileName}:${chunk.startLine}-${chunk.endLine}"
            }
            .let { "Sources: $it" }
    }
}
