package ru.agent.cli.commands.index

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent.getKoin
import ru.agent.features.rag.domain.model.IndexingResult
import ru.agent.features.rag.domain.model.IndexingStrategy
import ru.agent.features.rag.pipeline.IndexingPipeline
import ru.agent.features.rag.pipeline.IndexingProgress
import java.nio.file.Path

/**
 * Run command for indexing project documents.
 *
 * Indexes git-tracked files and generates embeddings for RAG search.
 */
class IndexRunCommand : CliktCommand(
    name = "run",
    help = "Index project documents for RAG"
) {
    private val strategy by option("-s", "--strategy", help = "Chunking strategy: fixed (consistent size) or semantic (structure-based)")
        .choice("fixed" to "fixed", "semantic" to "semantic", "f" to "fixed", "s" to "semantic")
        .default("fixed")

    private val rebuild by option("-r", "--rebuild", help = "Rebuild index from scratch (clears existing data)")
        .flag(default = false)

    private val path by option("-p", "--path", help = "Project path to index (defaults to current directory)")
        .path(mustExist = true)

    private val verbose by option("-v", "--verbose", help = "Verbose output with detailed progress")
        .flag(default = false)

    override fun run() {
        val indexStrategy = when (strategy) {
            "semantic" -> IndexingStrategy.STRUCTURE_BASED
            else -> IndexingStrategy.FIXED_SIZE
        }

        val projectPath = path ?: Path.of(System.getProperty("user.dir"))

        try {
            runBlocking {
                val pipeline: IndexingPipeline = getKoin().get()

                echo("Starting indexing with ${strategy} strategy...")
                echo("Project path: $projectPath")

                if (verbose) {
                    echo("Strategy: $indexStrategy")
                    echo("Rebuild: $rebuild")
                }

                var finalResult: IndexingResult? = null

                pipeline.indexProject(indexStrategy, rebuild) { progress ->
                    handleProgress(progress, verbose)
                    if (progress is IndexingProgress.Completed) {
                        finalResult = IndexingResult.Success(
                            totalFiles = progress.stats.totalFiles,
                            totalChunks = progress.stats.totalChunks,
                            duration = progress.stats.durationMs,
                            stats = progress.stats
                        )
                    }
                    if (progress is IndexingProgress.Error) {
                        finalResult = IndexingResult.Error(progress.message)
                    }
                }

                when (val result = finalResult) {
                    is IndexingResult.Success -> {
                        echo("")
                        echo("Indexing completed successfully!")
                    }
                    is IndexingResult.Error -> {
                        echo("")
                        echo("Error: ${result.message}", err = true)
                        throw ProgramResult(1)
                    }
                    null -> {
                        echo("")
                        echo("Error: Unknown error occurred", err = true)
                        throw ProgramResult(1)
                    }
                }
            }
        } catch (e: Exception) {
            echo("Error: ${e.message}", err = true)
            e.printStackTrace()
            throw ProgramResult(1)
        }
    }

    private fun handleProgress(progress: IndexingProgress, verbose: Boolean) {
        when (progress) {
            is IndexingProgress.CheckingOllama -> {
                if (verbose) echo("Checking Ollama connection...")
            }
            is IndexingProgress.ClearingIndex -> {
                echo("Clearing existing index...")
            }
            is IndexingProgress.CollectingFiles -> {
                if (verbose) echo("Collecting files from git...")
            }
            is IndexingProgress.ProcessingFile -> {
                if (verbose) {
                    echo("Processing [${progress.current}/${progress.total}]: ${progress.path}")
                } else {
                    // Show only periodic updates in non-verbose mode
                    if (progress.current == 1 || progress.current == progress.total || progress.current % 10 == 0) {
                        echo("Processing files: ${progress.current}/${progress.total}")
                    }
                }
            }
            is IndexingProgress.GeneratingEmbeddings -> {
                if (verbose) {
                    echo("Generating embeddings [${progress.chunkIndex}/${progress.total}]")
                } else {
                    // Show only periodic updates in non-verbose mode
                    if (progress.chunkIndex == 1 || progress.chunkIndex == progress.total || progress.chunkIndex % 20 == 0) {
                        echo("Generating embeddings: ${progress.chunkIndex}/${progress.total}")
                    }
                }
            }
            is IndexingProgress.SavingChunks -> {
                echo("Saving ${progress.count} chunks to database...")
            }
            is IndexingProgress.Completed -> {
                echo("")
                echo("Indexing completed successfully!")
                echo("  Files processed: ${progress.stats.totalFiles}")
                echo("  Chunks created: ${progress.stats.totalChunks}")
                echo("  Avg tokens/chunk: ${"%.1f".format(progress.stats.avgTokensPerChunk)}")
                echo("  Duration: ${formatDuration(progress.stats.durationMs)}")
            }
            is IndexingProgress.Error -> {
                echo("Error: ${progress.message}", err = true)
            }
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
