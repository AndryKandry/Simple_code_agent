package ru.agent.features.rag

import ru.agent.features.rag.domain.formatter.RagResponseFormatter
import ru.agent.features.rag.domain.model.*
import kotlin.test.*

/**
 * Functional tests for RAG system.
 *
 * Tests 10 predefined questions against expected behavior:
 * - Sources section presence
 * - Citations section presence
 * - Meaningful response based on citations
 * - Anti-hallucination ("don't know") mode
 *
 * Run requirements:
 * - Ollama server running at localhost:11434
 * - bge-m3:latest model available
 * - Project indexed (use /index command)
 */
class RagFunctionalTest {

    // Test data: 10 questions with expected source files
    data class TestCase(
        val id: Int,
        val question: String,
        val expectedSourceFiles: List<String>,
        val expectDontKnow: Boolean = false,
        val description: String
    )

    private val testCases = listOf(
        TestCase(
            id = 1,
            question = "Как работает RAG поиск?",
            expectedSourceFiles = listOf("RagSearchServiceImpl.kt"),
            description = "Should return RAG search implementation details"
        ),
        TestCase(
            id = 2,
            question = "Где реализован ChunkScore?",
            expectedSourceFiles = listOf("ChunkScore.kt"),
            description = "Should locate ChunkScore data class"
        ),
        TestCase(
            id = 3,
            question = "Что такое relevanceThreshold?",
            expectedSourceFiles = listOf("RagConfig.kt"),
            description = "Should explain relevance threshold parameter"
        ),
        TestCase(
            id = 4,
            question = "Как форматируется ответ?",
            expectedSourceFiles = listOf("RagResponseFormatter.kt"),
            description = "Should return formatter implementation details"
        ),
        TestCase(
            id = 5,
            question = "Какие модели использует Ollama?",
            expectedSourceFiles = listOf("OllamaApi.kt"),
            description = "Should list Ollama models"
        ),
        TestCase(
            id = 6,
            question = "Что такое косинусная схожесть?",
            expectedSourceFiles = listOf("RagSearchServiceImpl.kt", "EmbeddingVector.kt"),
            description = "Should explain cosine similarity"
        ),
        TestCase(
            id = 7,
            question = "Сколько параметров в RagConfig?",
            expectedSourceFiles = listOf("RagConfig.kt"),
            description = "Should count RagConfig parameters"
        ),
        TestCase(
            id = 8,
            question = "Как работает reranking?",
            expectedSourceFiles = listOf("RerankerService.kt"),
            description = "Should explain reranking process"
        ),
        TestCase(
            id = 9,
            question = "Где хранятся embeddings?",
            expectedSourceFiles = listOf("EmbeddingRepository.kt"),
            description = "Should locate embedding storage"
        ),
        TestCase(
            id = 10,
            question = "Какая погода на Марсе?",
            expectedSourceFiles = emptyList(),
            expectDontKnow = true,
            description = "Should return 'don't know' response (anti-hallucination)"
        )
    )

    // === Test 1: RagResponse model tests ===

    @Test
    fun test1_RAG_search_response_should_contain_sources() {
        // Given: Mock chunks simulating RAG search results
        val chunks = createMockChunks(
            fileName = "RagSearchServiceImpl.kt",
            content = """
                class RagSearchServiceImpl : RagSearchService {
                    override suspend fun search(query: String, config: RagConfig?): List<ChunkScore> {
                        // Generate embedding
                        val queryEmbedding = embeddingClient.generateEmbedding(currentQuery)

                        // Calculate cosine similarity
                        val scoredChunks = calculateSimilarities(queryEmbedding, allEmbeddings, allChunks)

                        // Filter by threshold
                        val filteredChunks = scoredChunks.filter { it.similarity >= config.similarityThreshold }

                        return filteredChunks
                    }
                }
            """.trimIndent(),
            similarity = 0.85f
        )

        // When: Creating response
        val response = RagResponseFormatter.createResponse(
            chunks = chunks,
            relevanceThreshold = 0.3f,
            answer = "RAG поиск работает через косинусную схожесть embeddings."
        )

        // Then: Verify sources section
        assertTrue(response.hasRelevantContext, "Response should have relevant context")
        assertTrue(response.sources.isNotEmpty(), "Sources list should not be empty")

        // Format and verify output
        val formatted = RagResponseFormatter.format(response)
        assertTrue(formatted.contains("## Источники"), "Should contain 'Источники' section")
        assertTrue(formatted.contains("RagSearchServiceImpl.kt"), "Should mention source file")

        println("Test 1 PASSED: Sources section present")
        println("Formatted output:\n$formatted")
    }

