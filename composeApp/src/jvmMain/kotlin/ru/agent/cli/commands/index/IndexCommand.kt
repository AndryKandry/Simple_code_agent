package ru.agent.cli.commands.index

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

/**
 * Index command group for RAG (Retrieval-Augmented Generation) document indexing.
 *
 * Usage:
 * ```
 * agent index run                    # Fixed-size chunking
 * agent index run --strategy semantic  # Structure-based chunking
 * agent index run --rebuild           # Rebuild from scratch
 * agent index stats                   # Show statistics
 * agent index compare                 # Compare strategies
 * agent index clear --yes             # Clear index
 * ```
 */
class IndexCommand : CliktCommand(
    name = "index",
    help = "Document indexing for RAG (Retrieval-Augmented Generation)"
) {
    init {
        subcommands(
            IndexRunCommand(),
            IndexStatsCommand(),
            IndexCompareCommand(),
            IndexClearCommand()
        )
    }

    override fun run() {
        if (currentContext.invokedSubcommand == null) {
            echo("Use --help to see available subcommands: run, stats, compare, clear")
        }
    }
}
