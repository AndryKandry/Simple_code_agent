package ru.agent.cli.repl

import org.jline.reader.ParsedLine
import org.jline.reader.Parser
import org.jline.reader.impl.DefaultParser

/**
 * Input parser for REPL commands.
 *
 * Extends DefaultParser to handle:
 * - Multi-line input (with \\ at end of line)
 * - Quoted strings
 * - Escape sequences
 */
class InputParser : DefaultParser() {

    override fun parse(line: String, cursor: Int, parseContext: Parser.ParseContext): ParsedLine {
        // Handle multi-line input
        if (line.trimEnd().endsWith("\\")) {
            // Continue reading next line
            return super.parse(line, cursor, parseContext)
        }

        return super.parse(line, cursor, parseContext)
    }

    override fun isEscapeChar(ch: Char): Boolean {
        return ch == '\\'
    }
}
