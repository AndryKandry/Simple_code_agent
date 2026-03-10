package ru.agent.cli.repl

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.*
import org.jline.reader.*
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.Terminal as JLineTerminal
import org.jline.terminal.TerminalBuilder
import ru.agent.cli.controller.CliChatController
import ru.agent.cli.controller.CliChatResult
import ru.agent.cli.formatters.OutputFormatter
import ru.agent.features.memory.domain.usecase.GetMemoryContextUseCase
import ru.agent.features.profile.domain.usecase.GetUserProfileUseCase
import java.io.IOException
import kotlin.system.exitProcess

/**
 * REPL (Read-Eval-Print Loop) Controller
 *
 * Provides interactive terminal interface with:
 * - Command history (arrow keys)
 * - Auto-completion (Tab key)
 * - Multi-line input support
 * - Keyboard shortcuts (Ctrl+C cancels input, Ctrl+D or exit/quit to exit)
 * - Task State Machine integration
 * - Memory integration
 */
class ReplController {
    private val terminal = Terminal()
    private lateinit var jlineTerminal: JLineTerminal
    private lateinit var reader: LineReader

    // Chat Controller (will be injected via Koin)
    private val chatController: CliChatController by lazy {
        val koin = org.koin.java.KoinJavaComponent.getKoin()
        CliChatController(
            sendMessageUseCase = koin.get(),
            sendSilentMessageUseCase = koin.get(),
            saveMessageUseCase = koin.get(),
            getChatHistoryUseCase = koin.get(),
            addMessageToMemoryUseCase = koin.get(),
            getTaskStateUseCase = koin.get(),
            createTaskFromMessageUseCase = koin.get(),
            generateTaskPlanUseCase = koin.get(),
            validateTaskResultUseCase = koin.get(),
            transitionTaskStageUseCase = koin.get(),
            updateTaskStateUseCase = koin.get(),
            pauseTaskUseCase = koin.get(),
            resumeTaskUseCase = koin.get(),
            cancelTaskUseCase = koin.get()
        )
    }

