package ru.agent.features.rag.domain.model

/**
 * Represents a complete RAG response with sources and citations.
 *
 * Used to structure the response from RAG search with all necessary metadata
 * for transparent source attribution and anti-hallucination features.
 *
 * @property answer The main response content generated based on retrieved context
 * @property sources List of sources used to generate the answer
 * @property citations Direct quotes from the retrieved chunks
 * @property hasRelevantContext Whether relevant context was found (similarity >= threshold)
 * @property maxSimilarity The highest similarity score among retrieved chunks
 * @property totalChunksRetrieved Total number of chunks retrieved from the index
 */
data class RagResponse(
    val answer: String,
    val sources: List<SourceInfo>,
    val citations: List<Citation>,
    val hasRelevantContext: Boolean,
    val maxSimilarity: Float,
    val totalChunksRetrieved: Int
) {
    /**
     * Returns true if no relevant context was found (should trigger "don't know" mode).
     */
    fun shouldRespondWithDontKnow(): Boolean = !hasRelevantContext

    /**
     * Returns a summary of the response for logging.
     */
    fun toSummary(): String {
        return "RagResponse(sources=${sources.size}, citations=${citations.size}, " +
               "hasContext=$hasRelevantContext, maxSimilarity=$maxSimilarity)"
    }

    companion object {
        /**
         * Empty response indicating no relevant context was found.
         */
        val EMPTY = RagResponse(
            answer = "",
            sources = emptyList(),
            citations = emptyList(),
            hasRelevantContext = false,
            maxSimilarity = 0f,
            totalChunksRetrieved = 0
        )

        /**
         * Create a "don't know" response when no relevant context is available.
         */
        fun dontKnow(query: String): RagResponse {
            return RagResponse(
                answer = buildDontKnowResponse(query),
                sources = emptyList(),
                citations = emptyList(),
                hasRelevantContext = false,
                maxSimilarity = 0f,
                totalChunksRetrieved = 0
            )
        }

        private fun buildDontKnowResponse(query: String): String {
            return """
                |## Ответ
                |
                |Не могу найти релевантную информацию в индексированных документах проекта для ответа на ваш вопрос.
                |
                |Пожалуйста, уточните вопрос или переформулируйте его.
                |
                |**Возможные причины:**
                |- Информация не содержится в проиндексированных файлах
                |- Вопрос слишком общий или не связан с кодовой базой
                |- Требуется переиндексация проекта с другими настройками
                |
                |**Рекомендации:**
                |- Используйте команду `/index` для обновления индекса
                |- Уточните, какой аспект проекта вас интересует
                |- Попробуйте использовать ключевые слова из кодовой базы
            """.trimMargin()
        }

        private const val DEFAULT_RELEVANCE_THRESHOLD = 0.5f
        const val DEFAULT_MAX_CITATIONS = 5

        /**
         * Create RagResponse from a list of ChunkScore results.
         *
         * @param chunks List of retrieved chunks with similarity scores
         * @param query Original search query (for context)
         * @param relevanceThreshold Minimum similarity to consider context relevant (default: 0.3)
         * @return RagResponse with sources, citations, and relevance information
         */
        fun fromChunks(
            chunks: List<ChunkScore>,
            query: String,
            relevanceThreshold: Float = DEFAULT_RELEVANCE_THRESHOLD
        ): RagResponse {
            if (chunks.isEmpty()) {
                return dontKnow(query)
            }

            val maxSimilarity = chunks.maxOfOrNull { it.similarity } ?: 0f
            val hasRelevantContext = maxSimilarity >= relevanceThreshold

            // If no relevant context, return "don't know" with empty sources
            if (!hasRelevantContext) {
                return dontKnow(query)
            }

            // Filter sources by relevance threshold - only include relevant chunks
            val relevantChunks = chunks.filter { it.similarity >= relevanceThreshold }
            val sources = relevantChunks.map { SourceInfo.fromChunkScore(it) }

            val citations = relevantChunks
                .take(DEFAULT_MAX_CITATIONS)
                .map { Citation.fromChunkScore(it) }

            return RagResponse(
                answer = "", // Will be filled after LLM generation
                sources = sources,
                citations = citations,
                hasRelevantContext = true,
                maxSimilarity = maxSimilarity,
                totalChunksRetrieved = chunks.size
            )
        }
    }
}

/**
 * Information about a single source used in RAG response.
 *
 * @property chunkId Unique identifier for the chunk
 * @property fileName Name of the source file
 * @property filePath Full path to the source file
 * @property section Optional section name (e.g., class/function name)
 * @property startLine Starting line number in the source file
 * @property endLine Ending line number in the source file
 * @property similarity Cosine similarity score (0-1)
 * @property rank Rank position in search results (1-based)
 * @property language Programming language or document type
 */
data class SourceInfo(
    val chunkId: String,
    val fileName: String,
    val filePath: String,
    val section: String?,
    val startLine: Int,
    val endLine: Int,
    val similarity: Float,
    val rank: Int,
    val language: String = "",
    val content: String = ""
) {
    /**
     * Returns a formatted location string for display.
     */
    fun getLocationString(): String {
        return buildString {
            append(fileName)
            if (startLine > 0) {
                append(":$startLine")
                if (endLine > startLine) {
                    append("-$endLine")
                }
            }
        }
    }

    /**
     * Returns a formatted source reference for citations.
     */
    fun getSourceReference(): String {
        val location = getLocationString()
        val sectionPart = section?.let { " [$it]" } ?: ""
        return "$location$sectionPart"
    }

    /**
     * Returns a preview of the content (truncated).
     */
    fun getContentPreview(maxLength: Int = 200): String {
        return if (content.length > maxLength) {
            content.take(maxLength) + "..."
        } else {
            content
        }
    }

    companion object {
        /**
         * Create SourceInfo from ChunkScore.
         */
        fun fromChunkScore(chunk: ChunkScore): SourceInfo {
            return SourceInfo(
                chunkId = chunk.chunkId,
                fileName = chunk.fileName,
                filePath = chunk.source,
                section = chunk.section,
                startLine = chunk.startLine,
                endLine = chunk.endLine,
                similarity = chunk.similarity,
                rank = chunk.rank,
                language = chunk.language,
                content = chunk.content
            )
        }
    }
}

/**
 * A direct citation (quote) from a retrieved chunk.
 *
 * @property text The quoted text fragment
 * @property source Reference to the source (fileName:lines)
 * @property chunkId ID of the source chunk
 * @property similarity Similarity score of the source chunk
 */
data class Citation(
    val text: String,
    val source: String,
    val chunkId: String,
    val similarity: Float
) {
    /**
     * Returns a formatted citation string.
     */
    fun toFormattedString(): String {
        return "> $text\n> (source: $source)"
    }

    companion object {
        /**
         * Create a citation from a ChunkScore.
         *
         * @param chunk The source chunk
         * @param maxLength Maximum length of the citation text (default: 500)
         */
        fun fromChunkScore(chunk: ChunkScore, maxLength: Int = 500): Citation {
            val text = if (chunk.content.length > maxLength) {
                chunk.content.take(maxLength) + "..."
            } else {
                chunk.content.trim()
            }

            val source = buildString {
                append(chunk.fileName)
                if (chunk.startLine > 0) {
                    append(":${chunk.startLine}")
                    if (chunk.endLine > chunk.startLine) {
                        append("-${chunk.endLine}")
                    }
                }
            }

            return Citation(
                text = text,
                source = source,
                chunkId = chunk.chunkId,
                similarity = chunk.similarity
            )
        }
    }
}
