#!/usr/bin/java.Kt
package ru.agent.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.terminal.Terminal
import ru.agent.cli.commands.ChatCommand
import ru.agent.cli.commands.McpCommand
import ru.agent.cli.commands.MemoryCommand
import ru.agent.cli.commands.ProfileCommand
import ru.agent.cli.commands.ShellCommand
import ru.agent.cli.commands.TaskCommand
import ru.agent.cli.commands.invariant.InvariantCommand
import ru.agent.cli.commands.scheduler.SchedulerCommand
import ru.agent.cli.repl.ReplController

/**
 * Main CLI application entry point.
 *
 * Provides two modes:
 * - Interactive REPL mode (default): Full interactive terminal interface
 * - Command mode: Direct execution of specific commands
 *
 * Usage:
 * ```
 * # Start interactive REPL
 * java -jar app.jar
 *
 * # Execute specific command
 * java -jar app.jar chat "Hello, AI!"
 * java -jar app.jar profile show
 * java -jar app.jar task list
 * java -jar app.jar shell ls -la
 * ```
 */
class CliApp : CliktCommand(
    name = "agent",
    invokeWithoutSubcommand = true,  // Run parent command even without subcommand
    help = """
        Simple Code Agent CLI

        AI-powered coding assistant with chat, task management, and shell integration.

        Examples:
          agent                  Start interactive REPL mode
          agent chat "Hello"     Send a chat message
          agent profile show     Display user profile
          agent task list        List all tasks
          agent shell ls -la     Execute shell command
    """.trimIndent()
) {
    private val interactive by option(
        "-i", "--interactive",
        help = "Start interactive REPL mode (default when no command specified)"
    ).flag(default = false)

    override fun run() {
        // Koin is already initialized in main.kt

        // If no subcommand and not explicitly interactive, start REPL
        if (currentContext.invokedSubcommand == null) {
            ReplController().start()
        }
    }

    companion object {
        /**
         * Create configured CliApp instance with all subcommands registered.
         */
        fun create(): CliApp {
            return CliApp().subcommands(
                ChatCommand(),
                ProfileCommand(),
                MemoryCommand(),
                TaskCommand(),
                ShellCommand(),
                InvariantCommand(),
                McpCommand(),
                SchedulerCommand()
            )
        }
    }
}

/**
 * Application shutdown manager for graceful cleanup.
 */
object ShutdownManager {
    private var isShuttingDown = false
    private val cleanupCallbacks = mutableListOf<() -> Unit>()

    /**
     * Register a cleanup callback to be executed on shutdown.
     */
    fun registerCleanup(callback: () -> Unit) {
        cleanupCallbacks.add(callback)
    }

    /**
     * Perform graceful shutdown.
     */
    fun performShutdown() {
        if (isShuttingDown) return
        isShuttingDown = true

        val terminal = Terminal()
        terminal.println(gray("\nShutting down gracefully..."))

        // Execute all cleanup callbacks
        cleanupCallbacks.forEach { callback ->
            try {
                callback()
            } catch (e: Exception) {
                terminal.println(red("Error during cleanup: ${e.message}"))
            }
        }

        terminal.println(gray("Goodbye!"))
    }

    /**
     * Setup JVM shutdown hook for graceful cleanup on Ctrl+C.
     */
    fun setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread {
            performShutdown()
        })
    }
}