    // Additional UseCases for slash commands
    private val getUserProfileUseCase: GetUserProfileUseCase by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }
    private val getMemoryContextUseCase: GetMemoryContextUseCase by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }

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
                    val line = reader.readLine(prompt())

                    if (line == null) {
                        // Ctrl+D was pressed - exit immediately
                        println()
                        break
                    }

                    if (line.isBlank()) continue

                    when (line.trim()) {
                        "exit", "quit" -> break
                        "help" -> showHelp()
                        "clear" -> clearScreen()
                        "status" -> {
                            replScope.launch { showStatus() }
                        }
                        else -> {
                            // Echo user message with label
                            terminal.println()
                            terminal.println("${bold(blue("You:"))} ${line.trim()}")
                            processCommand(line.trim())
                        }
                    }

                } catch (e: UserInterruptException) {
                    // Ctrl+C was pressed - cancel current input, continue
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
        terminal.println(gray("Version 1.0.0 | Chat with AI agent with Task State Machine"))
        terminal.println(gray("Type your message to chat, 'help' for commands, 'exit' to quit"))
        terminal.println()
    }

    /**
     * Generate dynamic prompt based on current state.
     */
    private fun prompt(): String {
        val task = chatController.getCurrentTask()
        return when {
            task?.waitingForUserInput == true -> {
                val stageColor = when (task.taskStage) {
                    ru.agent.features.task.domain.model.TaskStage.PLANNING -> yellow("plan")
                    ru.agent.features.task.domain.model.TaskStage.VALIDATION -> magenta("approve")
                    else -> cyan("task")
                }
                "${bold(stageColor)}${gray(">")} "
            }
            task != null && !task.isCompleted() -> {
                "${bold(blue("task"))}${gray(">")} "
            }
            else -> "${cyan("agent")}${gray(">")} "
        }
    }

    /**
     * Process command entered by user.
     */
    private fun processCommand(input: String) {
        when {
            input.startsWith("/") || input.startsWith("!") -> {
                replScope.launch {
                    try {
                        processSlashCommand(input)
                    } catch (e: Exception) {
                        withContext(Dispatchers.Default) {
                            terminal.println(red("Error: ${e.message}"))
                        }
                    }
                }
            }
            else -> {
                replScope.launch {
                    try {
                        processChatMessage(input)
                    } catch (e: Exception) {
                        withContext(Dispatchers.Default) {
                            terminal.println(red("Error: ${e.message}"))
                        }
                    }
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
            "status" -> showStatus()
            else -> terminal.println(yellow("Unknown command: $cmd. Type 'help' for available commands."))
        }
    }

    /**
     * Send chat message to AI through CliChatController.
     */
    private suspend fun processChatMessage(message: String) {
        terminal.println(gray("Thinking..."))

        val output: (String) -> Unit = { text ->
            terminal.println()
            terminal.print(bold(cyan("Agent")))
            terminal.print(gray(": "))
            terminal.println(text)
        }

        when (val result = chatController.processMessage(message, output)) {
            is CliChatResult.SimpleChat -> {
                // Response already output via callback
            }
            is CliChatResult.TaskCreated -> {
                terminal.println(gray("Task created: ${result.task.taskName}"))
            }
            is CliChatResult.TaskWaitingForApproval -> {
                // Approval prompt already output via callback
            }
            is CliChatResult.TaskCompleted -> {
                // Summary already output via callback
                terminal.println()
                terminal.println(green("✅ Task completed!"))
            }
            is CliChatResult.TaskCancelled -> {
                terminal.println(yellow("Task cancelled."))
            }
            is CliChatResult.Error -> {
                terminal.println(red("Error: ${result.message}"))
            }
            is CliChatResult.Empty -> {
                // Do nothing
            }
        }

        terminal.println()
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
                    terminal.println(yellow("No profile found."))
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
            "status", "" -> {
                val task = chatController.getCurrentTask()
                if (task != null) {
                    terminal.println(OutputFormatter.formatTask(task))
                } else {
                    terminal.println(gray("No active task."))
                }
            }
            "cancel" -> {
                val output: (String) -> Unit = { terminal.println(it) }
                when (chatController.cancelTask(output)) {
                    is CliChatResult.TaskCancelled -> terminal.println(yellow("Task cancelled."))
                    is CliChatResult.Error -> terminal.println(red("Failed to cancel task."))
                    else -> {}
                }
            }
            "pause" -> {
                val output: (String) -> Unit = { terminal.println(it) }
                chatController.pauseTask(output)
            }
            "resume" -> {
                val output: (String) -> Unit = { terminal.println(it) }
                chatController.resumeTask(output)
            }
            else -> terminal.println(yellow("Unknown task command. Use: status, cancel, pause, resume"))
        }
    }

    /**
     * Handle memory commands.
     */
    private suspend fun handleMemoryCommand(args: String) {
        val parts = args.split("\\s+".toRegex())
        when (parts.getOrNull(0)) {
            "show", "" -> {
                val context = getMemoryContextUseCase(CliChatController.CLI_SESSION_ID)
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
     * Show current status (task, memory, profile).
     */
    private suspend fun showStatus() {
        terminal.println()
        terminal.println(bold("=== Current Status ==="))
        terminal.println()

        // Task status
        val task = chatController.getCurrentTask()
        if (task != null) {
            terminal.println(OutputFormatter.formatTaskProgress(task))
        } else {
            terminal.println(gray("No active task."))
        }

        terminal.println()
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
        terminal.println("  ${cyan("status")}           Show current task status")
        terminal.println()
        terminal.println(bold("Slash Commands:"))
        terminal.println()
        terminal.println("  ${cyan("/profile show")}    Show user profile")
        terminal.println("  ${cyan("/task status")}     Show current task status")
        terminal.println("  ${cyan("/task cancel")}     Cancel current task")
        terminal.println("  ${cyan("/task pause")}      Pause current task")
        terminal.println("  ${cyan("/task resume")}     Resume paused task")
        terminal.println("  ${cyan("/memory show")}     Show memory context")
        terminal.println("  ${cyan("/shell <cmd>")}     Execute shell command")
        terminal.println()
        terminal.println(bold("Task Dialog Flow:"))
        terminal.println()
        terminal.println("  When a task is created, you'll be asked to approve the plan.")
        terminal.println("  Type ${green("'approve'")} or ${green("'ok'")} to proceed, or provide feedback to modify.")
        terminal.println("  After execution, you'll be asked to approve the result.")
        terminal.println()
        terminal.println(bold("Chat Mode:"))
        terminal.println()
        terminal.println("  Type any message to chat with AI")
        terminal.println("  Example: \"Create a new Kotlin class for user management\"")
        terminal.println()
        terminal.println(bold("Keyboard Shortcuts:"))
        terminal.println()
        terminal.println("  ${cyan("Ctrl+C")}           Cancel current input")
        terminal.println("  ${cyan("Ctrl+D")}           Exit the application")
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
