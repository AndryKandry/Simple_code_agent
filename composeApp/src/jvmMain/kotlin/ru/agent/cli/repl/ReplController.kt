package ru.agent.cli.repl

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.*
import org.jline.reader.*
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.Terminal as JLineTerminal
import org.jline.terminal.TerminalBuilder
import ru.agent.cli.formatters.OutputFormatter
import ru.agent.features.chat.domain.usecase.SendMessageUseCase
import ru.agent.features.profile.domain.usecase.GetUserProfileUseCase
import ru.agent.features.memory.domain.usecase.GetMemoryContextUseCase
import ru.agent.features.task.domain.usecase.GetTaskStateUseCase
import java.io.IOError
import java.io.IOException
import kotlin.system.exitProcess

/**
 * REPL (Read-Eval-Print Loop) Controller
 *
 * Provides interactive terminal interface with:
 * - Command history (arrow keys)
 * - Auto-completion (Tab key)
 * - Multi-line input support
 * - Keyboard shortcuts (Ctrl+C, Ctrl+D)
 * - Syntax highlighting
 */
class ReplController {
    private val terminal = Terminal()
    private lateinit var jlineTerminal: JLineTerminal
    private lateinit var reader: LineReader

    // UseCases (will be injected via Koin)
    private val sendMessageUseCase: SendMessageUseCase by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }
    private val getUserProfileUseCase: GetUserProfileUseCase by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }
    private val getMemoryContextUseCase: GetMemoryContextUseCase by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }
    private val getTaskStateUseCase: GetTaskStateUseCase by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }

    private val replScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val historyFile = System.getProperty("user.home") + "/.agent_history"

    /**
     * Start the REPL loop.
     */
    fun start() {
        try {
            initializeTerminal()
            showWelcome()

            // Register cleanup callback with shutdown manager
            ru.agent.cli.ShutdownManager.registerCleanup {
                saveHistory()
                closeTerminal()
            }

            while (true) {
                try {
                    val line = readLine()
                    if (line == null) {
                        // Ctrl+D was pressed
                        println()
                        break
                    }

                    if (line.isBlank()) continue

                    processCommand(line.trim())

                } catch (e: UserInterruptException) {
                    // Ctrl+C was pressed
                    println()
                    continue
                } catch (e: EndOfFileException) {
                    // Ctrl+D was pressed
                    println()
                    break
                } catch (e: Exception) {
                    terminal.println(red("Error: ${e.message}"))
                }
            }

            shutdown()

        } catch (e: Exception) {
            terminal.println(red("Fatal error: ${e.message}"))
            exitProcess(1)
        }
    }

    /**
     * Initialize JLine terminal with history and auto-completion.
     */
    private fun initializeTerminal() {
        jlineTerminal = TerminalBuilder.builder()
            .jna(true)
            .system(true)
            .build()

        reader = LineReaderBuilder.builder()
            .terminal(jlineTerminal)
            .history(DefaultHistory())
            .completer(AgentCompleter())
            .parser(InputParser())
            .variable(LineReader.HISTORY_FILE, historyFile)
            .build()

        // Load history
        try {
            reader.history.load()
        } catch (e: IOException) {
            // History file doesn't exist yet, ignore
        }
    }

    /**
     * Display welcome message.
     */
    private fun showWelcome() {
        terminal.println()
        terminal.println(bold(green("Simple Code Agent CLI")))
        terminal.println(gray("Version 1.0.0 | Type 'help' for available commands"))
        terminal.println()
    }

    /**
     * Read line from terminal with prompt.
     */
    private fun readLine(): String? {
        return try {
            reader.readLine(prompt())
        } catch (e: IOError) {
            null
        }
    }

    /**
     * Generate dynamic prompt based on current state.
     */
    private fun prompt(): String {
        return "${cyan("agent")}${gray(">")} "
    }

    /**
     * Process command entered by user.
     */
    private fun processCommand(input: String) {
        replScope.launch {
            try {
                when {
                    input.startsWith("/") || input.startsWith("!") -> processSlashCommand(input)
                    input == "exit" || input == "quit" -> {
                        shutdown()
                        exitProcess(0)
                    }
                    input == "help" -> showHelp()
                    input == "clear" -> clearScreen()
                    else -> processChatMessage(input)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Default) {
                    terminal.println(red("Error: ${e.message}"))
                }
            }
        }
    }

    /**
     * Process slash commands (e.g., /profile, /task, /shell).
     */
    private suspend fun processSlashCommand(command: String) {
        val parts = command.substring(1).split("\\s+".toRegex(), 2)
        val cmd = parts[0]
        val args = parts.getOrNull(1) ?: ""

        when (cmd) {
            "profile" -> handleProfileCommand(args)
            "task" -> handleTaskCommand(args)
            "memory" -> handleMemoryCommand(args)
            "shell" -> handleShellCommand(args)
            "help" -> showHelp()
            "clear" -> clearScreen()
            else -> terminal.println(yellow("Unknown command: $cmd. Type 'help' for available commands."))
        }
    }

    /**
     * Send chat message to AI.
     */
    private suspend fun processChatMessage(message: String) {
        terminal.println(gray("Thinking..."))

        try {
            val result = sendMessageUseCase("default", message)

            when (result) {
                is ru.agent.common.wrappers.ResultWrapper.Success -> {
                    val response = result.value
                    terminal.println()
                    terminal.println(OutputFormatter.formatMessage(response))
                    terminal.println()
                }
                is ru.agent.common.wrappers.ResultWrapper.Error -> {
                    terminal.println(red("Error: ${result.message}"))
                }
            }
        } catch (e: Exception) {
            terminal.println(red("Failed to send message: ${e.message}"))
        }
    }

    /**
     * Handle profile commands.
     */
    private suspend fun handleProfileCommand(args: String) {
        val parts = args.split("\\s+".toRegex())
        when (parts.getOrNull(0)) {
            "show", "" -> {
                val profile = getUserProfileUseCase()
                if (profile != null) {
                    terminal.println(OutputFormatter.formatProfile(profile))
                } else {
                    terminal.println(yellow("No profile found. Creating default profile..."))
                }
            }
            "update" -> terminal.println(blue("Profile update not implemented yet"))
            else -> terminal.println(yellow("Unknown profile command. Use: show, update"))
        }
    }

    /**
     * Handle task commands.
     */
    private suspend fun handleTaskCommand(args: String) {
        val parts = args.split("\\s+".toRegex())
        when (parts.getOrNull(0)) {
            "list", "" -> {
                terminal.println(blue("Task list not implemented yet"))
            }
            "status" -> {
                val taskId = parts.getOrNull(1)
                if (taskId != null) {
                    val task = getTaskStateUseCase(taskId)
                    if (task != null) {
                        terminal.println(OutputFormatter.formatTask(task))
                    } else {
                        terminal.println(yellow("Task not found: $taskId"))
                    }
                } else {
                    terminal.println(yellow("Usage: /task status <task-id>"))
                }
            }
            else -> terminal.println(yellow("Unknown task command. Use: list, status"))
        }
    }

    /**
     * Handle memory commands.
     */
    private suspend fun handleMemoryCommand(args: String) {
        val parts = args.split("\\s+".toRegex())
        when (parts.getOrNull(0)) {
            "show", "" -> {
                val context = getMemoryContextUseCase("default")
                terminal.println(OutputFormatter.formatMemory(context))
            }
            "clear" -> terminal.println(blue("Memory clear not implemented yet"))
            else -> terminal.println(yellow("Unknown memory command. Use: show, clear"))
        }
    }

    /**
     * Handle shell commands (execute system commands).
     */
    private suspend fun handleShellCommand(args: String) {
        if (args.isBlank()) {
            terminal.println(yellow("Usage: /shell <command>"))
            return
        }

        try {
            val process = ProcessBuilder(args.split("\\s+".toRegex()))
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()

            if (output.isNotEmpty()) {
                terminal.println(output)
            }
            if (error.isNotEmpty()) {
                terminal.println(red(error))
            }

        } catch (e: Exception) {
            terminal.println(red("Failed to execute command: ${e.message}"))
        }
    }

    /**
     * Show help message.
     */
    private fun showHelp() {
        terminal.println()
        terminal.println(bold("Available Commands:"))
        terminal.println()
        terminal.println("  ${cyan("help")}              Show this help message")
        terminal.println("  ${cyan("exit, quit")}       Exit the application")
        terminal.println("  ${cyan("clear")}            Clear the screen")
        terminal.println()
        terminal.println(bold("Slash Commands:"))
        terminal.println()
        terminal.println("  ${cyan("/profile show")}    Show user profile")
        terminal.println("  ${cyan("/task list")}       List all tasks")
        terminal.println("  ${cyan("/task status <id>")} Show task status")
        terminal.println("  ${cyan("/memory show")}     Show memory context")
        terminal.println("  ${cyan("/shell <cmd>")}     Execute shell command")
        terminal.println()
        terminal.println(bold("Chat Mode:"))
        terminal.println()
        terminal.println("  Type any message to chat with AI")
        terminal.println("  Example: \"Create a new Kotlin class for user management\"")
        terminal.println()
    }

    /**
     * Clear the terminal screen.
     */
    private fun clearScreen() {
        terminal.print("\u001b[2J\u001b[H")
    }

    /**
     * Shutdown REPL gracefully.
     */
    private fun shutdown() {
        replScope.cancel()
        saveHistory()
        closeTerminal()
        terminal.println(gray("Goodbye!"))
    }

    /**
     * Save command history to file.
     */
    private fun saveHistory() {
        try {
            reader.history.save()
        } catch (e: IOException) {
            // Ignore save errors
        }
    }

    /**
     * Close terminal resources.
     */
    private fun closeTerminal() {
        try {
            jlineTerminal.close()
        } catch (e: Exception) {
            // Ignore close errors
        }
    }
}
