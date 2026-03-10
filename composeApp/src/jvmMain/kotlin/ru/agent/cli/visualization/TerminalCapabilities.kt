package ru.agent.cli.visualization

/**
 * Detects terminal capabilities for graceful degradation.
 *
 * Determines:
 * - ANSI color support
 * - Unicode support
 * - TTY status (interactive terminal)
 */
object TerminalCapabilities {

    /**
     * Check if terminal supports ANSI escape codes.
     */
    fun supportsAnsiColors(): Boolean {
        val term = System.getenv("TERM") ?: ""
        val colorterm = System.getenv("COLORTERM") ?: ""

        // Check explicit color support
        if (colorterm.isNotEmpty()) return true

        // Check known terminals
        val colorTerminals = listOf(
            "xterm", "xterm-256color", "screen", "screen-256color",
            "vt100", "ansi", "linux", "cygwin", "rxvt"
        )

        return colorTerminals.any { term.startsWith(it, ignoreCase = true) }
    }

    /**
     * Check if terminal supports Unicode characters.
     */
    fun supportsUnicode(): Boolean {
        val lang = System.getenv("LANG") ?: ""
        val lcAll = System.getenv("LC_ALL") ?: ""
        val term = System.getenv("TERM") ?: ""

        // Check for UTF-8 encoding
        val utf8Patterns = listOf("UTF-8", "utf8", "UTF8")

        val hasUtf8 = utf8Patterns.any { pattern ->
            lang.contains(pattern, ignoreCase = true) ||
            lcAll.contains(pattern, ignoreCase = true)
        }

        // Known terminals with good Unicode support
        val unicodeTerminals = listOf(
            "xterm-256color", "screen-256color", "iterm", "iTerm",
            "apple-terminal", "gnome-terminal", "konsole"
        )

        return hasUtf8 || unicodeTerminals.any { term.contains(it, ignoreCase = true) }
    }

    /**
     * Check if running in an interactive terminal (TTY).
     */
    fun isInteractive(): Boolean {
        return System.console() != null
    }

    /**
     * Get terminal width in columns.
     */
    fun getTerminalWidth(): Int {
        return try {
            // Try to get from environment
            System.getenv("COLUMNS")?.toIntOrNull() ?: 80
        } catch (e: Exception) {
            80
        }
    }

    /**
     * Get terminal height in rows.
     */
    fun getTerminalHeight(): Int {
        return try {
            System.getenv("LINES")?.toIntOrNull() ?: 24
        } catch (e: Exception) {
            24
        }
    }

    /**
     * Get complete capabilities snapshot.
     */
    fun getCapabilities(): Capabilities = Capabilities(
        ansiColors = supportsAnsiColors(),
        unicode = supportsUnicode(),
        interactive = isInteractive(),
        width = getTerminalWidth(),
        height = getTerminalHeight()
    )

    /**
     * Terminal capabilities snapshot.
     */
    data class Capabilities(
        val ansiColors: Boolean,
        val unicode: Boolean,
        val interactive: Boolean,
        val width: Int,
        val height: Int
    ) {
        /**
         * Get spinner characters based on Unicode support.
         */
        fun getSpinnerChars(): List<String> = if (unicode) {
            listOf("⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏")
        } else {
            listOf("|", "/", "-", "\\", "|", "/", "-", "\\")
        }

        /**
         * Get progress bar characters based on Unicode support.
         */
        fun getProgressChars(): ProgressChars = if (unicode) {
            ProgressChars(
                filled = "█",
                empty = "░",
                partial = listOf("▏", "▎", "▍", "▌", "▋", "▊", "▉")
            )
        } else {
            ProgressChars(
                filled = "#",
                empty = "-",
                partial = listOf("-", "-", "-", "-", "-", "-", "-")
            )
        }

        /**
         * Get success/error icons based on Unicode support.
         */
        fun getStatusIcons(): StatusIcons = if (unicode) {
            StatusIcons(
                success = "✓",
                error = "✗",
                warning = "⚠",
                info = "ℹ"
            )
        } else {
            StatusIcons(
                success = "[OK]",
                error = "[ERR]",
                warning = "[!]",
                info = "[i]"
            )
        }
    }

    /**
     * Progress bar characters.
     */
    data class ProgressChars(
        val filled: String,
        val empty: String,
        val partial: List<String>
    )

    /**
     * Status icons.
     */
    data class StatusIcons(
        val success: String,
        val error: String,
        val warning: String,
        val info: String
    )
}
