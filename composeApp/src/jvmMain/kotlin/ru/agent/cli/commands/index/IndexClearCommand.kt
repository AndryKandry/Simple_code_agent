package ru.agent.cli.commands.index

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent.getKoin
import ru.agent.features.rag.domain.repository.DocumentIndexRepository
import ru.agent.features.rag.domain.repository.EmbeddingRepository
import ru.agent.features.rag.domain.repository.IndexMetadataRepository

/**
 * Clear command for clearing the document index.
 *
 * Removes all indexed documents, embeddings, and metadata.
 */
class IndexClearCommand : CliktCommand(
    name = "clear",
    help = "Clear the document index"
) {
    private val confirm by option("-y", "--yes", help = "Skip confirmation prompt")
        .flag(default = false)

    private val force by option("-f", "--force", help = "Force clear without any prompts")
        .flag(default = false)

    override fun run() {
        if (!confirm && !force) {
            echo("WARNING: This will delete all indexed documents, embeddings, and metadata.")
            echo("This action cannot be undone.")
            echo("")
            echo("To proceed, use:")
            echo("  agent index clear --yes    (with confirmation)")
            echo("  agent index clear --force  (without confirmation)")
            throw ProgramResult(1)
        }

        runBlocking {
            val chunkRepo: DocumentIndexRepository = getKoin().get()
            val embeddingRepo: EmbeddingRepository = getKoin().get()
            val metadataRepo: IndexMetadataRepository = getKoin().get()

            echo("Clearing document index...")

            try {
                // Get counts before clearing for reporting
                val chunkCount = chunkRepo.getChunkCount()
                val embeddingCount = embeddingRepo.getEmbeddingCount()

                // Clear all data
                chunkRepo.deleteAllChunks()
                embeddingRepo.deleteAllEmbeddings()
                metadataRepo.clear()

                echo("Index cleared successfully.")
                echo("  Deleted $chunkCount chunks")
                echo("  Deleted $embeddingCount embeddings")
            } catch (e: Exception) {
                echo("Error clearing index: ${e.message}", err = true)
                throw ProgramResult(1)
            }
        }
    }
}
