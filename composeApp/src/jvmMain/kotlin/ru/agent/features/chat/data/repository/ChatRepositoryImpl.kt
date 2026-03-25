package ru.agent.features.chat.data.repository

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.core.handlers.NetworkErrorHandling
import ru.agent.core.platform.getWorkingDirectory
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.chat.data.local.dao.ChatSessionDao
import ru.agent.features.chat.data.local.dao.MessageDao
import ru.agent.features.chat.data.local.entity.ChatSessionEntity
import ru.agent.features.chat.data.local.mapper.MessageMapper.toDomain
import ru.agent.features.chat.data.local.mapper.MessageMapper.toEntity
import ru.agent.features.chat.data.remote.LlmApiClient
import ru.agent.features.chat.data.remote.dto.ChatRequest
import ru.agent.features.chat.data.remote.dto.MessageDto
import ru.agent.features.chat.data.remote.dto.ToolDefinitionDto
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.model.SenderType
import ru.agent.features.chat.domain.optimization.ContextOptimizer
import ru.agent.features.chat.domain.optimization.OptimizedContext
import ru.agent.features.chat.domain.repository.ChatRepository
import ru.agent.features.chat.domain.tools.ToolExecutor
import ru.agent.features.invariant.domain.exception.InvariantViolationException
import ru.agent.features.invariant.domain.model.CheckType
import ru.agent.features.invariant.domain.service.ValidationService
import ru.agent.features.invariant.domain.usecase.ValidateInvariantViolationUseCase
import ru.agent.features.memory.domain.usecase.GetMemoryContextUseCase
import ru.agent.mcp.orchestration.ExecutionPlan
import ru.agent.mcp.orchestration.McpOrchestrator
import ru.agent.mcp.orchestration.OrchestrationContext
import ru.agent.mcp.orchestration.RequestType
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Implementation of ChatRepository using Room database for persistent storage.
 *
 * Uses LLM API (DeepSeek or Ollama) for AI responses and stores all messages in local database.
 * Includes context optimization to manage token limits.
 * Supports function calling (tools) via MCP integration.
 *
 * @property llmApiClient API client for LLM (DeepSeek or Ollama)
 * @property networkErrorHandling Handler for network errors
 * @property messageDao DAO for message persistence
 * @property chatSessionDao DAO for chat session persistence
 * @property contextOptimizer Optimizer for context window management
 * @property validateInvariantViolationUseCase Use case for validating invariants
 * @property getMemoryContextUseCase Use case for retrieving memory context
 * @property validationService Service for validation
 * @property toolExecutor Executor for MCP tools
 * @property orchestrator Orchestrator for parallel tool execution and planning
 * @property maxToolIterations Maximum iterations for tool calls (default: 10)
 * @property toolLoopHistorySize Size of history for loop detection (default: 10)
 */
