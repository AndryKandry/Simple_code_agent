package ru.agent.cli.commands.index

import com.github.ajalt.clikt.core.CliktCommand

/**
 * Compare command for comparing chunking strategies.
 *
 * Shows a comparison of fixed-size vs structure-based chunking strategies.
 */
class IndexCompareCommand : CliktCommand(
    name = "compare",
    help = "Compare chunking strategies"
) {
    override fun run() {
        echo("=== Chunking Strategy Comparison ===")
        echo("")
        echo("FIXED-SIZE STRATEGY")
        echo("  - Consistent chunk sizes (~500 tokens)")
        echo("  - Faster processing")
        echo("  - Predictable memory usage")
        echo("  - May split code structures mid-function")
        echo("  - Best for: Quick indexing, uniform content")
        echo("")
        echo("STRUCTURE-BASED STRATEGY")
        echo("  - Preserves code blocks and functions")
        echo("  - Variable chunk sizes (100-1000+ tokens)")
        echo("  - Better semantic coherence")
        echo("  - Slower processing (requires parsing)")
        echo("  - Best for: Code search, semantic queries")
        echo("")
        echo("=== Comparison Table ===")
        echo("")
        echo("| Aspect           | Fixed-Size | Structure-Based |")
        echo("|------------------|------------|-----------------|")
        echo("| Speed            | Fast       | Moderate        |")
        echo("| Chunk size       | Consistent | Variable        |")
        echo("| Code coherence   | Low        | High            |")
        echo("| Search quality   | Good       | Better          |")
        echo("| Memory usage     | Predictable| Variable        |")
        echo("")
        echo("=== How to Compare ===")
        echo("")
        echo("To compare actual results on your project:")
        echo("")
        echo("  1. Index with fixed-size strategy:")
        echo("     agent index run --strategy fixed")
        echo("     agent index stats")
        echo("")
        echo("  2. Rebuild with structure-based strategy:")
        echo("     agent index run --strategy semantic --rebuild")
        echo("     agent index stats")
        echo("")
        echo("  3. Compare the statistics (avg tokens, std dev, etc.)")
        echo("")
        echo("=== Recommendation ===")
        echo("")
        echo("For code projects: Use STRUCTURE-BASED (semantic)")
        echo("For documentation: Use FIXED-SIZE")
        echo("For mixed content: Try both and compare search results")
    }
}
