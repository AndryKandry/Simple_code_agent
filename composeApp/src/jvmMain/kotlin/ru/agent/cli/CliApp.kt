#!/usr/bin/java.Kt
package ru.agent.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.*
import org.jline.reader.*
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.Terminal as JLineTerminal
import org.jline.terminal.TerminalBuilder
import ru.agent.cli.formatters.OutputFormatter
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.features.chat.domain.usecase.SendMessageUseCase
import ru.agent.features.chat.domain.usecase.GetChatHistoryUseCase
import ru.agent.features.chat.domain.usecase.ClearChatHistoryUseCase
import ru.agent.features.profile.domain.usecase.GetUserProfileUseCase
import ru.agent.features.profile.domain.usecase.UpdateUserProfileUseCase
import ru.agent.features.profile.domain.usecase.CreateDefaultProfileUseCase
import ru.agent.features.memory.domain.usecase.GetMemoryContextUseCase
import ru.agent.features.memory.domain.usecase.ClearShortTermMemoryUseCase
import ru.agent.features.task.domain.usecase.*
import ru.agent.cli.commands.TaskCommand
import ru.agent.cli.commands.ShellCommand
import ru.agent.cli.commands.ChatCommand
import ru.agent.cli.commands.ProfileCommand
import ru.agent.cli.commands.MemoryCommand
import ru.agent.cli.repl.ReplController
import ru.agent.core.di.initKoin
import java.io.IOException
import kotlin.system.exitProcess

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
        // Initialize Koin DI
        initKoin()

        // If no subcommand and not explicitly interactive, start REPL
        if (currentContext.invokedSubcommand == null) {
            ReplController().start()
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

/**
 * Main entry point for CLI application.
 */
fun main(args: Array<String>) {
    // Setup graceful shutdown handler
    ShutdownManager.setupShutdownHook()

    val app = CliApp().subcommands(
        ChatCommand(),
        ProfileCommand(),
        MemoryCommand(),
        TaskCommand(),
        ShellCommand()
    )

    try {
        app.main(args)
    } catch (e: Exception) {
        System.exit(1)
    }
}