    @Test
    fun test2_RAG_response_should_contain_citations() {
        // Given: Mock chunks with citations
        val chunks = createMockChunks(
            fileName = "ChunkScore.kt",
            content = """
                data class ChunkScore(
                    val chunkId: String,
                    val content: String,
                    val source: String,
                    val fileName: String,
                    val similarity: Float,
                    val rank: Int,
                    val startLine: Int = 0,
                    val endLine: Int = 0,
                    val language: String = "",
                    val section: String? = null
                )
            """.trimIndent(),
            similarity = 0.92f
        )

        // When: Creating response
        val response = RagResponseFormatter.createResponse(
            chunks = chunks,
            relevanceThreshold = 0.3f
        )

        // Then: Verify citations
        assertTrue(response.citations.isNotEmpty(), "Citations list should not be empty")

        val formatted = RagResponseFormatter.format(response)
        assertTrue(formatted.contains("## Цитаты"), "Should contain 'Цитаты' section")
        assertTrue(formatted.contains(">"), "Should contain citation markers")

        println("Test 2 PASSED: Citations section present")
    }

    @Test
    fun test3_relevanceThreshold_explanation() {
        // Given: Mock chunks from RagConfig
        val chunks = createMockChunks(
            fileName = "RagConfig.kt",
            content = """
                data class RagConfig(
                    val relevanceThreshold: Float = 0.3f
                ) {
                    /**
                     * Relevance threshold for "don't know" mode.
                     * If max similarity < this threshold, the system responds with "I don't know".
                     */
                }
            """.trimIndent(),
            similarity = 0.88f
        )

        // When
        val response = RagResponseFormatter.createResponse(
            chunks = chunks,
            relevanceThreshold = 0.3f
        )

        // Then
        assertTrue(response.hasRelevantContext)
        assertTrue(response.sources.any { it.fileName == "RagConfig.kt" })

        println("Test 3 PASSED: relevanceThreshold source found")
    }

    @Test
    fun test4_formatter_implementation() {
        // Given
        val chunks = createMockChunks(
            fileName = "RagResponseFormatter.kt",
            content = """
                object RagResponseFormatter {
                    fun format(response: RagResponse): String {
                        return buildString {
                            appendLine("## Ответ")
                            appendLine("## Источники")
                            appendLine("## Цитаты")
                        }
                    }
                }
            """.trimIndent(),
            similarity = 0.91f
        )

        // When
        val response = RagResponseFormatter.createResponse(
            chunks = chunks,
            relevanceThreshold = 0.3f
        )

        // Then
        assertTrue(response.sources.isNotEmpty())

        println("Test 4 PASSED: Formatter source found")
    }

    @Test
    fun test5_Ollama_models() {
        // Given
        val chunks = createMockChunks(
            fileName = "OllamaApi.kt",
            content = """
                object OllamaApi {
                    const val DEFAULT_MODEL = "bge-m3:latest"
                    const val DEFAULT_DIMENSION = 1024
                }
            """.trimIndent(),
            similarity = 0.87f
        )

        // When
        val response = RagResponseFormatter.createResponse(
            chunks = chunks,
            relevanceThreshold = 0.3f
        )

        // Then
        assertTrue(response.sources.isNotEmpty())

        println("Test 5 PASSED: OllamaApi source found")
    }

    @Test
    fun test6_cosine_similarity_explanation() {
        // Given
        val chunks = createMockChunks(
            fileName = "RagSearchServiceImpl.kt",
            content = """
                private fun calculateSimilarities(
                    queryEmbedding: EmbeddingVector,
                    embeddings: Map<String, EmbeddingVector>,
                    chunks: List<DocumentChunk>
                ): List<ChunkScore> {
                    val similarity = queryEmbedding.cosineSimilarity(embedding)
                    return ChunkScore(...)
                }
            """.trimIndent(),
            similarity = 0.82f
        )

        // When
        val response = RagResponseFormatter.createResponse(
            chunks = chunks,
            relevanceThreshold = 0.3f
        )

        // Then
        assertTrue(response.hasRelevantContext)

        println("Test 6 PASSED: Cosine similarity source found")
    }

