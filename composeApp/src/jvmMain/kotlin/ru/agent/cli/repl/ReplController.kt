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
import ru.agent.cli.visualization.CliAnimator
import ru.agent.cli.visualization.domain.ProgressState
import ru.agent.features.memory.domain.usecase.GetMemoryContextUseCase
import ru.agent.features.profile.domain.usecase.GetUserProfileUseCase
import ru.agent.features.task.domain.repository.TaskStateRepository
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
 * - Enhanced prompt with progress indicators
 * - Real-time progress visualization
 */
class ReplController {
    private val terminal = Terminal()
    private lateinit var jlineTerminal: JLineTerminal
    private lateinit var reader: LineReader

    // Chat Controller (injected via Koin singleton)
    private val chatController: CliChatController by lazy {
        org.koin.java.KoinJavaComponent.getKoin().get()
    }

    // CLI Animator (injected via Koin singleton)
    private val cliAnimator: CliAnimator by lazy {
        org.koin.java.KoinJavaComponent.getKoin().get()
    }

    // Additional UseCases for slash commands
    private val getUserProfileUseCase: GetUserProfileUseCase by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }
    private val getMemoryContextUseCase: GetMemoryContextUseCase by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }

    // Repository for cleanup
    private val taskStateRepository: TaskStateRepository by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }

    private val replScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val historyFile = System.getProperty("user.home") + "/.agent_history"

    // Progress monitoring job
    private var progressMonitorJob: Job? = null

    /**
     * Start the REPL loop.
     */
    fun start() {
        try {
            initializeTerminal()

            // Clean up stale tasks from previous sessions
            replScope.launch {
                try {
                    taskStateRepository.deleteCompletedTasksForSession(CliChatController.CLI_SESSION_ID)
                } catch (e: Exception) {
                    terminal.println(yellow("Warning: Could not clean up old tasks: ${e.message}"))
                }
            }

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
                            // Clean the input from potential terminal artifacts
                            val cleanLine = cleanTerminalInput(line)
                            // Echo user message with label using JLine writer
                            val writer = jlineTerminal.writer()
                            writer.println()
                            writer.println("\u001B[1;34mYou:\u001B[0m $cleanLine")
                            writer.flush()
                            processCommand(cleanLine)
                        }
                    }

                } catch (e: UserInterruptException) {
                    // Ctrl+C was pressed
                    if (chatController.hasActiveTask() || chatController.getProgressState().value is ProgressState.InProgress) {
                        // Interrupt active operation
                        replScope.launch {
                            val output: (String) -> Unit = { terminal.println(it) }
                            chatController.handleInterrupt(output)
                        }
                    } else {
                        // Cancel current input, continue
                        println()
                        continue
                    }
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
            .encoding(Charsets.UTF_8)
            .build()

        reader = LineReaderBuilder.builder()
            .terminal(jlineTerminal)
            .history(DefaultHistory())
            .completer(AgentCompleter())
            .parser(InputParser())
            .variable(LineReader.HISTORY_FILE, historyFile)
            .variable(LineReader.EDITING_MODE, "emacs")
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
     * Includes validation indicator and progress information.
     */
    private fun prompt(): String {
        val task = chatController.getCurrentTask()
        val progressState = chatController.getProgressState().value

        // Enhanced prompt with progress
        return when {
            // Show progress during active operations
            progressState is ProgressState.InProgress -> {
                val percentage = progressState.getClampedPercentage()
                val stepInfo = progressState.getStepInfo()
                val stepText = stepInfo?.let { " [$it]" } ?: ""

                "${bold(blue("task"))}${gray("[$percentage%$stepText]")} "
            }

            // Show waiting state
            task?.waitingForUserInput == true -> {
                val stageColor = when (task.taskStage) {
                    ru.agent.features.task.domain.model.TaskStage.PLANNING -> yellow("plan")
                    ru.agent.features.task.domain.model.TaskStage.VALIDATION -> magenta("approve")
                    else -> cyan("task")
                }
                "${bold(stageColor)}${gray(">")} "
            }

            // Show active task with progress
            task != null && !task.isCompleted() -> {
                val progress = task.planProgressPercentage()
                val warningIndicator = if (chatController.wasInterrupted()) {
                    yellow(" ⚠")
                } else {
                    ""
                }
                "${bold(blue("task"))}${gray("[$progress%$warningIndicator]")} "
            }

            // Default prompt
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
            "invariant", "inv" -> handleInvariantCommand(args)
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
        val output: (String) -> Unit = { text ->
            terminal.println()
            terminal.print(bold(cyan("Agent")))
            terminal.print(gray(": "))
            terminal.println(text)
        }

        when (val result = chatController.processMessage(message, output)) {
            is CliChatResult.WithWarnings -> {
                // Display warnings first
                if (result.warnings.isNotEmpty()) {
                    terminal.println()
                    terminal.println(ru.agent.cli.formatters.UserMessageFormatter.formatValidationWarnings(result.warnings))
                    terminal.println()
                }
                // Then handle base result
                handleChatResult(result.baseResult)
            }
            else -> handleChatResult(result)
        }

        terminal.println()
    }

    /**
     * Handle chat result with optional warnings display.
     */
    private suspend fun handleChatResult(result: CliChatResult) {
        when (result) {
            is CliChatResult.SimpleChat -> {
                // Display warnings if present
                if (result.warnings.isNotEmpty()) {
                    terminal.println()
                    terminal.println(ru.agent.cli.formatters.UserMessageFormatter.formatValidationWarnings(result.warnings))
                    terminal.println()
                }
                // Response already output via callback
            }
            is CliChatResult.TaskCreated -> {
                terminal.println(gray("Task created: ${result.task.taskName}"))
            }
            is CliChatResult.TaskWaitingForApproval -> {
                // Display validation warnings if present
                if (result.warnings.isNotEmpty()) {
                    terminal.println()
                    terminal.println(ru.agent.cli.formatters.UserMessageFormatter.formatValidationWarnings(result.warnings))
                }
                // Display transition warnings if present
                if (result.transitionWarnings.isNotEmpty()) {
                    terminal.println()
                    terminal.println(ru.agent.cli.formatters.UserMessageFormatter.formatTransitionWarnings(result.transitionWarnings))
                }
                if (result.warnings.isNotEmpty() || result.transitionWarnings.isNotEmpty()) {
                    terminal.println()
                }
                // Approval prompt already output via callback
            }
            is CliChatResult.TaskCompleted -> {
                // Display transition warnings if present
                if (result.transitionWarnings.isNotEmpty()) {
                    terminal.println()
                    terminal.println(ru.agent.cli.formatters.UserMessageFormatter.formatTransitionWarnings(result.transitionWarnings))
                    terminal.println()
                }
                // Summary already output via callback
                terminal.println()
                terminal.println(green("Task completed!"))
            }
            is CliChatResult.TaskCancelled -> {
                terminal.println(yellow("Task cancelled."))
            }
            is CliChatResult.TaskInterrupted -> {
                terminal.println(yellow("Task interrupted."))
                if (result.canResume) {
                    terminal.println(gray("  Tip: You can resume this task"))
                }
            }
            is CliChatResult.Error -> {
                // Display error message
                if (result.message.isNotEmpty()) {
                    terminal.println(red(result.message))
                }
                // Display suggestions if present
                if (result.suggestions.isNotEmpty()) {
                    terminal.println()
                    terminal.println(ru.agent.cli.formatters.UserMessageFormatter.formatSuggestions(result.suggestions))
                }
            }
            is CliChatResult.Empty -> {
                // Do nothing
            }
            is CliChatResult.WithWarnings -> {
                // This case should not occur (handled above), but safe fallback
                handleChatResult(result.baseResult)
            }
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
     * Handle invariant commands.
     */
    private suspend fun handleInvariantCommand(args: String) {
        val getInvariantsUseCase: ru.agent.features.invariant.domain.usecase.GetInvariantsUseCase by lazy {
            org.koin.java.KoinJavaComponent.getKoin().get()
        }
        val addInvariantUseCase: ru.agent.features.invariant.domain.usecase.AddInvariantUseCase by lazy {
            org.koin.java.KoinJavaComponent.getKoin().get()
        }
        val toggleInvariantUseCase: ru.agent.features.invariant.domain.usecase.ToggleInvariantUseCase by lazy {
            org.koin.java.KoinJavaComponent.getKoin().get()
        }
        val removeInvariantUseCase: ru.agent.features.invariant.domain.usecase.RemoveInvariantUseCase by lazy {
            org.koin.java.KoinJavaComponent.getKoin().get()
        }

        val parts = args.trim().split("\\s+".toRegex(), 2)
        val subCommand = parts.getOrNull(0) ?: "list"
        val subArgs = parts.getOrNull(1) ?: ""

        when (subCommand) {
            "list", "ls", "" -> {
                val result = getInvariantsUseCase()
                if (result.invariants.isEmpty()) {
                    terminal.println(yellow("No invariants found"))
                } else {
                    terminal.println()
                    terminal.println(bold("Project Invariants (${result.totalCount}):"))
                    terminal.println()
                    result.invariants.forEach { inv ->
                        val status = if (inv.isActive) green("[ACTIVE]") else red("[INACTIVE]")
                        val priority = when (inv.priority) {
                            ru.agent.features.invariant.domain.model.InvariantPriority.CRITICAL -> red("[CRITICAL]")
                            ru.agent.features.invariant.domain.model.InvariantPriority.HIGH -> yellow("[HIGH]")
                            ru.agent.features.invariant.domain.model.InvariantPriority.MEDIUM -> gray("[MEDIUM]")
                        }
                        terminal.println("  ${cyan(inv.id.take(16))} $status $priority")
                        terminal.println("    ${inv.description.take(60)}${if (inv.description.length > 60) "..." else ""}")
                    }
                    terminal.println()
                    terminal.println(gray("Use /invariant add|toggle|remove to manage"))
                }
            }
            "add" -> {
                if (subArgs.isBlank()) {
                    terminal.println(red("Usage: /invariant add <description> [-c category] [-p priority]"))
                    terminal.println(gray("Categories: architecture, technology, stack, business"))
                    terminal.println(gray("Priorities: critical, high, medium"))
                    return
                }
                val result = addInvariantUseCase(
                    description = subArgs,
                    category = ru.agent.features.invariant.domain.model.InvariantCategory.BUSINESS_RULE,
                    priority = ru.agent.features.invariant.domain.model.InvariantPriority.MEDIUM
                )
                result.fold(
                    onSuccess = { inv ->
                        terminal.println(green("✓ Added: ${inv.id}"))
                        terminal.println(gray("  ${inv.description}"))
                    },
                    onFailure = { e ->
                        terminal.println(red("✗ Failed: ${e.message}"))
                    }
                )
            }
            "toggle" -> {
                if (subArgs.isBlank()) {
                    terminal.println(red("Usage: /invariant toggle <id>"))
                    return
                }
                val result = toggleInvariantUseCase(subArgs.trim())
                result.fold(
                    onSuccess = { inv ->
                        val status = if (inv.isActive) green("ENABLED") else red("DISABLED")
                        terminal.println("✓ $status: ${inv.id}")
                    },
                    onFailure = { e ->
                        terminal.println(red("✗ Failed: ${e.message}"))
                    }
                )
            }
            "remove", "rm" -> {
                if (subArgs.isBlank()) {
                    terminal.println(red("Usage: /invariant remove <id>"))
                    return
                }
                val result = removeInvariantUseCase(subArgs.trim())
                result.fold(
                    onSuccess = {
                        terminal.println(green("✓ Removed: ${subArgs.trim()}"))
                    },
                    onFailure = { e ->
                        terminal.println(red("✗ Failed: ${e.message}"))
                    }
                )
            }
            else -> {
                terminal.println(yellow("Unknown invariant command: $subCommand"))
                terminal.println(gray("Use: list, add, toggle, remove"))
            }
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
        terminal.println("  ${cyan("/invariant list")}  Show project invariants")
        terminal.println("  ${cyan("/invariant add")}   Add new invariant")
        terminal.println("  ${cyan("/invariant toggle")} Enable/disable invariant")
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
     * Clean terminal input from potential artifacts.
     *
     * When editing text in terminal (backspace, delete), sometimes
     * escape sequences and control characters remain in the input.
     */
    private fun cleanTerminalInput(input: String): String {
        return input
            // Remove ANSI escape sequences
            .replace(Regex("\u001B\\[[;\\d]*[ -/]*[@-~]"), "")
            // Remove control characters (except newlines and tabs)
            .replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]"), "")
            // Remove null characters
            .replace("\u0000", "")
            // Clean up multiple spaces
            .replace(Regex("  +"), " ")
            .trim()
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
