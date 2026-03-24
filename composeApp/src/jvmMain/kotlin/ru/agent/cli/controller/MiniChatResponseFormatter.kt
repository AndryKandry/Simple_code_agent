package ru.agent.cli.controller

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.bold
import ru.agent.features.rag.domain.model.ChunkScore

/**
 * Compact formatter for mini-chat CLI output.
 *
 * Provides clean, structured output for:
 * - Assistant responses
 * - RAG sources (compact format)
 * - Task context information
 */
object MiniChatResponseFormatter {

    /**
     * Format the complete assistant response with sources.
     *
     * @param content The assistant's response content
     * @param timestamp The timestamp string (e.g., "11:14")
     * @param sources Optional list of RAG sources
     * @return Formatted string ready for output
     */
    fun formatResponse(
        content: String,
        timestamp: String,
        sources: List<ChunkScore>? = null
    ): String {
        return buildString {
            // Header with timestamp
            appendLine()
            append(gray("$timestamp ${bold("Assistant:")}"))
            appendLine()

            // Content with proper indentation
            val lines = content.lines()
            lines.forEach { line ->
                when {
                    line.startsWith("## ") -> {
                        // Markdown headers - format as subheaders
                        val headerText = line.removePrefix("## ")
                        append(cyan("▸ $headerText"))
                        appendLine()
                    }
                    line.startsWith("# ") -> {
                        // Main headers
                        val headerText = line.removePrefix("# ")
                        append(bold("┃ $headerText"))
                        appendLine()
                    }
                    line.startsWith("  - ") || line.startsWith("- ") -> {
                        // Bullet points
                        val bulletText = line.removePrefix("  - ").removePrefix("- ")
                        append("  ${green("•")} $bulletText")
                        appendLine()
                    }
                    line.startsWith("    ") -> {
                        // Indented content (code blocks, etc.)
                        append(gray(line))
                        appendLine()
                    }
                    line.startsWith("```") -> {
                        // Code block markers - skip for cleaner output
                    }
                    line.isNotBlank() -> {
                        // Regular content
                        append("  $line")
                        appendLine()
                    }
                }
            }

            // Sources section (compact)
            if (!sources.isNullOrEmpty()) {
                appendLine()
                append(formatCompactSources(sources))
            }
        }
    }

    /**
     * Format sources in a compact, user-friendly way.
     *
     * @param sources List of chunks with similarity scores
     * @return Formatted sources string
     */
    fun formatCompactSources(sources: List<ChunkScore>): String {
        return buildString {
            append(bold("📚 Sources:"))
            append(" ")
            append(gray("(${sources.size} chunks found)"))
            appendLine()

            sources.take(5).forEachIndexed { index, chunk ->
                val scoreColor = when {
                    chunk.similarity >= 0.8f -> green
                    chunk.similarity >= 0.6f -> yellow
                    else -> gray
                }

                val score = scoreColor("(%.0f%%)".format(chunk.similarity * 100))

                // Format: filename:lines or just filename
                val location = if (chunk.startLine > 0) {
                    "${chunk.fileName}:${chunk.startLine}${if (chunk.endLine > chunk.startLine) "-${chunk.endLine}" else ""}"
                } else {
                    chunk.fileName
                }

                // Optional section info
                val section = chunk.section?.let { " ── $it" } ?: ""

                append("  ${scoreColor("│")} ")
                append(bold(location))
                append(" ")
                append(score)
                if (section.isNotBlank()) {
                    append(gray(section))
                }
                appendLine()
            }

            if (sources.size > 5) {
                append("  ${gray("│")} ...and ${sources.size - 5} more")
                appendLine()
            }
        }
    }

    /**
     * Format task context header.
     *
     * @param goal The current goal (optional)
     * @param clarificationsCount Number of clarifications
     * @param constraintsCount Number of constraints
     * @param ragQueriesCount Number of RAG queries
     * @return Formatted context string
     */
    fun formatContext(
        goal: String?,
        clarificationsCount: Int,
        constraintsCount: Int,
        ragQueriesCount: Int
    ): String {
        if (goal == null) return ""

        return buildString {
            appendLine()
            append(gray("Task Context:"))
            appendLine()
            append(cyan("  Goal: $goal"))
            appendLine()

            val details = mutableListOf<String>()
            if (clarificationsCount > 0) details.add("$clarificationsCount clarifications")
            if (constraintsCount > 0) details.add("$constraintsCount constraints")
            details.add("$ragQueriesCount RAG queries")

            append(gray("  ${details.joinToString(", ")}"))
            appendLine()
        }
    }
}