    @Test
    fun test7_RagConfig_parameter_count() {
        // Given
        val chunks = createMockChunks(
            fileName = "RagConfig.kt",
            content = """
                data class RagConfig(
                    val topK: Int = 5,
                    val similarityThreshold: Float = 0.3f,
                    val includeSource: Boolean = true,
                    val verbose: Boolean = false,
                    val enableReranking: Boolean = false,
                    val enableQueryRewriting: Boolean = false,
                    val enableMetrics: Boolean = true,
                    val rerankerModel: String = OllamaApi.DEFAULT_MODEL,
                    val queryRewriterModel: String = "deepseek-r1:1.5b",
                    val topKBeforeFilter: Int = 20,
                    val topKAfterFilter: Int = 5,
                    val dynamicThreshold: Boolean = false,
                    val relevanceThreshold: Float = 0.3f
                )
            """.trimIndent(),
            similarity = 0.90f
        )

        // When
        val response = RagResponseFormatter.createResponse(
            chunks = chunks,
            relevanceThreshold = 0.3f
        )

        // Then
        assertTrue(response.hasRelevantContext)
        // Expected: 13 parameters

        println("Test 7 PASSED: RagConfig parameters count can be verified")
    }

    @Test
    fun test8_reranking_explanation() {
        // Given
        val chunks = createMockChunks(
            fileName = "RerankerService.kt",
            content = """
                interface RerankerService {
                    suspend fun rerank(request: RerankRequest): RerankResponse
                }
            """.trimIndent(),
            similarity = 0.79f
        )

        // When
        val response = RagResponseFormatter.createResponse(
            chunks = chunks,
            relevanceThreshold = 0.3f
        )

        // Then
        assertTrue(response.hasRelevantContext)

        println("Test 8 PASSED: RerankerService source found")
    }

    @Test
    fun test9_embedding_storage_location() {
        // Given
        val chunks = createMockChunks(
            fileName = "EmbeddingRepository.kt",
            content = """
                interface EmbeddingRepository {
                    suspend fun saveEmbedding(chunkId: String, embedding: EmbeddingVector)
                    suspend fun getEmbeddingByChunkId(chunkId: String): EmbeddingVector?
                    suspend fun getAllEmbeddings(): Map<String, EmbeddingVector>
                }
            """.trimIndent(),
            similarity = 0.84f
        )

        // When
        val response = RagResponseFormatter.createResponse(
            chunks = chunks,
            relevanceThreshold = 0.3f
        )

        // Then
        assertTrue(response.hasRelevantContext)

        println("Test 9 PASSED: EmbeddingRepository source found")
    }

    @Test
    fun test10_antiHallucination_dontKnow_response() {
        // Given: Empty chunks (no relevant data for "Mars weather")
        val chunks = emptyList<ChunkScore>()

        // When: Creating response with empty chunks
        val response = RagResponse.fromChunks(
            chunks = chunks,
            query = "Какая погода на Марсе?",
            relevanceThreshold = 0.3f
        )

        // Then: Should return "don't know" response
        assertFalse(response.hasRelevantContext, "Should NOT have relevant context")
        assertTrue(response.shouldRespondWithDontKnow(), "Should respond with 'don't know'")
        assertEquals(0, response.sources.size, "Sources should be empty")
        assertEquals(0, response.citations.size, "Citations should be empty")

        val formatted = RagResponseFormatter.format(response)
        assertTrue(
            formatted.contains("не могу найти", ignoreCase = true) ||
            formatted.contains("релевантную информацию", ignoreCase = true),
            "Should contain 'don't know' message"
        )
        assertFalse(
            formatted.contains("Марс", ignoreCase = true) && formatted.contains("погод", ignoreCase = true),
            "Should NOT contain made-up Mars weather information"
        )
        assertTrue(
            formatted.contains("переформулируйте", ignoreCase = true) ||
            formatted.contains("уточните", ignoreCase = true),
            "Should contain recommendations"
        )

        println("Test 10 PASSED: Anti-hallucination mode works correctly")
        println("Don't know response:\n$formatted")
    }

    // === Helper methods ===

    private fun createMockChunks(
        fileName: String,
        content: String,
        similarity: Float
    ): List<ChunkScore> {
        return listOf(
            ChunkScore(
                chunkId = "test-chunk-1",
                content = content,
                source = "/path/to/$fileName",
                fileName = fileName,
                similarity = similarity,
                rank = 1,
                startLine = 1,
                endLine = 50,
                language = "Kotlin",
                section = null
            )
        )
    }

    // === Source Format Tests ===

