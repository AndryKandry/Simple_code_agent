package ru.agent.features.chat.domain.optimization

import ru.agent.core.time.currentTimeMillis
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.model.SenderType

/**
 * TOON (Token Oriented Object Notation) Encoder.
 *
 * TOON is a token-efficient format for serializing chat messages.
 * It uses a tabular format instead of JSON, saving approximately 50% of tokens.
 *
 * Format example:
 * ```
 * messages[3]{role,content}:
 * user,Hello
 * assistant,Hi there!
 * user,How are you?
 * ```
 *
 * Reference: https://github.com/toon-format/spec
 */
object ToonEncoder {

    private const val HEADER_PREFIX = "messages["
    private const val HEADER_SUFFIX = "]{role,content}:\n"
    private const val ROLE_USER = "user"
    private const val ROLE_ASSISTANT = "assistant"

    /**
     * Encode a list of messages to TOON format.
     *
     * @param messages List of messages to encode
     * @return TOON-encoded string
     */
    fun encode(messages: List<Message>): String {
        if (messages.isEmpty()) {
            return ""
        }

        val header = "$HEADER_PREFIX${messages.size}$HEADER_SUFFIX"
        val body = messages.joinToString("\n") { message ->
            val role = when (message.senderType) {
                SenderType.USER -> ROLE_USER
                SenderType.ASSISTANT -> ROLE_ASSISTANT
            }
            // Escape commas and newlines in content
            val escapedContent = escapeContent(message.content)
            "$role,$escapedContent"
        }

        return header + body
    }

    /**
     * Decode a TOON string back to a list of messages.
     *
     * @param toonString TOON-encoded string
     * @return List of decoded messages
     */
    fun decode(toonString: String): List<Message> {
        if (toonString.isBlank()) {
            return emptyList()
        }

        val lines = toonString.lines()

        // Find header line
        val headerIndex = lines.indexOfFirst { it.startsWith(HEADER_PREFIX) }
        if (headerIndex == -1) {
            return emptyList()
        }

        // Parse message count from header
        val countMatch = Regex("messages\\[(\\d+)]").find(lines[headerIndex])
        val expectedCount = countMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

        // Parse messages
        val messages = lines
            .drop(headerIndex + 1)
            .filter { it.isNotBlank() }
            .take(expectedCount)
            .mapIndexed { index, line ->
                parseMessageLine(line, index)
            }

        return messages
    }

    /**
     * Estimate token count for a given text.
     * Uses a simple approximation: ~4 characters per token.
     *
     * @param text Text to estimate
     * @return Estimated token count
     */
    fun estimateTokens(text: String): Int {
        if (text.isBlank()) {
            return 0
        }
        // Rough approximation: ~4 characters per token
        // This is a simplification; real tokenizers vary by model
        return (text.length / 4.0).toInt().coerceAtLeast(1)
    }

    /**
     * Calculate token savings when using TOON vs JSON.
     *
     * @param messages List of messages to compare
     * @return TokenSavings with comparison data
     */
    fun calculateSavings(messages: List<Message>): TokenSavings {
        if (messages.isEmpty()) {
            return TokenSavings(
                jsonTokens = 0,
                toonTokens = 0,
                savedTokens = 0,
                savingsPercentage = 0.0
            )
        }

        val jsonString = messages.toJsonString()
        val toonString = encode(messages)

        val jsonTokens = estimateTokens(jsonString)
        val toonTokens = estimateTokens(toonString)
        val savedTokens = jsonTokens - toonTokens

        val savingsPercentage = if (jsonTokens > 0) {
            (savedTokens.toDouble() / jsonTokens) * 100
        } else {
            0.0
        }

        return TokenSavings(
            jsonTokens = jsonTokens,
            toonTokens = toonTokens,
            savedTokens = savedTokens,
            savingsPercentage = savingsPercentage
        )
    }

    private fun escapeContent(content: String): String {
        return content
            .replace("\\", "\\\\")
            .replace(",", "\\,")
            .replace("\n", "\\n")
    }

    private fun unescapeContent(content: String): String {
        return content
            .replace("\\n", "\n")
            .replace("\\,", ",")
            .replace("\\\\", "\\")  // Must be LAST to avoid double-unescape
    }

    private fun parseMessageLine(line: String, index: Int): Message {
        // Find first unescaped comma
        var commaIndex = -1
        var i = 0
        while (i < line.length) {
            if (line[i] == ',' && (i == 0 || line[i - 1] != '\\')) {
                commaIndex = i
                break
            }
            i++
        }

        val role = if (commaIndex >= 0) line.substring(0, commaIndex) else ROLE_USER
        val content = if (commaIndex >= 0 && commaIndex < line.length - 1) {
            unescapeContent(line.substring(commaIndex + 1))
        } else {
            ""
        }

        val senderType = when (role) {
            ROLE_USER -> SenderType.USER
            ROLE_ASSISTANT -> SenderType.ASSISTANT
            else -> SenderType.USER
        }

        return Message(
            id = "toon-$index",
            content = content,
            senderType = senderType,
            timestamp = currentTimeMillis()
        )
    }

    private fun List<Message>.toJsonString(): String {
        val messagesJson = this.joinToString(",\n") { message ->
            val role = when (message.senderType) {
                SenderType.USER -> "user"
                SenderType.ASSISTANT -> "assistant"
            }
            """    {"role": "$role", "content": "${message.content.escapeJson()}"}"""
        }
        return "[\n$messagesJson\n]"
    }

    private fun String.escapeJson(): String {
        return this
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\t", "\\t")
    }
}

/**
 * Data class representing token savings comparison.
 */
data class TokenSavings(
    val jsonTokens: Int,
    val toonTokens: Int,
    val savedTokens: Int,
    val savingsPercentage: Double
)