class ChatRepositoryImpl(
    private val llmApiClient: LlmApiClient,
    private val networkErrorHandling: NetworkErrorHandling,
    private val messageDao: MessageDao,
    private val chatSessionDao: ChatSessionDao,
    private val contextOptimizer: ContextOptimizer,
    private val validateInvariantViolationUseCase: ValidateInvariantViolationUseCase,
    private val getMemoryContextUseCase: GetMemoryContextUseCase,
    private val validationService: ValidationService,
    private val toolExecutor: ToolExecutor,
    private val orchestrator: McpOrchestrator,
    private val maxToolIterations: Int = DEFAULT_MAX_TOOL_ITERATIONS,
    private val toolLoopHistorySize: Int = DEFAULT_TOOL_LOOP_HISTORY_SIZE
) : ChatRepository {

    private val logger = Logger.withTag("ChatRepository")

    companion object {
        /** Maximum tool call iterations to prevent infinite loops */
        const val DEFAULT_MAX_TOOL_ITERATIONS = 10

        /** Size of history for detecting tool call loops */
        const val DEFAULT_TOOL_LOOP_HISTORY_SIZE = 10

        /** Environment variable for log level configuration */
        const val ENV_LOG_LEVEL = "AGENT_LOG_LEVEL"

        /** Environment variable for debug mode */
        const val ENV_DEBUG_MODE = "AGENT_DEBUG"
    }

    /**
     * Get working directory name for display purposes.
     * Returns the last directory component for human-readable display.
     *
     * @param path Full path
     * @return Last directory component name
     */
    private fun getWorkingDirectoryName(path: String): String {
        if (path.isBlank()) return "<unknown>"
        val lastComponent = path.substringAfterLast('/', path.substringAfterLast('\\'))
        return if (lastComponent.isNotBlank()) lastComponent else "<root>"
    }

    /**
     * Build tool instructions system prompt dynamically based on available tools.
     *
     * @param workingDirectory Current working directory (FULL ABSOLUTE PATH)
     * @param tools List of available tools
     * @return Formatted system prompt with tool instructions
     */
    private suspend fun buildToolInstructions(
        workingDirectory: String,
        tools: List<ToolDefinitionDto>
    ): String {
        val projectName = getWorkingDirectoryName(workingDirectory)

        // Dynamically generate tools description
        val toolsDescription = if (tools.isEmpty()) {
            "No tools currently available."
        } else {
            tools.joinToString("\n") { tool ->
                val params = tool.function.parameters
                    ?.get("properties")
                    ?.let { props ->
                        @Suppress("UNCHECKED_CAST")
                        (props as? Map<String, Any>)?.keys?.joinToString(", ") ?: ""
                    } ?: "none"
                "- ${tool.function.name}: ${tool.function.description.ifBlank { "No description" }} (params: $params)"
            }
        }

        // Check if scheduler tools are available
        val hasSchedulerTools = tools.any { it.function.name.startsWith("scheduler_") }

        return """
            You are an AI assistant in CLI (Command Line Interface) mode.

            ╔══════════════════════════════════════════════════════════════════════════════╗
            ║                         CLI ENVIRONMENT CONTEXT                              ║
            ╠══════════════════════════════════════════════════════════════════════════════╣
            ║ This is a COMMAND LINE application.                                          ║
            ║ DO NOT generate Compose Desktop UI code, Swing, JavaFX, or any GUI code.     ║
            ║ DO NOT generate @Composable functions or remember {} state blocks.           ║
            ║ Only CLI commands, text output, and tool calls are appropriate.              ║
            ╚══════════════════════════════════════════════════════════════════════════════╝

            WORKING DIRECTORY (FULL ABSOLUTE PATH): $workingDirectory
            Project name: $projectName

            ╔══════════════════════════════════════════════════════════════════════════════╗
            ║                    CRITICAL: YOU HAVE ACCESS TO TOOLS                        ║
            ╠══════════════════════════════════════════════════════════════════════════════╣
            ║ DO NOT answer from memory or training data when user asks about files.       ║
            ║ DO NOT guess, estimate, or fabricate ANY file content.                       ║
            ║ DO NOT generate code when you can use a TOOL instead.                        ║
            ║ YOU MUST use tools to access real files and system features.                 ║
            ╚══════════════════════════════════════════════════════════════════════════════╝

            === MANDATORY TOOL USAGE - NO EXCEPTIONS ===

            If user mentions ANY of these actions, you MUST call the tool FIRST:
            - "read", "show", "open", "display", "what is in", "contents of" + file name
            - "list", "show files", "directory contents", "what files"
            - "write", "create", "modify", "edit" + file name
            - "search", "find", "look for" + file pattern
            - "remind", "reminder", "schedule", "alarm", "notification", "notify"
            - "cron", "periodic", "recurring task", "scheduled task"

            BEFORE responding with ANY file content:
            1. Call filesystem_read_file(path="FULL_ABSOLUTE_PATH")
            2. WAIT for the tool result
            3. ONLY THEN use the ACTUAL content from the tool result

            IF YOU RESPOND WITH FILE CONTENT WITHOUT CALLING THE TOOL FIRST, YOU ARE WRONG.

            ${if (hasSchedulerTools) """
            === SCHEDULER TOOLS ===
            You have access to task scheduling tools. USE THEM instead of generating code!

            For reminders/notifications: Use scheduler_schedule_reminder
            - message: The reminder text to display
            - cron: Cron expression (e.g., "* * * * *" = every minute, "0 9 * * *" = daily at 9:00)

            For scheduled commands: Use scheduler_schedule_command
            - command: Shell command to execute
            - cron: Cron expression

            Common cron patterns:
            - "* * * * *" = every minute
            - "*/5 * * * *" = every 5 minutes
            - "0 * * * *" = every hour
            - "0 9 * * *" = every day at 9:00
            - "0 9 * * 1" = every Monday at 9:00

            === SCHEDULER EXAMPLES ===

            User: "Создай напоминание"
            YOUR ACTION: Call scheduler_schedule_reminder with appropriate cron expression
            THEN: Confirm the reminder was created with task details

            User: "Напомни мне через минуту"
            YOUR ACTION: Call scheduler_schedule_reminder(message="...", cron="* * * * *")

            User: "Every day at 9am remind me to check emails"
            YOUR ACTION: Call scheduler_schedule_reminder(message="Check emails", cron="0 9 * * *")

            User: "Покажи список задач"
            YOUR ACTION: Call scheduler_list_scheduled_tasks
            THEN: Show the returned task list

            ╔══════════════════════════════════════════════════════════════════════════════╗
            ║  WRONG: Generating Compose Desktop code for reminders                        ║
            ║  CORRECT: Using scheduler_schedule_reminder tool                             ║
            ╚══════════════════════════════════════════════════════════════════════════════╝
            """ else ""}

            === PATH REQUIREMENTS ===
            ALWAYS use FULL ABSOLUTE PATH starting with: $workingDirectory
            Example: $workingDirectory/README.md (NOT just "README.md" or "./README.md")

            === AVAILABLE TOOLS ===
            $toolsDescription

            === FILE EXAMPLE INTERACTIONS ===

            User: "Прочитай файл README.md"
            YOUR FIRST ACTION: Call filesystem_read_file(path="$workingDirectory/README.md")
            THEN: Report the actual content returned by the tool

            User: "Read the README"
            YOUR FIRST ACTION: Call filesystem_read_file(path="$workingDirectory/README.md")
            THEN: Report the actual content returned by the tool

            User: "What's in the config file?"
            YOUR FIRST ACTION: Call filesystem_read_file(path="$workingDirectory/config")
            THEN: Report the actual content returned by the tool

            ╔══════════════════════════════════════════════════════════════════════════════╗
            ║  WRONG: "I read the file, here's what it contains: [made up content]"        ║
            ║  CORRECT: [Call tool first, then respond with actual tool result]            ║
            ╚══════════════════════════════════════════════════════════════════════════════╝
        """.trimIndent()
    }

    /**
     * Detect if user message requires tool operations (file or scheduler).
     * Used to determine if we should force tool usage.
     *
     * @param message User message to analyze
     * @return true if message likely requires tools
     */
    private fun isFileOperationRequest(message: String): Boolean {
        val lowerMessage = message.lowercase()

        // File reading keywords
        val readKeywords = listOf(
            "read", "прочитай", "читай", "прочитать",
            "show", "покажи", "показать",
            "open", "открой", "открыть",
            "display", "отобрази",
            "contents", "содержимое", "содержание",
            "what is in", "что в", "что внутри",
            "file", "файл", "файла",
            "list", "лист", "список",
            "directory", "директория", "папка", "каталог"
        )

        // Check for file operation intent
        val hasReadIntent = readKeywords.any { lowerMessage.contains(it) }

        // Check for specific file references (has extension or common file names)
        val hasFileReference = lowerMessage.contains(Regex("""\.(md|txt|kt|java|py|js|json|xml|yaml|yml|gradle|properties|conf|cfg)""")) ||
                lowerMessage.contains("readme") ||
                lowerMessage.contains("config") ||
                lowerMessage.contains("build") ||
                lowerMessage.contains("settings")

        val isFileRequest = hasReadIntent && hasFileReference

        // Check for scheduler/reminder operation intent
        val schedulerKeywords = listOf(
            "remind", "reminder", "напомни", "напоминание", "напомнить",
            "schedule", "scheduled", "запланируй", "запланировать", "планировщик",
            "alarm", "будильник", "таймер", "timer",
            "notify", "notification", "уведомление", "уведоми",
            "cron", "periodic", "периодический", "регулярный",
            "every minute", "каждую минуту", "каждый день", "every day",
            "at", "в", "repeat", "повторяй"
        )

        val isSchedulerRequest = schedulerKeywords.any { lowerMessage.contains(it) }

        return isFileRequest || isSchedulerRequest
    }

    /**
     * Validate message order for API compatibility.
     * Every message with role="tool" must have a preceding assistant message with tool_calls.
     * Removes orphaned tool messages that would cause API errors.
     *
     * @param messages List of messages to validate
     * @return Validated list with orphaned tool messages removed
     */
    private fun validateMessageOrder(messages: List<MessageDto>): List<MessageDto> {
        val result = mutableListOf<MessageDto>()
        var lastAssistantHadToolCalls = false

        for (msg in messages) {
            when (msg.role) {
                "assistant" -> {
                    lastAssistantHadToolCalls = !msg.toolCalls.isNullOrEmpty()
                    result.add(msg)
                }
                "tool" -> {
                    // Only add tool message if last assistant had tool_calls
                    if (lastAssistantHadToolCalls) {
                        result.add(msg)
                    } else {
                        logger.w { "Removing orphaned tool message (no preceding assistant with tool_calls)" }
                    }
                    // Reset after processing tool message
                    lastAssistantHadToolCalls = false
                }
                else -> {
                    // user, system messages - just add them
                    result.add(msg)
                    // Reset tool_calls flag on user messages (new conversation turn)
                    if (msg.role == "user") {
                        lastAssistantHadToolCalls = false
                    }
                }
            }
        }

        return result
    }

    /**
     * Prepare messages for API request with system prompt and context.
     *
     * @param sessionId Session ID for context
     * @param additionalMessage Optional additional message to add
     * @param includeTools Whether to include tool instructions in system prompt
     * @return List of prepared messages
     */
    private suspend fun prepareMessages(
        sessionId: String,
        additionalMessage: String? = null,
        includeTools: Boolean = false
    ): MutableList<MessageDto> {
        val optimizedContext = getOptimizedContext(sessionId)
        val memoryContext = getMemoryContextUseCase(sessionId)
        val systemPrompt = memoryContext.toSystemPrompt()

        val messages = mutableListOf<MessageDto>()

        // Build full system prompt
        val fullSystemPrompt = buildString {
            if (systemPrompt.isNotBlank()) {
                append(systemPrompt)
                append("\n\n")
            }
            if (includeTools) {
                val workingDirectory = getWorkingDirectory()
                val tools = toolExecutor.getToolsForApi()
                append(buildToolInstructions(workingDirectory, tools))
            }
        }

        // Add system prompt
        if (fullSystemPrompt.isNotBlank()) {
            messages.add(MessageDto(role = "system", content = fullSystemPrompt))
        }

        // Add conversation messages (from DB - these don't have tool_calls info)
        val dbMessages = optimizedContext.messages.map { msg ->
            MessageDto(
                role = when (msg.senderType) {
                    SenderType.USER -> "user"
                    SenderType.ASSISTANT -> "assistant"
                    SenderType.SYSTEM -> "system"
                },
                content = msg.content
            )
        }
        messages.addAll(dbMessages)

        // Add additional message if provided
        additionalMessage?.let {
            messages.add(MessageDto(role = "user", content = it))
        }

        // Validate message order - remove orphaned tool messages
        // This is a safety net since DB doesn't store tool_calls info
        return validateMessageOrder(messages).toMutableList()
    }

    /**
     * Ensure the session exists before performing operations.
     * Creates the session if it doesn't exist.
     *
     * @param sessionId Session ID to ensure exists
     */
    private suspend fun ensureSessionExists(sessionId: String) {
        val existingSession = chatSessionDao.getSessionById(sessionId)
        if (existingSession == null) {
            logger.i { "Session $sessionId does not exist, creating it..." }
            val now = currentTimeMillis()
            val newSession = ChatSessionEntity(
                id = sessionId,
                title = "New Chat",
                createdAt = now,
                updatedAt = now,
                isArchived = false,
                messageCount = 0
            )
            chatSessionDao.insertSession(newSession)
            logger.i { "Session $sessionId created successfully" }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun sendMessage(sessionId: String, message: String): ResultWrapper<Message> {
        logger.i { "sendMessage called for session: $sessionId, message: ${message.take(50)}..." }

        return withContext(Dispatchers.IO) {
            try {
                // Step 0: Ensure session exists
                ensureSessionExists(sessionId)

                // === Validate user request against invariants ===
                val requestValidation = validateInvariantViolationUseCase(
                    text = message,
                    checkType = CheckType.USER_REQUEST
                )

                if (requestValidation.shouldBlock) {
                    logger.w { "User request blocked by invariant: ${requestValidation.blockMessage}" }
                    return@withContext ResultWrapper.Error(
                        throwable = InvariantViolationException(
                            message = requestValidation.blockMessage ?: "Invariant violation",
                            violations = requestValidation.violations
                        ),
                        message = requestValidation.blockMessage ?: "Запрос заблокирован из-за нарушения правил проекта"
                    )
                }

                if (requestValidation.hasViolations) {
                    logger.w { "User request has warnings: ${requestValidation.violations.size}" }
                }

                // Step 1: Create user message with UUID
                val userMessage = Message(
                    id = Uuid.random().toString(),
                    content = message,
                    senderType = SenderType.USER,
                    timestamp = currentTimeMillis()
                )

                // Step 2: Save user message to database
                messageDao.insertMessage(userMessage.toEntity(sessionId))
                logger.d { "User message saved to database with ID: ${userMessage.id}" }

                // Step 3: Increment message count in session
                chatSessionDao.incrementMessageCount(sessionId, currentTimeMillis())

                // Step 4-6: Prepare messages with tools
                val currentMessages = prepareMessages(sessionId, includeTools = true)
                logger.i { "Sending request to ${llmApiClient.getProviderName()} with ${currentMessages.size} messages" }

                // Detect if user is asking for file operations
                val isFileRequest = isFileOperationRequest(message)
                if (isFileRequest) {
                    logger.i { "Detected file operation request, will require tool usage" }
                }

                // Step 7: Call LLM API with tools
                var iteration = 0
                var finalResponse: ru.agent.features.chat.data.remote.dto.ChatResponse? = null
                var finalContent: String? = null

                // Enhanced loop detection: track recent tool call signatures
                val toolCallHistory = mutableSetOf<String>()
                val toolCallOrder = mutableListOf<String>()

                // Track if model skipped tool on file request
                var toolSkippedOnFileRequest = false

                logger.i { "Fetching tools for API request..." }

                while (iteration < maxToolIterations) {
                    iteration++

                    // Prepare request - ALWAYS with tools
                    // Use tool_choice="required" for file operations on first iteration
                    val response = try {
                        val tools = toolExecutor.getToolsForApi()
                        val shouldForceToolUse = isFileRequest && iteration == 1

                        logger.i { "Sending request WITH ${tools.size} tools (iteration: $iteration, forceTool: $shouldForceToolUse)" }

                        // CRITICAL: Validate and fix message order before sending to API
                        // This removes orphaned tool messages that would cause API errors
                        val validatedMessages = validateMessageOrder(currentMessages)
                        if (validatedMessages.size != currentMessages.size) {
                            logger.w { "Message order validation removed ${currentMessages.size - validatedMessages.size} orphaned messages" }
                            currentMessages.clear()
                            currentMessages.addAll(validatedMessages)
                        }

                        // DEBUG: Log message structure
                        val toolResultsCount = currentMessages.count { it.role == "tool" }
                        val assistantWithToolCallsCount = currentMessages.count { it.role == "assistant" && !it.toolCalls.isNullOrEmpty() }
                        logger.i { "Message structure: ${currentMessages.size} total, $toolResultsCount tool results, $assistantWithToolCallsCount assistant with tool_calls" }

                        if (tools.isEmpty()) {
                            logger.w { "No tools available, falling back to simple request" }
                            val requestSimple = ChatRequest.simple(messages = currentMessages)
                            llmApiClient.sendMessage(requestSimple)
                        } else if (shouldForceToolUse) {
                            // Force tool usage for file operations
                            logger.i { "Forcing tool usage with tool_choice=required" }
                            val requestWithRequiredTools = ChatRequest.withRequiredTools(
                                messages = currentMessages,
                                tools = tools
                            )
                            llmApiClient.sendMessage(requestWithRequiredTools)
                        } else {
                            val requestWithTools = ChatRequest.withTools(
                                messages = currentMessages,
                                tools = tools
                            )
                            llmApiClient.sendMessage(requestWithTools)
                        }
                    } catch (e: Exception) {
                        logger.e(throwable = e) { "Error during API request: ${e.message}" }
                        messageDao.deleteMessageById(userMessage.id)
                        return@withContext ResultWrapper.Error(
                            throwable = e,
                            message = e.message ?: "API request failed"
                        )
                    }
                    logger.i { "Received response from ${llmApiClient.getProviderName()}. ID: ${response.id}, choices: ${response.choices.size}, iteration: $iteration" }

                    // Validate response
                    if (response.choices.isEmpty()) {
                        logger.e { "Empty response from API" }
                        messageDao.deleteMessageById(userMessage.id)
                        return@withContext ResultWrapper.Error(
                            throwable = IllegalStateException("Empty response from API"),
                            message = "Received empty response from LLM API"
                        )
                    }

                    // Check finish_reason first - if "stop", this is a complete response
                    val finishReason = response.choices.firstOrNull()?.finishReason
                    logger.i { "Response finish_reason: $finishReason" }

                    // Special handling: if file request and model returned stop without tool calls
                    if (finishReason == "stop" && isFileRequest && !response.hasToolCalls()) {
                        if (iteration == 1) {
                            // First attempt - model skipped tool, add explicit instruction
                            logger.w { "Model skipped tool call on file request! Adding explicit instruction." }
                            toolSkippedOnFileRequest = true

                            // Add system message demanding tool use
                            currentMessages.add(MessageDto(
                                role = "system",
                                content = """
                                    CRITICAL ERROR: You were asked to perform a file operation but did not call any tool.

                                    The user asked: "$message"

                                    You MUST call filesystem_read_file tool NOW with the correct path.
                                    DO NOT respond with text. Call the tool first.
                                """.trimIndent()
                            ))
                            continue  // Retry
                        } else if (toolSkippedOnFileRequest) {
                            // Second attempt still failed - accept the response but log warning
                            logger.w { "Model still not using tools after explicit instruction, accepting response" }
                        }
                    }

                    // IMPORTANT: Check tool_calls BEFORE finish_reason!
                    // DeepSeek can return both finish_reason="stop" AND tool_calls
                    // We must process tool_calls first, then check finish_reason

                    // Check if response has tool calls
                    if (response.hasToolCalls()) {
                        // Execute all tool calls
                        val toolCalls = response.getAllToolCalls()
                        logger.i { "Response contains ${toolCalls.size} tool calls: ${toolCalls.map { "${it.function.name}(${it.id})" }}" }

                        // Enhanced loop detection: check for patterns in history
                        val currentToolSignature = toolCalls.joinToString(";") {
                            "${it.function.name}:${it.function.arguments.hashCode()}"
                        }

                        // Check for immediate repetition - add warning BEFORE assistant message
                        val lastSignature = toolCallOrder.lastOrNull()
                        if (currentToolSignature == lastSignature) {
                            logger.w { "Detected immediate repeated tool call, forcing text response" }
                            // Add system message BEFORE assistant message to maintain message order
                            currentMessages.add(MessageDto(
                                role = "system",
                                content = "IMPORTANT: You just called this exact tool. Provide a text response to the user NOW without calling any more tools."
                            ))
                        }
                        // Check for loop pattern (A->B->A pattern)
                        else if (currentToolSignature in toolCallHistory) {
                            logger.w { "Detected tool call loop pattern (signature seen before), forcing text response" }
                            // Add system message BEFORE assistant message to maintain message order
                            currentMessages.add(MessageDto(
                                role = "system",
                                content = "IMPORTANT: You are in a tool call loop. Stop calling tools and provide a text response to the user NOW."
                            ))
                        }

                        // Add assistant message with tool calls to history
                        // IMPORTANT: This must come AFTER any system messages and BEFORE tool results
                        // CRITICAL FIX: Create assistant message with ALL tool calls from response,
                        // not just from first choice. API may return tool_calls in different choices.
                        val assistantContent = response.choices.firstOrNull()?.message?.content
                        val assistantMessage = MessageDto(
                            role = "assistant",
                            content = assistantContent,
                            toolCalls = toolCalls  // Use toolCalls from getAllToolCalls() - ensures consistency
                        )
                        logger.d { "Adding assistant message with tool_calls: ${assistantMessage.toolCalls?.size ?: 0} calls" }
                        currentMessages.add(assistantMessage)

                        // Update history with LRU eviction
                        toolCallHistory.add(currentToolSignature)
                        toolCallOrder.add(currentToolSignature)
                        if (toolCallOrder.size > toolLoopHistorySize) {
                            val removed = toolCallOrder.removeAt(0)
                            toolCallHistory.remove(removed)
                        }

                        // === Orchestrated Tool Execution with Parallelism ===
                        // Classify request type for orchestration
                        val requestType = when {
                            isFileRequest -> RequestType.FILE_OPERATION
                            else -> RequestType.UNKNOWN
                        }

                        // Create orchestration context
                        val orchestrationContext = OrchestrationContext(
                            sessionId = sessionId,
                            workingDirectory = getWorkingDirectory(),
                            recentToolCalls = emptyList() // Could be populated from toolCallOrder if needed
                        )

                        // Analyze and plan (optional - for complex workflows)
                        val planResult = orchestrator.analyzeAndPlan(message, orchestrationContext)
                        val plan = planResult.getOrElse { ExecutionPlan.default() }

                        logger.i { "Executing ${toolCalls.size} tools with orchestration (parallelize=${plan.canParallelize})" }

                        // Execute with dependencies (parallel where possible)
                        orchestrator.executeWithDependencies(toolCalls, plan)
                            .collect { result ->
                                val toolResultContent = result.result ?: result.error ?: "No result"
                                logger.i { "Tool ${result.toolName} (id=${result.toolCallId}) result (first 200 chars): ${toolResultContent.take(200)}" }

                                // Add tool result to messages
                                currentMessages.add(
                                    MessageDto.toolResult(
                                        toolCallId = result.toolCallId,
                                        name = result.toolName,
                                        content = toolResultContent
                                    )
                                )
                                logger.d { "Added tool result to messages, total messages: ${currentMessages.size}" }
                            }

                        logger.i { "Tool execution completed, continuing with ${currentMessages.size} messages" }
                        continue
                    }

                    // No tool calls - this is the final response
                    logger.i { "No tool calls in response (finish_reason=$finishReason), returning final response" }
                    finalResponse = response
                    finalContent = response.choices.firstOrNull()?.message?.content
                    break
                }

                // Check if we exceeded max iterations
                if (finalContent == null && iteration >= maxToolIterations) {
                    logger.w { "Max tool iterations reached ($maxToolIterations)" }
                    finalContent = "Превышено максимальное количество вызовов инструментов ($maxToolIterations). " +
                            "Возможно, возникла циклическая зависимость при обработке запроса. " +
                            "Попробуйте упростить запрос или задать вопрос иначе."
                }

                // === Validate AI response against invariants ===
                val aiResponse = finalContent
                if (aiResponse.isNullOrBlank()) {
                    logger.e { "Empty AI response content" }
                    messageDao.deleteMessageById(userMessage.id)
                    return@withContext ResultWrapper.Error(
                        throwable = IllegalStateException("Empty AI response content"),
                        message = "Received empty content from AI"
                    )
                }

                val responseValidation = validateInvariantViolationUseCase(
                    text = aiResponse,
                    checkType = CheckType.AI_RESPONSE
                )

                if (responseValidation.shouldBlock) {
                    logger.w { "AI response blocked by invariant: ${responseValidation.blockMessage}" }
                    messageDao.deleteMessageById(userMessage.id)

                    val blockedMessage = Message(
                        id = Uuid.random().toString(),
                        content = "Сгенерированный ответ нарушает инвариант проекта:\n\n${responseValidation.blockMessage}\n\nПожалуйста, уточните запрос.",
                        senderType = SenderType.SYSTEM,
                        timestamp = currentTimeMillis()
                    )

                    return@withContext ResultWrapper.Success(blockedMessage)
                }

                if (responseValidation.hasViolations) {
                    logger.w { "AI response has warnings: ${responseValidation.violations.size}" }
                }

                // Step 8: Create and save assistant message
                val assistantMessage = Message(
                    id = finalResponse?.id ?: Uuid.random().toString(),
                    content = aiResponse,
                    senderType = SenderType.ASSISTANT,
                    timestamp = currentTimeMillis()
                )

                messageDao.insertMessage(assistantMessage.toEntity(sessionId))
                logger.d { "Assistant message saved with ID: ${assistantMessage.id}" }

                chatSessionDao.incrementMessageCount(sessionId, currentTimeMillis())

                // Step 9: Update session title if this was first exchange
                val messageCount = messageDao.getMessageCount(sessionId)
                if (messageCount == 2) {
                    val newTitle = generateTitleFromMessage(message)
                    val session = chatSessionDao.getSessionById(sessionId)
                    if (session != null && session.title == "New Chat") {
                        chatSessionDao.updateSession(
                            session.copy(
                                title = newTitle,
                                updatedAt = currentTimeMillis()
                            )
                        )
                        logger.i { "Session title updated to: $newTitle" }
                    }
                }

                logger.i { "sendMessage completed successfully for session: $sessionId" }
                ResultWrapper.Success(assistantMessage)

            } catch (e: InvariantViolationException) {
                logger.e(throwable = e) { "Invariant violation in sendMessage" }
                ResultWrapper.Error(
                    throwable = e,
                    message = e.message ?: "Invariant violation"
                )
            } catch (e: Exception) {
                logger.e(throwable = e) { "Error sending message to ${llmApiClient.getProviderName()}" }
                networkErrorHandling.transformToResultWrapper(e)
            }
        }
    }

    /**
     * Send a message to LLM without saving to chat history.
     * Used for internal operations like planning and validation.
     *
     * @param sessionId Session ID for context
     * @param message Message to send
     * @param includeTools Whether to include MCP tool execution (default: false)
     * @return Result containing the AI response
     */
    override suspend fun sendSilentMessage(
        sessionId: String,
        message: String,
        includeTools: Boolean
    ): ResultWrapper<String> {
        logger.i { "sendSilentMessage called for session: $sessionId, includeTools: $includeTools" }

        return withContext(Dispatchers.IO) {
            try {
                // Validate USER_REQUEST before sending
                val requestValidation = validationService.validate(message, CheckType.USER_REQUEST)
                if (requestValidation.shouldBlock) {
                    val blockMsg = requestValidation.blockMessage ?: "Validation blocked"
                    logger.w { "User request blocked by invariant validation: $blockMsg" }
                    return@withContext ResultWrapper.Error(
                        throwable = InvariantViolationException(blockMsg),
                        message = blockMsg
                    )
                }

                // Prepare messages with or without tools based on parameter
                val currentMessages = prepareMessages(sessionId, additionalMessage = message, includeTools = includeTools).toMutableList()

                logger.i { "Sending silent request to ${llmApiClient.getProviderName()} with ${currentMessages.size} messages" }

                // Tool call loop handling (similar to sendMessage but without DB persistence)
                var iteration = 0
                var finalContent: String? = null

                while (iteration < maxToolIterations) {
                    iteration++

                    // Build request with or without tools
                    val response = if (includeTools) {
                        val tools = toolExecutor.getToolsForApi()
                        logger.i { "Silent request WITH ${tools.size} tools (iteration: $iteration)" }

                        // CRITICAL: Validate and fix message order before sending to API
                        val validatedMessages = validateMessageOrder(currentMessages)
                        if (validatedMessages.size != currentMessages.size) {
                            logger.w { "Silent: Message order validation removed ${currentMessages.size - validatedMessages.size} orphaned messages" }
                            currentMessages.clear()
                            currentMessages.addAll(validatedMessages)
                        }

                        if (tools.isEmpty()) {
                            llmApiClient.sendMessage(ChatRequest.simple(messages = currentMessages))
                        } else {
                            llmApiClient.sendMessage(ChatRequest.withTools(
                                messages = currentMessages,
                                tools = tools
                            ))
                        }
                    } else {
                        llmApiClient.sendMessage(ChatRequest(messages = currentMessages))
                    }

                    if (response.choices.isEmpty()) {
                        logger.e { "Empty response from API" }
                        return@withContext ResultWrapper.Error(
                            throwable = IllegalStateException("Empty response from API"),
                            message = "Received empty response from LLM API"
                        )
                    }

                    // Check finish_reason
                    val finishReason = response.choices.firstOrNull()?.finishReason
                    logger.i { "Silent response finish_reason: $finishReason" }

                    // Check if response has tool calls (process BEFORE checking finish_reason)
                    if (response.hasToolCalls()) {
                        val toolCalls = response.getAllToolCalls()
                        logger.i { "Silent response contains ${toolCalls.size} tool calls: ${toolCalls.map { "${it.function.name}(${it.id})" }}" }

                        // Add assistant message with tool calls to current messages
                        // CRITICAL FIX: Create assistant message with ALL tool calls from response
                        val assistantContent = response.choices.firstOrNull()?.message?.content
                        val assistantMessage = MessageDto(
                            role = "assistant",
                            content = assistantContent,
                            toolCalls = toolCalls  // Use toolCalls from getAllToolCalls() - ensures consistency
                        )
                        logger.d { "Adding assistant message with tool_calls: ${assistantMessage.toolCalls?.size ?: 0} calls" }
                        currentMessages.add(assistantMessage)

                        // Create orchestration context
                        val orchestrationContext = OrchestrationContext(
                            sessionId = sessionId,
                            workingDirectory = getWorkingDirectory(),
                            recentToolCalls = emptyList()
                        )

                        // Analyze and plan
                        val planResult = orchestrator.analyzeAndPlan(message, orchestrationContext)
                        val plan = planResult.getOrElse { ExecutionPlan.default() }

                        logger.i { "Executing ${toolCalls.size} tools with orchestration (parallelize=${plan.canParallelize})" }

                        // Execute with dependencies
                        orchestrator.executeWithDependencies(toolCalls, plan)
                            .collect { result ->
                                val toolResultContent = result.result ?: result.error ?: "No result"
                                logger.i { "Tool ${result.toolName} (id=${result.toolCallId}) result (first 200 chars): ${toolResultContent.take(200)}" }

                                currentMessages.add(
                                    MessageDto.toolResult(
                                        toolCallId = result.toolCallId,
                                        name = result.toolName,
                                        content = toolResultContent
                                    )
                                )
                            }

                        logger.i { "Tool execution completed, continuing with ${currentMessages.size} messages" }
                        continue
                    }

                    // No tool calls - this is the final response
                    logger.i { "No tool calls in silent response (finish_reason=$finishReason), returning final response" }
                    finalContent = response.choices.firstOrNull()?.message?.content
                    break
                }

                // Check if we exceeded max iterations
                if (finalContent == null && iteration >= maxToolIterations) {
                    logger.w { "Max tool iterations reached ($maxToolIterations) in silent message" }
                    finalContent = "Превышено максимальное количество вызовов инструментов."
                }

                if (finalContent.isNullOrBlank()) {
                    logger.e { "Empty response content in silent message" }
                    return@withContext ResultWrapper.Error(
                        throwable = IllegalStateException("Empty response content"),
                        message = "Received empty content from AI"
                    )
                }

                // Validate AI_RESPONSE after receiving
                val responseValidation = validationService.validate(finalContent, CheckType.AI_RESPONSE)
                if (responseValidation.shouldBlock) {
                    val blockMsg = responseValidation.blockMessage ?: "AI response blocked by validation"
                    logger.w { "AI response blocked by invariant validation: $blockMsg" }
                    throw InvariantViolationException(blockMsg)
                }

                // Log warnings if any
                if (responseValidation.warnings.isNotEmpty()) {
                    logger.w { "AI response has warnings: ${responseValidation.warnings.size}" }
                }
                if (requestValidation.warnings.isNotEmpty()) {
                    logger.w { "User request has warnings: ${requestValidation.warnings.size}" }
                }

                logger.i { "Silent request completed successfully" }
                ResultWrapper.Success(finalContent)

            } catch (e: Exception) {
                logger.e(throwable = e) { "Error in silent message to ${llmApiClient.getProviderName()}" }
                networkErrorHandling.transformToResultWrapper(e)
            }
        }
    }

    /**
     * Save a message directly to chat history without sending to LLM.
     *
     * @param sessionId Session ID
     * @param message Message to save
     */
    override suspend fun saveMessage(sessionId: String, message: Message) {
        withContext(Dispatchers.IO) {
            logger.d { "saveMessage: Saving message to session $sessionId" }
            messageDao.insertMessage(message.toEntity(sessionId))
            chatSessionDao.incrementMessageCount(sessionId, currentTimeMillis())
            logger.d { "Message saved with ID: ${message.id}" }
        }
    }

    override suspend fun getChatHistory(sessionId: String): List<Message> {
        return withContext(Dispatchers.IO) {
            val messages = messageDao.getMessagesForSession(sessionId).toDomain()
            logger.d { "getChatHistory: Loaded ${messages.size} messages for session $sessionId" }
            messages
        }
    }

    override fun getChatHistoryFlow(sessionId: String): Flow<List<Message>> {
        logger.d { "getChatHistoryFlow: Subscribing to messages flow for session $sessionId" }
        return messageDao.getMessagesForSessionFlow(sessionId)
            .map { entities -> entities.toDomain() }
    }

    override suspend fun clearHistory(sessionId: String) {
        withContext(Dispatchers.IO) {
            logger.i { "clearHistory: Clearing messages for session $sessionId" }
            messageDao.deleteMessagesForSession(sessionId)

            val session = chatSessionDao.getSessionById(sessionId)
            if (session != null) {
                chatSessionDao.updateSession(
                    session.copy(
                        messageCount = 0,
                        updatedAt = currentTimeMillis()
                    )
                )
            }

            logger.i { "clearHistory: Messages cleared for session $sessionId" }
        }
    }

    override suspend fun getOptimizedContext(sessionId: String): OptimizedContext {
        val messages = getChatHistory(sessionId)
        val optimized = contextOptimizer.optimize(messages)

        logger.d {
            "Optimized context for session: $sessionId. " +
            "Strategy: ${optimized.strategy}, " +
            "Tokens: ${optimized.estimatedTokens}, " +
            "Truncated: ${optimized.truncatedCount}"
        }

        return optimized
    }

    /**
     * Generate a session title from the first user message.
     * Takes first 50 characters or first line, whichever is shorter.
     *
     * @param message User message to generate title from
     * @return Generated title
     */
    private fun generateTitleFromMessage(message: String): String {
        val firstLine = message.lines().firstOrNull() ?: message
        return if (firstLine.length > 50) {
            firstLine.take(47) + "..."
        } else {
            firstLine
        }
    }
}
