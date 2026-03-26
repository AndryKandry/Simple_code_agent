package ru.agent.cli.commands.index

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent.getKoin
import ru.agent.features.rag.domain.repository.DocumentIndexRepository
import ru.agent.features.rag.domain.repository.IndexMetadataRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Stats command for showing indexing statistics.
 *
 * Displays statistics about the current document index.
 */
class IndexStatsCommand : CliktCommand(
    name = "stats",
    help = "Show indexing statistics"
) {
    private val json by option("-j", "--json", help = "Output in JSON format")
        .flag(default = false)

    override fun run() {
        runBlocking {
            val metadataRepo: IndexMetadataRepository = getKoin().get()
            val chunkRepo: DocumentIndexRepository = getKoin().get()

            val stats = metadataRepo.getStats()
            if (stats == null) {
                echo("No index found. Run 'agent index run' first.")
                return@runBlocking
            }

            if (json) {
                printJsonStats(stats, chunkRepo)
            } else {
                printHumanStats(stats, chunkRepo)
            }
        }
    }

    private suspend fun printHumanStats(
        stats: ru.agent.features.rag.domain.model.IndexStats,
        chunkRepo: DocumentIndexRepository
    ) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        echo("=== Index Statistics ===")
        echo("")
        echo("Strategy: ${formatStrategy(stats.strategy)}")
        echo("Embedding model: ${stats.model}")
        echo("")
        echo("Files indexed: ${stats.totalFiles}")
        echo("Total chunks: ${stats.totalChunks}")
        echo("")
        echo("=== Token Statistics ===")
        echo("Average tokens/chunk: ${"%.1f".format(stats.avgTokensPerChunk)}")
        echo("Min tokens: ${stats.minTokens}")
        echo("Max tokens: ${stats.maxTokens}")
        echo("Std deviation: ${"%.1f".format(stats.stdDevTokens)}")
        echo("")
        echo("Indexing duration: ${formatDuration(stats.durationMs)}")
        echo("Indexed at: ${dateFormat.format(Date(stats.indexedAt))}")
        echo("")

        // Show chunks by language
        val byLanguage = chunkRepo.getChunkCountByLanguage()
        if (byLanguage.isNotEmpty()) {
            echo("=== Chunks by Language ===")
            byLanguage.entries
                .sortedByDescending { it.value }
                .forEach { (lang, count) ->
                    val percentage = (count.toDouble() / stats.totalChunks * 100)
                    echo("  $lang: $count (${"%.1f".format(percentage)}%)")
                }
        }
    }

    private suspend fun printJsonStats(
        stats: ru.agent.features.rag.domain.model.IndexStats,
        chunkRepo: DocumentIndexRepository
    ) {
        val byLanguage = chunkRepo.getChunkCountByLanguage()

        echo("""
            |{
            |  "strategy": "${stats.strategy.name}",
            |  "model": "${stats.model}",
            |  "totalFiles": ${stats.totalFiles},
            |  "totalChunks": ${stats.totalChunks},
            |  "tokens": {
            |    "avg": ${stats.avgTokensPerChunk},
            |    "min": ${stats.minTokens},
            |    "max": ${stats.maxTokens},
            |    "stdDev": ${stats.stdDevTokens}
            |  },
            |  "durationMs": ${stats.durationMs},
            |  "indexedAt": ${stats.indexedAt},
            |  "byLanguage": {
            ${byLanguage.entries.joinToString(",\n") { (lang, count) ->
                "    \"$lang\": $count"
            }}
            |  }
            |}
        """.trimMargin())
    }

    private fun formatStrategy(strategy: ru.agent.features.rag.domain.model.IndexingStrategy): String {
        return when (strategy) {
            ru.agent.features.rag.domain.model.IndexingStrategy.FIXED_SIZE -> "Fixed-Size (consistent chunk sizes)"
            ru.agent.features.rag.domain.model.IndexingStrategy.STRUCTURE_BASED -> "Structure-Based (code-aware chunking)"
        }
    }

    private fun formatDuration(ms: Long): String {
        return when {
            ms < 1000 -> "${ms}ms"
            ms < 60000 -> "${ms / 1000}.${(ms % 1000) / 100}s"
            else -> "${ms / 60000}m ${(ms % 60000) / 1000}s"
        }
    }
}
