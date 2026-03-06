package ru.agent.cli.repl

import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine

/**
 * Auto-completer for Agent CLI commands.
 *
 * Provides context-aware completion for:
 * - Slash commands (/profile, /task, /memory, /shell)
 * - Command arguments
 * - File paths (for shell commands)
 * - Task IDs
 */
class AgentCompleter : Completer {

    private val commands = mapOf(
        "/" to listOf("profile", "task", "memory", "shell", "help", "clear"),
        "/profile" to listOf("show", "update"),
        "/task" to listOf("list", "status", "create", "cancel", "pause", "resume"),
        "/memory" to listOf("show", "clear", "search")
    )

    override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
        val buffer = line.line().substring(0, line.cursor())

        when {
            // Complete slash commands
            buffer.startsWith("/") && !buffer.contains(" ") -> {
                val partial = buffer.substring(1)
                commands["/"]?.filter { it.startsWith(partial) }?.forEach { cmd ->
                    candidates.add(Candidate("/$cmd"))
                }
            }

            // Complete subcommands
            buffer.contains(" ") -> {
                val parts = buffer.split("\\s+".toRegex())
                val command = parts.first()
                val partial = parts.last()

                commands[command]?.filter { it.startsWith(partial) }?.forEach { subcmd ->
                    candidates.add(Candidate(subcmd))
                }
            }

            // Complete top-level commands
            else -> {
                listOf("help", "exit", "quit", "clear").filter { it.startsWith(buffer) }.forEach { cmd ->
                    candidates.add(Candidate(cmd))
                }
            }
        }
    }
}
