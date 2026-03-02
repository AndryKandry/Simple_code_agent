package ru.agent.features.chat.domain.optimization

import co.touchlab.kermit.Logger
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.chat.data.remote.dto.ChatRequest
import ru.agent.features.chat.data.remote.dto.MessageDto
import ru.agent.features.chat.domain.model.ExtractedFactDto
import ru.agent.features.chat.domain.model.Fact
import ru.agent.features.chat.domain.model.FactCategory
import ru.agent.features.chat.domain.repository.LlmApiClient
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Extracts facts from conversation using LLM.
 *
 * This class uses LLM API to analyze conversations
 * and extract important facts (goals, constraints, preferences, etc.)
 * that should be preserved in the conversation context.
 *
 * Following Clean Architecture: Uses LlmApiClient interface instead of
 * concrete implementation to maintain domain layer independence.
 */
class FactsExtractor(
    private val llmApiClient: LlmApiClient
) {
    private val logger = Logger.withTag("FactsExtractor")

    companion object {
        const val EXTRACTION_TIMEOUT = 60_000L // 1 minute timeout for extraction
        const val MAX_FACTS_PER_EXTRACTION = 10
    }

    /**
     * System prompt for fact extraction.
     */
    private val extractionSystemPrompt = """
You are a fact extraction assistant. Your task is to analyze conversations and extract important facts.

Extract facts in these categories:
- GOAL: Project goals, objectives, or targets
- CONSTRAINTS: Limitations, restrictions, or boundaries
- PREFERENCES: User preferences, likes, or dislikes
- DECISIONS: Decisions made during the conversation
- AGREEMENTS: Agreements or commitments between parties

Instructions:
1. Analyze the user message and assistant response
2. Extract ONLY new or updated facts (not already in existing facts)
3. Return facts as a JSON array
4. Each fact should have: category, key (short name), value (detailed description), confidence (0.0-1.0)
5. If no new facts are found, return an empty array []
6. Respond ONLY with the JSON array, no additional text

Example output:
[
  {"category": "GOAL", "key": "Project Type", "value": "Building a mobile app for task management", "confidence": 0.95},
  {"category": "PREFERENCES", "key": "Framework", "value": "User prefers React Native for cross-platform development", "confidence": 0.9}
]
    """.trimIndent()

    /**
     * Extract facts from a conversation turn.
     *
     * @param sessionId The session ID
     * @param userMessage The user's message
     * @param assistantResponse The assistant's response
     * @param existingFacts Facts already extracted in this session
     * @param sourceMessageId ID of the source message (for tracking)
     * @return Result with list of extracted facts or error
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun extractFacts(
        sessionId: String,
        userMessage: String,
        assistantResponse: String,
        existingFacts: List<Fact>,
        sourceMessageId: String? = null
    ): Result<List<Fact>> {
        return try {
            logger.i { "Extracting facts for session: $sessionId" }

            withTimeout(EXTRACTION_TIMEOUT) {
                val prompt = buildExtractionPrompt(
                    userMessage = userMessage,
                    assistantResponse = assistantResponse,
                    existingFacts = existingFacts
                )

                val request = ChatRequest(
                    messages = listOf(
                        MessageDto(role = "system", content = extractionSystemPrompt),
                        MessageDto(role = "user", content = prompt)
                    ),
                    temperature = 0.3, // Lower temperature for more consistent extraction
                    maxTokens = 1000
                )

                val response = llmApiClient.sendMessage(request)
                val responseContent = response.choices.firstOrNull()?.message?.content

                if (responseContent.isNullOrBlank()) {
                    logger.d { "No response from LLM for fact extraction" }
                    return@withTimeout Result.success(emptyList())
                }

                val extractedFacts = parseExtractedFacts(
                    responseContent = responseContent,
                    sessionId = sessionId,
                    sourceMessageId = sourceMessageId
                )

                logger.i { "Extracted ${extractedFacts.size} facts" }
                Result.success(extractedFacts)
            }
        } catch (e: TimeoutCancellationException) {
            logger.w { "Fact extraction timed out" }
            Result.success(emptyList()) // Return empty on timeout, don't fail
        } catch (e: Exception) {
            logger.e(e) { "Failed to extract facts" }
            Result.failure(e)
        }
    }

    /**
     * Build the extraction prompt with context.
     */
    private fun buildExtractionPrompt(
        userMessage: String,
        assistantResponse: String,
        existingFacts: List<Fact>
    ): String {
        val existingFactsText = if (existingFacts.isNotEmpty()) {
            "\n\nExisting facts in this conversation:\n" +
                    existingFacts.groupBy { it.category }
                        .entries.joinToString("\n\n") { (category, facts) ->
                            "${category.icon} ${category.displayName}:\n" +
                                    facts.joinToString("\n") { "  - ${it.key}: ${it.value}" }
                        }
        } else {
            ""
        }

        return """
Analyze this conversation and extract any NEW or UPDATED facts.

Existing facts:$existingFactsText

User message:
$userMessage

Assistant response:
$assistantResponse

Extract only facts that are NEW or UPDATED (different from existing facts).
Return JSON array format.
        """.trimIndent()
    }

    /**
     * Parse the LLM response to extract facts.
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun parseExtractedFacts(
        responseContent: String,
        sessionId: String,
        sourceMessageId: String?
    ): List<Fact> {
        return try {
            // Clean the response - remove markdown code blocks if present
            val cleanedResponse = responseContent
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val json = Json { ignoreUnknownKeys = true }
            val extractedDtos = json.decodeFromString<List<ExtractedFactDto>>(cleanedResponse)

            val timestamp = currentTimeMillis()

            extractedDtos
                .take(MAX_FACTS_PER_EXTRACTION)
                .mapNotNull { dto ->
                    dto.toFact(
                        id = Uuid.random().toString(),
                        sessionId = sessionId,
                        sourceMessageId = sourceMessageId,
                        timestamp = timestamp
                    )
                }
                .filter { it.key.isNotBlank() && it.value.isNotBlank() }
        } catch (e: Exception) {
            logger.w { "Failed to parse extracted facts: ${e.message}" }
            emptyList()
        }
    }

    /**
     * Format facts for inclusion in the LLM context.
     *
     * @param facts Facts to format
     * @param maxFactsPerCategory Maximum facts per category to include
     * @return Formatted string for context
     */
    fun formatFactsForContext(
        facts: List<Fact>,
        maxFactsPerCategory: Int = 5
    ): String {
        if (facts.isEmpty()) {
            return ""
        }

        val groupedFacts = facts
            .groupBy { it.category }
            .mapValues { (_, categoryFacts) ->
                categoryFacts
                    .sortedByDescending { it.confidence }
                    .take(maxFactsPerCategory)
            }

        val factsText = groupedFacts.entries.joinToString("\n\n") { (category, categoryFacts) ->
            val factsList = categoryFacts.joinToString("\n") { fact ->
                "  - ${fact.key}: ${fact.value}"
            }
            "${category.icon} ${category.displayName}:\n$factsList"
        }

        return """
Important facts from our conversation:
$factsText
        """.trimIndent()
    }
}
