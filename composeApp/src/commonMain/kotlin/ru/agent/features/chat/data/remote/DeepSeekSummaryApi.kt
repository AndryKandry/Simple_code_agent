package ru.agent.features.chat.data.remote

import co.touchlab.kermit.Logger
import ru.agent.features.chat.data.remote.dto.ChatRequest
import ru.agent.features.chat.data.remote.dto.MessageDto
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.model.SenderType
import ru.agent.features.chat.domain.repository.SummaryApi

/**
 * DeepSeek implementation of SummaryApi.
 *
 * Uses DeepSeek chat completions API to generate conversation summaries.
 * This class belongs to the data layer and handles all API-specific logic.
 */
class DeepSeekSummaryApi(
    private val deepSeekApiClient: DeepSeekApiClient
) : SummaryApi {

    private val logger = Logger.withTag("DeepSeekSummaryApi")

    companion object {
        private const val SYSTEM_PROMPT = """
You are a conversation summarizer. Create a concise summary of the conversation.

Requirements:
1. Preserve key information, decisions, and context
2. Keep the summary under 200 tokens
3. Maintain chronological order
4. Include code snippets, file names, technical details
5. Note unresolved questions or ongoing tasks
6. Be concise but comprehensive

Format the summary in a clear, readable way.
"""
    }

    override suspend fun generateSummary(
        messages: List<Message>,
        maxTokens: Int
    ): String? {
        if (messages.isEmpty()) {
            logger.w { "Cannot generate summary: messages list is empty" }
            return null
        }

        logger.i { "Generating summary for ${messages.size} messages via DeepSeek API" }

        return try {
            // Prepare conversation for summarization
            val conversationText = formatConversationForSummary(messages)

            val request = ChatRequest(
                messages = listOf(
                    MessageDto(
                        role = "system",
                        content = SYSTEM_PROMPT.trimIndent()
                    ),
                    MessageDto(
                        role = "user",
                        content = "Please summarize the following conversation:\n\n$conversationText"
                    )
                ),
                maxTokens = maxTokens
            )

            val response = deepSeekApiClient.sendMessage(request)

            if (response.choices.isEmpty()) {
                logger.e { "Empty response from DeepSeek API when generating summary" }
                return null
            }

            val summary = response.choices.first().message.content
            logger.i { "Summary generated successfully (${summary.length} chars)" }

            summary

        } catch (e: DeepSeekApiTimeoutException) {
            logger.e(throwable = e) { "DeepSeek API timeout when generating summary" }
            null
        } catch (e: Exception) {
            logger.e(throwable = e) { "Failed to generate summary via DeepSeek API" }
            null
        }
    }

    /**
     * Format messages into a text suitable for summarization.
     */
    private fun formatConversationForSummary(messages: List<Message>): String {
        return messages.joinToString("\n\n") { message ->
            val sender = when (message.senderType) {
                SenderType.USER -> "User"
                SenderType.ASSISTANT -> "Assistant"
            }
            "$sender: ${message.content}"
        }
    }
}
