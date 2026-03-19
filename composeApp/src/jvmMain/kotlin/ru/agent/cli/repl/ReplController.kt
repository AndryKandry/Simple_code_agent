package ru.agent.cli.repl

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.longOrNull
import org.jline.reader.*
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.Terminal as JLineTerminal
import org.jline.terminal.TerminalBuilder
import ru.agent.cli.controller.CliChatController
import ru.agent.cli.controller.CliChatResult
import ru.agent.cli.formatters.OutputFormatter
import ru.agent.cli.visualization.CliAnimator
import ru.agent.cli.visualization.domain.ProgressState
import ru.agent.features.memory.domain.usecase.ClearShortTermMemoryUseCase
import ru.agent.features.memory.domain.usecase.GetMemoryContextUseCase
import ru.agent.features.memory.domain.repository.WorkingMemoryRepository
import ru.agent.features.profile.domain.usecase.GetUserProfileUseCase
import ru.agent.features.task.domain.repository.TaskStateRepository
import ru.agent.scheduler.SchedulerEngine
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
    private val clearShortTermMemoryUseCase: ClearShortTermMemoryUseCase by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }

    // Repository for cleanup
    private val taskStateRepository: TaskStateRepository by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }
    private val workingMemoryRepository: WorkingMemoryRepository by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }

    // MCP Manager for scheduler operations
    private val mcpManager: ru.agent.mcp.McpManager by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }

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
                            // Clear the prompt line and move cursor to beginning
                            val writer = jlineTerminal.writer()
                            writer.print("\u001B[1A\u001B[2K")  // Move up and clear line
                            writer.print("\u001B[2K\r")         // Clear current line and move to start
                            writer.flush()
                            // Show user message with timestamp
                            val timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                            writer.println("\u001B[90m$timestamp\u001B[0m \u001B[1;34mYou:\u001B[0m $cleanLine")
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

        // Start scheduler engine in background
        startSchedulerEngine()
    }

    /**
     * Start the scheduler engine for background task execution.
     */
    private fun startSchedulerEngine() {
        try {
            val engine: SchedulerEngine? = org.koin.java.KoinJavaComponent.getKoin().getOrNull(SchedulerEngine::class)
            if (engine != null) {
                engine.start()
                terminal.println(gray("Scheduler engine started"))
            }
        } catch (e: Exception) {
            terminal.println(yellow("Warning: Could not start scheduler engine: ${e.message}"))
        }
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
            "mcp" -> handleMcpCommand(args)
            "scheduler", "sched" -> handleSchedulerCommand(args)
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
            is CliChatResult.CompareResult -> {
                // Compare result already output via callback
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
            "clear" -> {
                try {
                    // Clear short-term memory (in-memory cache)
                    clearShortTermMemoryUseCase(CliChatController.CLI_SESSION_ID)
                    clearShortTermMemoryUseCase.clearAll()

                    // Clear working memory (database)
                    workingMemoryRepository.clearWorkingMemory(CliChatController.CLI_SESSION_ID)

                    // Clear all task states for session
                    val allTasks = taskStateRepository.getAllTasksForSession(CliChatController.CLI_SESSION_ID)
                    allTasks.forEach { task ->
                        taskStateRepository.deleteTaskState(task.taskId)
                    }

                    terminal.println(green("✓ Memory cleared successfully"))
                    terminal.println(blue("  - Short-term memory cleared"))
                    terminal.println(blue("  - Working memory cleared"))
                    terminal.println(blue("  - ${allTasks.size} task states cleared"))
                } catch (e: Exception) {
                    terminal.println(red("Error clearing memory: ${e.message}"))
                }
            }
            else -> terminal.println(yellow("Unknown memory command. Use: show, clear"))
        }
    }

    /**
     * Handle scheduler commands.
     */
    private suspend fun handleSchedulerCommand(args: String) {
        val parts = args.trim().split("\\s+".toRegex())
        when (parts.getOrNull(0)?.lowercase()) {
            "list", "ls", "" -> {
                val showAll = parts.contains("-a") || parts.contains("--all")
                try {
                    val statusFilter = if (showAll) null else "pending"
                    val result = mcpManager.listScheduledTasks(statusFilter = statusFilter)
                    if (result.isSuccess) {
                        terminal.println(result.getOrDefault("No tasks found"))
                    } else {
                        terminal.println(red("Error: ${result.exceptionOrNull()?.message}"))
                    }
                } catch (e: Exception) {
                    terminal.println(red("Error listing tasks: ${e.message}"))
                }
            }
            "cancel" -> {
                val taskId = parts.getOrNull(1)
                if (taskId.isNullOrBlank()) {
                    terminal.println(yellow("Usage: /scheduler cancel <task_id>"))
                    return
                }
                try {
                    val result = mcpManager.cancelScheduledTask(taskId)
                    if (result.isSuccess) {
                        terminal.println(green("✓ Task cancelled: $taskId"))
                        terminal.println(result.getOrDefault(""))
                    } else {
                        terminal.println(red("Error: ${result.exceptionOrNull()?.message}"))
                    }
                } catch (e: Exception) {
                    terminal.println(red("Error cancelling task: ${e.message}"))
                }
            }
            "delete", "rm", "remove" -> {
                val taskId = parts.getOrNull(1)
                if (taskId.isNullOrBlank()) {
                    terminal.println(yellow("Usage: /scheduler delete <task_id>"))
                    return
                }
                try {
                    val result = mcpManager.deleteScheduledTask(taskId)
                    if (result.isSuccess) {
                        terminal.println(red("🗑 Task deleted: $taskId"))
                        terminal.println(result.getOrDefault(""))
                    } else {
                        terminal.println(red("Error: ${result.exceptionOrNull()?.message}"))
                    }
                } catch (e: Exception) {
                    terminal.println(red("Error deleting task: ${e.message}"))
                }
            }
            "pause" -> {
                val taskId = parts.getOrNull(1)
                if (taskId.isNullOrBlank()) {
                    terminal.println(yellow("Usage: /scheduler pause <task_id>"))
                    return
                }
                try {
                    val result = mcpManager.pauseScheduledTask(taskId)
                    if (result.isSuccess) {
                        terminal.println(yellow("⏸ Task paused: $taskId"))
                        terminal.println(result.getOrDefault(""))
                    } else {
                        terminal.println(red("Error: ${result.exceptionOrNull()?.message}"))
                    }
                } catch (e: Exception) {
                    terminal.println(red("Error pausing task: ${e.message}"))
                }
            }
            "resume" -> {
                val taskId = parts.getOrNull(1)
                if (taskId.isNullOrBlank()) {
                    terminal.println(yellow("Usage: /scheduler resume <task_id>"))
                    return
                }
                try {
                    val result = mcpManager.resumeScheduledTask(taskId)
                    if (result.isSuccess) {
                        terminal.println(green("▶ Task resumed: $taskId"))
                        terminal.println(result.getOrDefault(""))
                    } else {
                        terminal.println(red("Error: ${result.exceptionOrNull()?.message}"))
                    }
                } catch (e: Exception) {
                    terminal.println(red("Error resuming task: ${e.message}"))
                }
            }
            "get" -> {
                val taskId = parts.getOrNull(1)
                if (taskId.isNullOrBlank()) {
                    terminal.println(yellow("Usage: /scheduler get <task_id>"))
                    return
                }
                try {
                    val result = mcpManager.getScheduledTask(taskId)
                    if (result.isSuccess) {
                        terminal.println(result.getOrDefault("Task not found"))
                    } else {
                        terminal.println(red("Error: ${result.exceptionOrNull()?.message}"))
                    }
                } catch (e: Exception) {
                    terminal.println(red("Error getting task: ${e.message}"))
                }
            }
            "history" -> {
                val taskId = parts.getOrNull(1)
                if (taskId.isNullOrBlank()) {
                    terminal.println(yellow("Usage: /scheduler history <task_id>"))
                    return
                }
                try {
                    val result = mcpManager.getTaskHistory(taskId)
                    if (result.isSuccess) {
                        terminal.println(result.getOrDefault("No history found"))
                    } else {
                        terminal.println(red("Error: ${result.exceptionOrNull()?.message}"))
                    }
                } catch (e: Exception) {
                    terminal.println(red("Error getting history: ${e.message}"))
                }
            }
            "help" -> showSchedulerHelp()
            else -> {
                terminal.println(yellow("Unknown scheduler command. Available: list, cancel, delete, pause, resume, get, history"))
                terminal.println(gray("Type '/scheduler help' for more details"))
            }
        }
    }

    private fun showSchedulerHelp() {
        terminal.println()
        terminal.println(bold("Scheduler Commands:"))
        terminal.println()
        terminal.println("  ${cyan("/scheduler list")}         List active scheduled tasks")
        terminal.println("  ${cyan("/scheduler list -a")}       List all tasks (including completed)")
        terminal.println("  ${cyan("/scheduler get <id>")}      Get task details")
        terminal.println("  ${cyan("/scheduler cancel <id>")}   Cancel a scheduled task")
        terminal.println("  ${cyan("/scheduler delete <id>")}   Delete a task permanently")
        terminal.println("  ${cyan("/scheduler pause <id>")}    Pause a scheduled task")
        terminal.println("  ${cyan("/scheduler resume <id>")}   Resume a paused task")
        terminal.println("  ${cyan("/scheduler history <id>")}  Show task execution history")
        terminal.println()
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
     * Handle MCP commands.
     */
    private suspend fun handleMcpCommand(args: String) {
        val mcpManager: ru.agent.mcp.McpManager by lazy {
            org.koin.java.KoinJavaComponent.getKoin().get()
        }

        val parts = args.trim().split("\\s+".toRegex(), 2)
        val subCommand = parts.getOrNull(0) ?: "status"
        val subArgs = parts.getOrNull(1) ?: ""

        when (subCommand) {
            "status", "" -> {
                terminal.println()
                terminal.println(bold("MCP Infrastructure Status:"))
                terminal.println()

                // Built-in servers
                terminal.println(brightBlue("Built-in Servers:"))
                mcpManager.getBuiltInServers().forEach { server ->
                    terminal.println("  ${green(server.name)}: ${server.tools.size} tools")
                }

                // External connections
                terminal.println(brightBlue("\nExternal Connections:"))
                val connected = mcpManager.getConnectedExternalServers()
                if (connected.isEmpty()) {
                    terminal.println(gray("  (no connections)"))
                } else {
                    connected.forEach { server ->
                        val status = if (mcpManager.isExternalServerConnected(server)) {
                            green("connected")
                        } else {
                            red("disconnected")
                        }
                        terminal.println("  ${yellow(server)}: $status")
                    }
                }

                // Summary
                val totalTools = mcpManager.getAllAvailableTools().size
                terminal.println(brightBlue("\nTotal Tools Available: $totalTools"))
            }

            "list", "ls" -> {
                terminal.println()
                terminal.println(bold("MCP Servers:"))
                terminal.println()

                terminal.println(brightBlue("Built-in:"))
                terminal.println("  ${green("filesystem")} - File operations (read, write, list, search)")
                terminal.println("  ${green("terminal")}   - Command execution (safe commands only)")

                terminal.println(gray("\nUse /mcp connect <name> <url> to connect to external MCP servers."))
            }

            "tools" -> {
                val tools = mcpManager.getAllAvailableTools()
                    .filter { subArgs.isEmpty() || it.serverName.contains(subArgs, ignoreCase = true) }

                terminal.println()
                terminal.println(bold("Available MCP Tools (${tools.size}):"))
                terminal.println()

                tools.groupBy { it.serverName }.forEach { (serverName, serverTools) ->
                    terminal.println(brightBlue("$serverName:"))
                    serverTools.forEach { tool ->
                        terminal.println("  ${green(tool.fullName)}")
                        if (subArgs.contains("-v", ignoreCase = true)) {
                            terminal.println(gray("    ${tool.description}"))
                        }
                    }
                }
            }

            "connect" -> {
                if (subArgs.isBlank()) {
                    terminal.println(red("Usage: /mcp connect <server> [url]"))
                    terminal.println(gray("Example: /mcp connect github"))
                    terminal.println(gray("Example: /mcp connect custom http://localhost:8080/mcp"))
                    return
                }

                val connectParts = subArgs.split("\\s+".toRegex(), 2)
                val serverName = connectParts[0]
                val url = connectParts.getOrNull(1)

                if (url == null) {
                    terminal.println(red("Error: URL required."))
                    terminal.println(gray("Usage: /mcp connect <name> <url>"))
                    terminal.println(gray("Example: /mcp connect myserver http://localhost:8080/mcp"))
                    return
                }

                val serverUrl = url
                terminal.println(cyan("Connecting to $serverName at $serverUrl..."))

                try {
                    kotlinx.coroutines.withTimeout(30000) {
                        val result = mcpManager.connectToServer(serverName, serverUrl, 30000)
                        result.fold(
                            onSuccess = {
                                terminal.println(green("Successfully connected to $serverName"))
                            },
                            onFailure = { error ->
                                terminal.println(red("Failed to connect: ${error.message}"))
                            }
                        )
                    }
                } catch (e: Exception) {
                    terminal.println(red("Connection timeout: ${e.message}"))
                }
            }

            "disconnect" -> {
                if (subArgs.isBlank() || subArgs == "-a" || subArgs == "--all") {
                    mcpManager.disconnectAll()
                    terminal.println(green("Disconnected from all external servers."))
                    return
                }

                val result = mcpManager.disconnectFromServer(subArgs.trim())
                result.fold(
                    onSuccess = {
                        terminal.println(green("Disconnected from $subArgs."))
                    },
                    onFailure = { error ->
                        terminal.println(red("Failed to disconnect: ${error.message}"))
                    }
                )
            }

            "exec" -> {
                val execParts = subArgs.split("\\s+".toRegex(), 2)
                val fullName = execParts.getOrNull(0)
                val argsJson = execParts.getOrNull(1)

                if (fullName.isNullOrBlank()) {
                    terminal.println(red("Usage: /mcp exec <server:tool> [json_args]"))
                    terminal.println(gray("Example: /mcp exec filesystem:read_file {\"path\": \"README.md\"}"))
                    return
                }

                // Parse arguments
                val arguments = if (!argsJson.isNullOrBlank()) {
                    try {
                        kotlinx.serialization.json.Json.decodeFromString<
                            kotlinx.serialization.json.JsonObject>(argsJson)
                            .mapValues { (_, value) ->
                                when (value) {
                                    is kotlinx.serialization.json.JsonPrimitive -> {
                                        value.booleanOrNull ?: value.longOrNull ?: value.content
                                    }
                                    else -> value.toString()
                                }
                            }
                    } catch (e: Exception) {
                        terminal.println(red("Invalid JSON arguments: ${e.message}"))
                        return
                    }
                } else {
                    emptyMap()
                }

                terminal.println(cyan("Executing $fullName..."))

                val result = mcpManager.executeToolByFullName(fullName, arguments)
                result.fold(
                    onSuccess = { output ->
                        terminal.println(green("Result:"))
                        terminal.println(output)
                    },
                    onFailure = { error ->
                        terminal.println(red("Error: ${error.message}"))
                    }
                )
            }

            "read" -> {
                if (subArgs.isBlank()) {
                    terminal.println(red("Usage: /mcp read <file_path>"))
                    return
                }

                val result = mcpManager.readFile(subArgs.trim())
                result.fold(
                    onSuccess = { content ->
                        terminal.println(content)
                    },
                    onFailure = { error ->
                        terminal.println(red("Error: ${error.message}"))
                    }
                )
            }

            "write" -> {
                val writeParts = subArgs.split("\\s+".toRegex(), 2)
                val path = writeParts.getOrNull(0)
                val content = writeParts.getOrNull(1)

                if (path.isNullOrBlank() || content.isNullOrBlank()) {
                    terminal.println(red("Usage: /mcp write <file_path> <content>"))
                    return
                }

                val result = mcpManager.writeFile(path, content)
                result.fold(
                    onSuccess = { message ->
                        terminal.println(green(message))
                    },
                    onFailure = { error ->
                        terminal.println(red("Error: ${error.message}"))
                    }
                )
            }

            "run" -> {
                if (subArgs.isBlank()) {
                    terminal.println(red("Usage: /mcp run <command>"))
                    terminal.println(gray("Use /mcp exec terminal:list_allowed_commands to see allowed commands."))
                    return
                }

                val command = subArgs.trim()

                if (!mcpManager.isCommandAllowed(command)) {
                    terminal.println(red("Error: Command not allowed by security policy."))
                    terminal.println(gray("Use /mcp exec terminal:list_allowed_commands to see allowed commands."))
                    return
                }

                terminal.println(cyan("Executing: $command"))
                terminal.println("-".repeat(50))

                val result = mcpManager.executeCommand(command, 30000)
                result.fold(
                    onSuccess = { output ->
                        terminal.println(output)
                    },
                    onFailure = { error ->
                        terminal.println(red("Error: ${error.message}"))
                    }
                )
            }

            else -> {
                terminal.println(yellow("Unknown MCP command: $subCommand"))
                terminal.println(gray("Use: status, list, tools, connect, disconnect, exec, read, write, run"))
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
        terminal.println("  ${cyan("/memory clear")}    Clear all memory and reset session")
        terminal.println("  ${cyan("/invariant list")}  Show project invariants")
        terminal.println("  ${cyan("/invariant add")}   Add new invariant")
        terminal.println("  ${cyan("/invariant toggle")} Enable/disable invariant")
        terminal.println("  ${cyan("/shell <cmd>")}     Execute shell command")
        terminal.println()
        terminal.println(bold("MCP Commands (Model Context Protocol):"))
        terminal.println()
        terminal.println("  ${cyan("/mcp status")}          Show MCP servers status")
        terminal.println("  ${cyan("/mcp list")}            List available MCP servers")
        terminal.println("  ${cyan("/mcp tools")}           List all available MCP tools")
        terminal.println("  ${cyan("/mcp tools -v")}        List tools with descriptions")
        terminal.println("  ${cyan("/mcp connect <n> <url>")} Connect to external MCP server")
        terminal.println("  ${cyan("/mcp disconnect <srv>")} Disconnect from server")
        terminal.println("  ${cyan("/mcp disconnect -a")}   Disconnect from all servers")
        terminal.println("  ${cyan("/mcp exec <srv:tool>")} Execute MCP tool")
        terminal.println("  ${cyan("/mcp read <path>")}     Read file via filesystem MCP")
        terminal.println("  ${cyan("/mcp write <p> <c>")}   Write file via filesystem MCP")
        terminal.println("  ${cyan("/mcp run <command>")}   Execute terminal command")
        terminal.println()
        terminal.println(gray("  Built-in servers: filesystem, terminal, scheduler"))
        terminal.println()
        terminal.println(bold("Scheduler Commands (Task Scheduling):"))
        terminal.println()
        terminal.println("  ${cyan("/scheduler list")}              List all scheduled tasks")
        terminal.println("  ${cyan("/scheduler list -a")}            List all tasks including completed")
        terminal.println("  ${cyan("/scheduler get <id>")}           Get task details")
        terminal.println("  ${cyan("/scheduler cancel <id>")}        Cancel a scheduled task")
        terminal.println("  ${cyan("/scheduler delete <id>")}        Delete a task permanently")
        terminal.println("  ${cyan("/scheduler pause <id>")}         Pause a scheduled task")
        terminal.println("  ${cyan("/scheduler resume <id>")}        Resume a paused task")
        terminal.println("  ${cyan("/scheduler history <id>")}       Show task execution history")
        terminal.println()
        terminal.println(green("  Natural language scheduling:"))
        terminal.println(gray("  \"Создай напоминание Проверить почту каждый день в 9 утра\""))
        terminal.println(gray("  \"Напомни мне через минуту сохранить файл\""))
        terminal.println(gray("  \"Schedule a reminder every hour to drink water\""))
        terminal.println()
        terminal.println(yellow("  Cron expressions:"))
        terminal.println(gray("  * * * * *     - every minute"))
        terminal.println(gray("  */5 * * * *   - every 5 minutes"))
        terminal.println(gray("  0 * * * *     - every hour"))
        terminal.println(gray("  0 9 * * *     - every day at 9:00"))
        terminal.println(gray("  0 9 * * 1     - every Monday at 9:00"))
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
        terminal.println("  The agent can use MCP tools automatically when needed")
        terminal.println("  Example: \"Read README.md and summarize it\"")
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
