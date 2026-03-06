package ru.agent.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Shell command for executing system commands.
 *
 * Supports common shell commands:
 * - ls, dir, pwd, cd, cat, find, grep
 *
 * Usage:
 * ```
 * agent shell ls -la
 * agent shell pwd
 * agent shell cat build.gradle.kts
 * ```
 */
class ShellCommand : CliktCommand(
    name = "shell",
    help = "Execute shell commands"
) {
    private val command by argument(help = "Command to execute").multiple(required = true)

    private val terminal = Terminal()

    companion object {
        /**
         * Whitelist of allowed commands that can be executed.
         * Only these commands are permitted for security reasons.
         */
        private val ALLOWED_COMMANDS = setOf(
            // File system navigation
            "ls", "dir", "pwd", "cd", "tree",
            // File reading
            "cat", "head", "tail", "less", "more",
            // File search
            "find", "locate", "whereis", "which",
            // Text search
            "grep", "egrep", "fgrep", "rg",
            // File information
            "stat", "file", "du", "df",
            // Process information
            "ps", "top", "htop",
            // Network information (safe read-only)
            "whoami", "hostname", "uname", "date", "echo"
        )

        /**
         * Blacklist of dangerous patterns that should never be allowed.
         * These patterns indicate potentially destructive operations.
         */
        private val DANGEROUS_PATTERNS = listOf(
            // File deletion
            "rm\\s+(-[rf]+|.*-rf)".toRegex(RegexOption.IGNORE_CASE),
            "\\brm\\s+-.*[rf]".toRegex(RegexOption.IGNORE_CASE),
            // Privilege escalation
            "\\bsudo\\b".toRegex(RegexOption.IGNORE_CASE),
            "\\bsu\\b".toRegex(RegexOption.IGNORE_CASE),
            // Permission changes
            "\\bchmod\\s+(777|a\\+rwx|u\\+rwx)".toRegex(RegexOption.IGNORE_CASE),
            "\\bchown\\b".toRegex(RegexOption.IGNORE_CASE),
            // System modification
            "\\bmkfs\\b".toRegex(RegexOption.IGNORE_CASE),
            "\\bdd\\b.*\\bof=".toRegex(RegexOption.IGNORE_CASE),
            "\\bformat\\b".toRegex(RegexOption.IGNORE_CASE),
            // Network dangerous
            "\\biptables\\b".toRegex(RegexOption.IGNORE_CASE),
            "\\bnetstat\\b".toRegex(RegexOption.IGNORE_CASE),
            // Process control
            "\\bkill(-9| -9|all)\\b".toRegex(RegexOption.IGNORE_CASE),
            "\\bpkill\\b".toRegex(RegexOption.IGNORE_CASE),
            // Shell injection
            ";\\s*rm".toRegex(RegexOption.IGNORE_CASE),
            "\\|\\s*rm".toRegex(RegexOption.IGNORE_CASE),
            "`.*rm.*`".toRegex(RegexOption.IGNORE_CASE),
            "\\$\\(.*rm.*\\)".toRegex(RegexOption.IGNORE_CASE)
        )

        /**
         * Validate if a command is safe to execute.
         *
         * @param command The command to validate
         * @return Pair of (isSafe, errorMessage)
         */
        fun validateCommand(command: String): Pair<Boolean, String> {
            val trimmedCommand = command.trim()
            val commandParts = trimmedCommand.split("\\s+".toRegex())
            val baseCommand = commandParts.firstOrNull()?.lowercase() ?: ""

            // Check if base command is in whitelist
            if (baseCommand !in ALLOWED_COMMANDS) {
                return Pair(
                    false,
                    "Command '$baseCommand' is not in the allowed list. " +
                    "Allowed commands: ${ALLOWED_COMMANDS.sorted().joinToString(", ")}"
                )
            }

            // Check for dangerous patterns
            for (pattern in DANGEROUS_PATTERNS) {
                if (pattern.containsMatchIn(trimmedCommand)) {
                    return Pair(
                        false,
                        "Command contains potentially dangerous pattern. " +
                        "For security reasons, this operation is not allowed."
                    )
                }
            }

            // Check for shell injection attempts
            val shellInjectionChars = arrayOf(";", "|", "&", "`", "$(", ">", ">>")
            for (injectionChar in shellInjectionChars) {
                if (injectionChar in trimmedCommand) {
                    return Pair(
                        false,
                        "Shell injection detected. Command chaining is not allowed for security reasons."
                    )
                }
            }

            return Pair(true, "")
        }
    }

    override fun run() {
        val fullCommand = command.joinToString(" ")

        when (val cmd = command.first()) {
            "ls", "dir" -> executeLs()
            "pwd" -> executePwd()
            "cd" -> executeCd()
            "cat" -> executeCat()
            "find" -> executeFind()
            "grep" -> executeGrep()
            else -> executeSystemCommand(fullCommand)
        }
    }

    /**
     * List directory contents (cross-platform).
     */
    private fun executeLs() {
        val path = if (command.size > 1) command[1] else "."
        val dir = File(path)

        if (!dir.exists()) {
            terminal.println("Directory not found: $path")
            return
        }

        if (!dir.isDirectory) {
            terminal.println("Not a directory: $path")
            return
        }

        val files = dir.listFiles()
        if (files != null) {
            files.sorted().forEach { file ->
                val prefix = if (file.isDirectory) "d " else "f "
                val name = if (file.isDirectory) "\u001B[34m${file.name}\u001B[0m" else file.name
                terminal.println("$prefix$name")
            }
        }
    }

    /**
     * Print working directory.
     */
    private fun executePwd() {
        terminal.println(File("").absolutePath)
    }

    /**
     * Change directory.
     */
    private fun executeCd() {
        if (command.size < 2) {
            terminal.println("Usage: cd <directory>")
            return
        }

        val path = command[1]
        val dir = File(path)

        if (!dir.exists()) {
            terminal.println("Directory not found: $path")
            return
        }

        if (!dir.isDirectory) {
            terminal.println("Not a directory: $path")
            return
        }

        System.setProperty("user.dir", dir.absolutePath)
        terminal.println("Changed to: ${dir.absolutePath}")
    }

    /**
     * Display file contents.
     */
    private fun executeCat() {
        if (command.size < 2) {
            terminal.println("Usage: cat <file>")
            return
        }

        val path = command[1]
        val file = File(path)

        if (!file.exists()) {
            terminal.println("File not found: $path")
            return
        }

        if (!file.isFile) {
            terminal.println("Not a file: $path")
            return
        }

        terminal.println(file.readText())
    }

    /**
     * Find files by pattern.
     */
    private fun executeFind() {
        if (command.size < 2) {
            terminal.println("Usage: find <pattern>")
            return
        }

        val pattern = command[1].replace("*", ".*").toRegex()
        val startDir = if (command.size > 2) File(command[2]) else File(".")

        findFiles(startDir, pattern).forEach { file ->
            terminal.println(file.relativeTo(File("")).path)
        }
    }

    /**
     * Grep pattern in files.
     */
    private fun executeGrep() {
        if (command.size < 2) {
            terminal.println("Usage: grep <pattern> <file>")
            return
        }

        val pattern = command[1].toRegex()
        val filePath = if (command.size > 2) command[2] else "."

        val file = File(filePath)
        if (!file.exists()) {
            terminal.println("File not found: $filePath")
            return
        }

        file.readLines().forEachIndexed { index, line ->
            if (pattern.containsMatchIn(line)) {
                terminal.println("${index + 1}: $line")
            }
        }
    }

    /**
     * Execute arbitrary system command with security validation.
     */
    private fun executeSystemCommand(fullCommand: String) {
        // Validate command against whitelist and blacklist
        val (isSafe, errorMessage) = validateCommand(fullCommand)

        if (!isSafe) {
            terminal.println("\u001B[31mSecurity Error: $errorMessage\u001B[0m")
            return
        }

        try {
            val process = ProcessBuilder(fullCommand.split("\\s+".toRegex()))
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()

            if (output.isNotEmpty()) {
                terminal.println(output)
            }
            if (error.isNotEmpty()) {
                terminal.println("\u001B[31m$error\u001B[0m")
            }

        } catch (e: Exception) {
            terminal.println("Failed to execute command: ${e.message}")
        }
    }

    /**
     * Recursively find files matching pattern.
     */
    private fun findFiles(dir: File, pattern: Regex): List<File> {
        val result = mutableListOf<File>()

        dir.walk().forEach { file ->
            if (pattern.matches(file.name)) {
                result.add(file)
            }
        }

        return result
    }
}