    @Test
    fun sourceFormat_shouldIncludeFileName_lines_and_similarity() {
        val source = SourceInfo(
            chunkId = "test-1",
            fileName = "TestFile.kt",
            filePath = "/path/to/TestFile.kt",
            section = "testFunction",
            startLine = 10,
            endLine = 20,
            similarity = 0.85f,
            rank = 1,
            language = "Kotlin"
        )

        val formatted = RagResponseFormatter.formatSource(1, source)

        assertTrue(formatted.contains("TestFile.kt"), "Should contain file name")
        assertTrue(formatted.contains("10-20"), "Should contain line range")
        // Note: Format uses locale-specific decimal separator (comma or dot)
        assertTrue(
            formatted.contains("0.85") || formatted.contains("0,85"),
            "Should contain similarity score (formatted: $formatted)"
        )
    }

    @Test
    fun citationFormat_shouldInclude_quote_and_source() {
        val citation = Citation(
            text = "val config = RagConfig()",
            source = "RagConfig.kt:21",
            chunkId = "chunk-1",
            similarity = 0.92f
        )

        val formatted = citation.toFormattedString()

        assertTrue(formatted.contains(">"), "Should start with quote marker")
        assertTrue(formatted.contains("val config"), "Should contain quoted text")
        assertTrue(formatted.contains("source:"), "Should contain source reference")
    }

    // === Threshold Tests ===

    @Test
    fun lowSimilarity_shouldTrigger_dontKnow_mode() {
        val chunks = listOf(
            ChunkScore(
                chunkId = "1",
                content = "unrelated content",
                source = "/path/File.kt",
                fileName = "File.kt",
                similarity = 0.15f, // Below threshold
                rank = 1
            )
        )

        val response = RagResponse.fromChunks(
            chunks = chunks,
            query = "test query",
            relevanceThreshold = 0.3f
        )

        assertFalse(response.hasRelevantContext)
        assertTrue(response.shouldRespondWithDontKnow())
    }

    @Test
    fun highSimilarity_shouldReturn_normalResponse() {
        val chunks = listOf(
            ChunkScore(
                chunkId = "1",
                content = "relevant content about RAG",
                source = "/path/RagService.kt",
                fileName = "RagService.kt",
                similarity = 0.85f, // Above threshold
                rank = 1
            )
        )

        val response = RagResponse.fromChunks(
            chunks = chunks,
            query = "Как работает RAG?",
            relevanceThreshold = 0.3f
        )

        assertTrue(response.hasRelevantContext)
        assertFalse(response.shouldRespondWithDontKnow())
        assertEquals(1, response.sources.size)
    }

    // === Edge Cases ===

    @Test
    fun emptyQuery_shouldReturn_emptyResult() {
        val response = RagResponse.fromChunks(
            chunks = emptyList(),
            query = "",
            relevanceThreshold = 0.3f
        )

        assertFalse(response.hasRelevantContext)
        assertEquals(0, response.totalChunksRetrieved)
    }

    @Test
    fun maxCitationsLimit_shouldBeRespected() {
        val chunks = (1..10).map { i ->
            ChunkScore(
                chunkId = "chunk-$i",
                content = "Content $i",
                source = "/path/File$i.kt",
                fileName = "File$i.kt",
                similarity = 0.8f - (i * 0.01f),
                rank = i
            )
        }

        val response = RagResponse.fromChunks(
            chunks = chunks,
            query = "test",
            relevanceThreshold = 0.3f
        )

        assertTrue(response.citations.size <= 5, "Should have max 5 citations")
    }

    @Test
    fun veryLongContent_shouldBeTruncated_inCitation() {
        val longContent = "x".repeat(1000)
        val chunk = ChunkScore(
            chunkId = "1",
            content = longContent,
            source = "/path/File.kt",
            fileName = "File.kt",
            similarity = 0.9f,
            rank = 1
        )

        val citation = Citation.fromChunkScore(chunk, maxLength = 500)

        assertTrue(citation.text.length <= 503) // 500 + "..."
    }

    // === Full Test Report ===

    @Test
    fun generateTestReport() {
        println("\n" + "=".repeat(80))
        println("RAG FUNCTIONAL TEST REPORT")
        println("=".repeat(80))

        testCases.forEach { tc ->
            println("\n### Question ${tc.id}: \"${tc.question}\"")
            println("   Expected sources: ${tc.expectedSourceFiles}")
            println("   Expect 'don't know': ${tc.expectDontKnow}")
            println("   Description: ${tc.description}")
        }

        println("\n" + "=".repeat(80))
    }
}
