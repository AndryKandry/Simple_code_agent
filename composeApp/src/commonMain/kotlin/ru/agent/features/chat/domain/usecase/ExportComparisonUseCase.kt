package ru.agent.features.chat.domain.usecase

import ru.agent.features.chat.domain.model.ComparisonResult
import ru.agent.features.chat.domain.model.ContextStrategy
import ru.agent.features.chat.domain.model.ExportFormat
import ru.agent.features.chat.domain.model.QualityScore

/**
 * Use Case for exporting comparison results.
 *
 * Supports multiple export formats: JSON and Markdown.
 */
class ExportComparisonUseCase {

    /**
     * Export comparison result to specified format.
     *
     * @param result The comparison result to export
     * @param format Export format (default: JSON)
     * @return Formatted string ready to save to file
     */
    operator fun invoke(
        result: ComparisonResult,
        format: ExportFormat = ExportFormat.JSON
    ): String {
        return when (format) {
            ExportFormat.JSON -> exportToJson(result)
            ExportFormat.MARKDOWN -> exportToMarkdown(result)
        }
    }

    /**
     * Export to JSON format.
     */
    private fun exportToJson(result: ComparisonResult): String {
        val resultsJson = result.results.entries.joinToString(",\n") { (strategy, strategyResult) ->
            """
            |    "${strategy.name}": {
            |      "strategy": "${strategyResult.strategy.displayName}",
            |      "sessionId": "${strategyResult.sessionId}",
            |      "assistantResponse": ${escapeJson(strategyResult.assistantResponse)},
            |      "tokenCount": ${strategyResult.tokenCount},
            |      "responseTimeMs": ${strategyResult.responseTimeMs},
            |      "messageCount": ${strategyResult.messageCount},
            |      "qualityScore": "${strategyResult.qualityScore.displayName}",
            |      "factsUsed": ${formatFactsJson(strategyResult.factsUsed)},
            |      "branchUsed": ${strategyResult.branchUsed?.let { "\"$it\"" } ?: "null"}
            |    }""".trimMargin()
        }

        return """
            |{
            |  "id": "${result.id}",
            |  "comparisonSessionId": "${result.comparisonSessionId}",
            |  "createdAt": ${result.createdAt},
            |  "userMessage": ${escapeJson(result.userMessage)},
            |  "results": {
            |${resultsJson}
            |  }
            |}
        """.trimMargin()
    }

    /**
     * Export to Markdown format (human-readable).
     */
    private fun exportToMarkdown(result: ComparisonResult): String {
        val resultsMarkdown = result.results.entries.joinToString("\n\n") { (strategy, strategyResult) ->
            """
            |## ${strategy.displayName}
            |
            |**Response Time:** ${formatTime(strategyResult.responseTimeMs)}
            |
            |**Tokens:** ${strategyResult.tokenCount}
            |
            |**Messages:** ${strategyResult.messageCount}
            |
            |**Quality:** ${strategyResult.qualityScore.emoji} ${strategyResult.qualityScore.displayName}
            |
            |### Assistant Response
            |
            |${strategyResult.assistantResponse}
            |
            |${formatFactsMarkdown(strategyResult.factsUsed)}
            |${formatBranchMarkdown(strategyResult.branchUsed)}
            """.trimMargin()
        }

        return """
            |# Context Strategy Comparison
            |
            |**Date:** ${formatTimestamp(result.createdAt)}
            |
            |**User Message:**
            |
            |> ${result.userMessage}
            |
            |---
            |
            |$resultsMarkdown
            |
            |---
            |
            |## Summary
            |
            |${generateSummary(result)}
        """.trimMargin()
    }

    /**
     * Format facts for JSON.
     */
    private fun formatFactsJson(facts: List<ru.agent.features.chat.domain.model.Fact>?): String {
        if (facts.isNullOrEmpty()) return "[]"

        val factsJson = facts.joinToString(",\n") { fact ->
            """
            |        {
            |          "id": "${fact.id}",
            |          "category": "${fact.category.displayName}",
            |          "key": ${escapeJson(fact.key)},
            |          "value": ${escapeJson(fact.value)}
            |        }""".trimMargin()
        }

        return "[\n$factsJson\n      ]"
    }

    /**
     * Format facts for Markdown.
     */
    private fun formatFactsMarkdown(facts: List<ru.agent.features.chat.domain.model.Fact>?): String {
        if (facts.isNullOrEmpty()) return ""

        val factsByCategory = facts.groupBy { it.category }
        val factsMarkdown = factsByCategory.entries.joinToString("\n") { (category, categoryFacts) ->
            val factsList = categoryFacts.joinToString("\n") { fact ->
                "- **${fact.key}:** ${fact.value}"
            }
            "### ${category.icon} ${category.displayName}\n\n$factsList"
        }

        return "\n### Facts Used\n\n$factsMarkdown\n"
    }

    /**
     * Format branch info for Markdown.
     */
    private fun formatBranchMarkdown(branch: String?): String {
        if (branch == null) return ""
        return "\n**Branch:** $branch\n"
    }

    /**
     * Generate summary comparison.
     */
    private fun generateSummary(result: ComparisonResult): String {
        val results = result.results.values.toList()

        val fastest = results.minByOrNull { it.responseTimeMs }
        val mostEfficient = results.minByOrNull { it.tokenCount }
        val bestQuality = results.maxByOrNull { it.qualityScore.ordinal }

        return """
            |- **Fastest Response:** ${fastest?.strategy?.displayName ?: "N/A"} (${formatTime(fastest?.responseTimeMs ?: 0)})
            |- **Most Token Efficient:** ${mostEfficient?.strategy?.displayName ?: "N/A"} (${mostEfficient?.tokenCount ?: 0} tokens)
            |- **Best Quality:** ${bestQuality?.strategy?.displayName ?: "N/A"} (${bestQuality?.qualityScore?.emoji ?: ""} ${bestQuality?.qualityScore?.displayName ?: "N/A"})
        """.trimMargin()
    }

    /**
     * Format timestamp to readable date.
     */
    private fun formatTimestamp(timestamp: Long): String {
        return timestamp.toString()
    }

    /**
     * Format time in milliseconds.
     */
    private fun formatTime(ms: Long): String {
        return when {
            ms < 1000 -> "${ms}ms"
            ms < 60000 -> "${ms / 1000.0}s"
            else -> "${ms / 60000}m ${ms % 60000 / 1000}s"
        }
    }

    /**
     * Escape string for JSON with proper handling of all special characters.
     */
    private fun escapeJson(text: String): String {
        return buildString {
            append("\"")
            for (char in text) {
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")  // form feed
                    in '\u0000'..'\u001F' -> append("\\u${char.code.toString(16).padStart(4, '0')}")
                    else -> append(char)
                }
            }
            append("\"")
        }
    }
}
