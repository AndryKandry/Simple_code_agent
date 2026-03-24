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
import ru.agent.features.chat.data.remote.DeepSeekApiClient
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
import ru.agent.features.rag.domain.formatter.RagResponseFormatter
import ru.agent.features.rag.domain.model.RagConfig
import ru.agent.features.rag.domain.model.RagResponse
import ru.agent.mcp.orchestration.ExecutionPlan
import ru.agent.mcp.orchestration.McpOrchestrator
import ru.agent.mcp.orchestration.OrchestrationContext
import ru.agent.mcp.orchestration.RequestType
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Implementation of ChatRepository using Room database for persistent storage.
 *
 * Uses DeepSeek API for AI responses and stores all messages in local database.
 * Includes context optimization to manage token limits.
 * Supports function calling (tools) via MCP integration.
 *
 * @property deepSeekApiClient API client for DeepSeek
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
    private val deepSeekApiClient: DeepSeekApiClient,
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
     * Validate tool call ID and log error if invalid.
     * @return true if valid, false otherwise
     */
    private fun isValidToolCallId(
        toolCallId: String,
        validToolCallIds: Set<String>,
        context: String = ""
    ): Boolean {
        if (toolCallId !in validToolCallIds) {
            logger.e { "[$context] Invalid tool call ID: $toolCallId. Not found in assistant message tool_calls." }
            return false
        }
        return true
    }

    /**
     * Build tool instructions system prompt dynamically based on available tools.
     *
     * @param workingDirectory Current working directory (FULL ABSOLUTE PATH)
     * @param tools List of available tools
     * @param hasRagContext Whether RAG context is available for this request
     * @return Formatted system prompt with tool instructions
     */
    private suspend fun buildToolInstructions(
        workingDirectory: String,
        tools: List<ToolDefinitionDto>,
        hasRagContext: Boolean = false
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

        // Build RAG section if context is available
        val ragSection = if (hasRagContext) {
            """

            ╔══════════════════════════════════════════════════════════════════════════════╗
            ║              RAG CONTEXT - RETRIEVAL-AUGMENTED GENERATION                    ║
            ╠══════════════════════════════════════════════════════════════════════════════╣
            ║ YOU HAVE RAG CONTEXT in "RELEVANT CODE CONTEXT" section below.               ║
            ║ This contains ACTUAL code snippets from the codebase matching your query.    ║
            ╚══════════════════════════════════════════════════════════════════════════════╝

            ╔══════════════════════════════════════════════════════════════════════════════╗
            ║  MANDATORY RAG USAGE RULES - NO EXCEPTIONS                                   ║
            ╠══════════════════════════════════════════════════════════════════════════════╣
            ║ 1. USE file paths EXACTLY as shown in RAG context (they are REAL paths)      ║
            ║ 2. DO NOT fabricate, guess, or hallucinate ANY file paths                    ║
            ║ 3. DO NOT invent files that are not in RAG context or tool results           ║
            ║ 4. When referencing code, use paths from RAG context verbatim                ║
            ║ 5. If RAG doesn't have the info, say so - don't make things up              ║
            ╚══════════════════════════════════════════════════════════════════════════════╝

            RAG CONTEXT STRUCTURE:
            Each snippet shows:
            - Source: EXACT file path (use this path verbatim)
            - Lines: Line numbers in the file
            - Similarity: How relevant (0.0-1.0, higher = more relevant)
            - Content: Actual code from that file

            HOW TO RESPOND:
            ✓ CORRECT: "According to RequestClassifier.kt (lines 45-67)..."
            ✗ WRONG: "In src/utils/classifier.ts..." (if not in RAG context)
            ✗ WRONG: Making up file paths that weren't in RAG results

            WHEN RAG CONTEXT IS INSUFFICIENT:
            - Say "Based on the indexed codebase, I found..."
            - Use filesystem tools to explore further if needed
            - Never fabricate information not present in RAG or tool results

            WHEN RAG CONTEXT IS SUFFICIENT:
            - Answer DIRECTLY using RAG context - DO NOT call tools
            - Tools are ONLY for when RAG doesn't have the information
            - If RAG shows the answer, respond immediately without tool calls
            """
        } else {
            ""
        }

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
            ║ BUT: If RAG context already has the answer, respond directly without tools.  ║
            ╚══════════════════════════════════════════════════════════════════════════════╝

            === MANDATORY TOOL USAGE ===

            If user mentions ANY of these actions, you MUST call the tool FIRST:
            - "read", "show", "open", "display", "what is in", "contents of" + file name
            - "list", "show files", "directory contents", "what files"
            - "write", "create", "modify", "edit" + file name
            - "search", "find", "look for" + file pattern
            - "remind", "reminder", "schedule", "alarm", "notification", "notify"
            - "cron", "periodic", "recurring task", "scheduled task"

            EXCEPTION: If RAG context already contains the exact file/info needed, respond
            directly using RAG context WITHOUT calling tools.

            BEFORE responding with ANY file content (when RAG doesn't have it):
            1. Call filesystem_read_file(path="FULL_ABSOLUTE_PATH")
            2. WAIT for the tool result
            3. ONLY THEN use the ACTUAL content from the tool result

            IF YOU RESPOND WITH FILE CONTENT WITHOUT CALLING THE TOOL FIRST (and no RAG), YOU ARE WRONG.

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
            $ragSection

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
     * Detect if user message requires RAG context enrichment.
     * Used to determine if we should use RAG for semantic code search.
     *
     * @param message User message to analyze
     * @param requestType Pre-classified request type (optional)
     * @return true if message likely benefits from RAG
     */
    private fun isRagRequest(message: String, requestType: RequestType? = null): Boolean {
        logger.d { "isRagRequest called: message='${message.take(50)}...', requestType=$requestType" }

        // If request is already classified as RAG_REQUEST, use it
        if (requestType == RequestType.RAG_REQUEST) {
            logger.d { "isRagRequest: RAG_REQUEST classified, enabling RAG" }
            return true
        }

        // Exclude file/terminal operations - they use MCP tools
        if (requestType in listOf(
            RequestType.FILE_OPERATION,
            RequestType.TERMINAL_COMMAND,
            RequestType.GIT_OPERATION,
            RequestType.SCHEDULING
        )) {
            logger.d { "isRagRequest: $requestType excludes RAG" }
            return false
        }

        // Simple heuristics to exclude trivial queries
        val lowerMessage = message.lowercase().trim()
        val trivialPatterns = listOf(
            Regex("^(hi|hello|hey|привет|здравствуй|хай)[\\s!?.]*$"),
            Regex("^(how are you|как дела|как ты)[\\s!?.]*$"),
            Regex("^(thanks|thank you|спасибо|благодарю)[\\s!?.]*$"),
            Regex("^(yes|no|да|нет|ок|ok)[\\s!?.]*$")
        )

        if (trivialPatterns.any { it.matches(lowerMessage) }) {
            logger.d { "isRagRequest: trivial query, disabling RAG" }
            return false
        }

        // For UNKNOWN type, check for RAG-like keywords
        if (requestType == RequestType.UNKNOWN || requestType == null) {
            val ragKeywords = listOf(
                "where", "где", "how", "как", "architecture", "архитектура",
                "implementation", "реализация", "class", "класс", "function", "функция",
                "explain", "объясни", "find all", "найди все", "usage", "использование",
                "code", "код", "project", "проект", "system", "система",
                "source", "источник", "model", "модель", "service", "сервис",
                "repository", "usecase", "chunk", "rag", "embedding"
            )
            val hasRagKeywords = ragKeywords.any { lowerMessage.contains(it) }
            logger.d { "isRagRequest: UNKNOWN type, ragKeywords=$hasRagKeywords, lowerMessage='$lowerMessage'" }
            return hasRagKeywords
        }

        // Default: use RAG for CODE_ANALYSIS and MULTI_TYPE
        val result = requestType in listOf(RequestType.CODE_ANALYSIS, RequestType.MULTI_TYPE)
        logger.d { "isRagRequest: $requestType, result=$result" }
        return result
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
     * Result of message preparation with RAG context.
     *
     * @property messages Prepared messages for API
     * @property ragChunks RAG chunks that were used for context (empty if RAG disabled or no results)
     * @property isDontKnowMode True if max similarity is below relevance threshold (should return "don't know" response)
     * @property ragResponse Structured RAG response with sources and citations (null if RAG disabled)
     */
    private data class PreparedMessagesResult(
        val messages: MutableList<MessageDto>,
        val ragChunks: List<ru.agent.features.rag.domain.model.ChunkScore>,
        val isDontKnowMode: Boolean = false,
        val ragResponse: ru.agent.features.rag.domain.model.RagResponse? = null
    )

    /**
     * Prepare messages for API request with system prompt and context.
     *
     * @param sessionId Session ID for context
     * @param additionalMessage Optional additional message to add
     * @param includeTools Whether to include tool instructions in system prompt
     * @param ragEnabled Enable RAG for context enrichment
     * @param searchQuery Optional search query for RAG (defaults to message if not provided)
     * @param requestType Pre-classified request type for intelligent RAG usage
     * @return PreparedMessagesResult with messages and RAG chunks
     */
    private suspend fun prepareMessages(
        sessionId: String,
        additionalMessage: String? = null,
        includeTools: Boolean = false,
        ragEnabled: Boolean = true,
        searchQuery: String? = null,
        requestType: RequestType? = null
    ): PreparedMessagesResult {
        // Determine if RAG should be used based on classification
        val shouldUseRag = ragEnabled && when {
            additionalMessage == null -> false // No message - no RAG
            else -> isRagRequest(additionalMessage, requestType)
        }

        val optimizedContext = getOptimizedContext(sessionId)
        val memoryContext = getMemoryContextUseCase(
            sessionId = sessionId,
            searchQuery = if (shouldUseRag) searchQuery ?: additionalMessage else null,
            ragEnabled = shouldUseRag
        )
        val systemPrompt = memoryContext.toSystemPrompt()
        val hasRagContext = memoryContext.relevantChunks.isNotEmpty()
        logger.d { "RAG context: hasRagContext=$hasRagContext, chunks=${memoryContext.relevantChunks.size}" }

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
                append(buildToolInstructions(workingDirectory, tools, hasRagContext))
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

        logger.i {
            "Prepared messages: ${messages.size} total, RAG=$shouldUseRag, " +
            "chunks=${memoryContext.relevantChunks.size}"
        }

        // Validate message order - remove orphaned tool messages
        // This is a safety net since DB doesn't store tool_calls info
        val validatedMessages = validateMessageOrder(messages).toMutableList()

        // Use RagResponse from MemoryContext (already created by GetMemoryContextUseCase)
        val ragResponse = memoryContext.ragResponse

        // Check "don't know" mode using RagResponse.hasRelevantContext directly
        // This is consistent with how RagResponse was created in GetMemoryContextUseCase
        // The relevance check is already embedded in RagResponse.fromChunks()
        val isDontKnowMode = shouldUseRag && ragResponse?.shouldRespondWithDontKnow() == true

        if (isDontKnowMode) {
            val maxSimilarity = ragResponse?.maxSimilarity ?: 0f
            logger.w { "RAG 'don't know' mode activated: maxSimilarity=$maxSimilarity, hasRelevantContext=false" }
        }

        // Return both messages and RAG chunks for sources
        return PreparedMessagesResult(
            messages = validatedMessages,
            ragChunks = memoryContext.relevantChunks,
            isDontKnowMode = isDontKnowMode,
            ragResponse = ragResponse
        )
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
    override suspend fun sendMessage(
        sessionId: String,
        message: String,
        ragEnabled: Boolean,
        searchQuery: String?,
        includeTools: Boolean,
        skipInvariantValidation: Boolean,
        skipMarkdownFormatting: Boolean
    ): ResultWrapper<Message> {
        logger.i { "sendMessage called for session: $sessionId, message: ${message.take(50)}..., ragEnabled: $ragEnabled, skipInvariantValidation: $skipInvariantValidation" }

        return withContext(Dispatchers.IO) {
            try {
                // Step 0: Ensure session exists
                ensureSessionExists(sessionId)

                // === Validate user request against invariants ===
                if (!skipInvariantValidation) {
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
                }

                // Classify request type for intelligent RAG usage
                val classifier = ru.agent.mcp.orchestration.RequestClassifier()
                val requestType = classifier.classify(message)
                logger.i { "Request classified as: $requestType" }

                // Determine if RAG should be used based on classification
                val shouldUseRag = ragEnabled && isRagRequest(message, requestType)
                logger.i { "RAG decision: enabled=$ragEnabled, shouldUse=$shouldUseRag, type=$requestType" }

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

                // Step 4-6: Prepare messages with tools and RAG
                val prepareResult = prepareMessages(
                    sessionId = sessionId,
                    additionalMessage = message,  // <-- FIX: Pass message for RAG
                    includeTools = includeTools,
                    ragEnabled = shouldUseRag,
                    searchQuery = searchQuery ?: message,
                    requestType = requestType
                )
                val currentMessages = prepareResult.messages
                val ragChunks = prepareResult.ragChunks
                val isDontKnowMode = prepareResult.isDontKnowMode
                val ragResponse = prepareResult.ragResponse
                logger.d { "RAG chunks after prepareMessages: ${ragChunks.size}, isDontKnowMode=$isDontKnowMode" }

                // === Handle "don't know" mode - return response WITHOUT calling LLM ===
                if (isDontKnowMode && ragResponse != null) {
                    logger.i { "RAG 'don't know' mode: skipping LLM call, returning predefined response" }

                    // Safe access to ragResponse.answer with fallback to default "don't know" message
                    val dontKnowContent = ragResponse.answer.ifBlank {
                        RagResponse.dontKnow("").answer
                    }

                    // Create and save assistant message with "don't know" response
                    val assistantMessage = Message(
                        id = Uuid.random().toString(),
                        content = dontKnowContent,
                        senderType = SenderType.ASSISTANT,
                        timestamp = currentTimeMillis(),
                        sources = null // No sources for "don't know" mode
                    )

                    messageDao.insertMessage(assistantMessage.toEntity(sessionId))
                    chatSessionDao.incrementMessageCount(sessionId, currentTimeMillis())

                    // Update session title if this was first exchange
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

                    logger.i { "sendMessage completed in 'don't know' mode for session: $sessionId" }
                    return@withContext ResultWrapper.Success(assistantMessage)
                }

                logger.i {
                    "Sending request to DeepSeek API with ${currentMessages.size} messages " +
                    "(RAG=$shouldUseRag, type=$requestType)"
                }

                // Detect if user is asking for file operations
                val isFileRequest = isFileOperationRequest(message)
                if (isFileRequest && includeTools) {
                    logger.i { "Detected file operation request, will require tool usage" }
                }

                // Step 7: Call DeepSeek API
                var iteration = 0
                var finalResponse: ru.agent.features.chat.data.remote.dto.ChatResponse? = null
                var finalContent: String? = null

                // If tools are disabled, send simple request without tool loop
                if (!includeTools) {
                    logger.i { "Tools disabled, sending simple request (RAG=$shouldUseRag)" }
                    val simpleResponse = try {
                        deepSeekApiClient.sendMessage(ChatRequest.simple(messages = currentMessages))
                    } catch (e: Exception) {
                        logger.e(throwable = e) { "Error during simple API request: ${e.message}" }
                        messageDao.deleteMessageById(userMessage.id)
                        return@withContext ResultWrapper.Error(
                            throwable = e,
                            message = e.message ?: "API request failed"
                        )
                    }

                    if (simpleResponse.choices.isEmpty()) {
                        logger.e { "Empty response from API" }
                        messageDao.deleteMessageById(userMessage.id)
                        return@withContext ResultWrapper.Error(
                            throwable = IllegalStateException("Empty response from API"),
                            message = "Received empty response from DeepSeek API"
                        )
                    }

                    finalResponse = simpleResponse
                    finalContent = simpleResponse.choices.firstOrNull()?.message?.content
                } else {
                    // Tools enabled - use tool call loop
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
                            deepSeekApiClient.sendMessage(requestSimple)
                        } else if (shouldForceToolUse) {
                            // Force tool usage for file operations
                            logger.i { "Forcing tool usage with tool_choice=required" }
                            val requestWithRequiredTools = ChatRequest.withRequiredTools(
                                messages = currentMessages,
                                tools = tools
                            )
                            deepSeekApiClient.sendMessage(requestWithRequiredTools)
                        } else {
                            val requestWithTools = ChatRequest.withTools(
                                messages = currentMessages,
                                tools = tools
                            )
                            deepSeekApiClient.sendMessage(requestWithTools)
                        }
                    } catch (e: Exception) {
                        logger.e(throwable = e) { "Error during API request: ${e.message}" }
                        messageDao.deleteMessageById(userMessage.id)
                        return@withContext ResultWrapper.Error(
                            throwable = e,
                            message = e.message ?: "API request failed"
                        )
                    }
                    logger.i { "Received response from DeepSeek API. ID: ${response.id}, choices: ${response.choices.size}, iteration: $iteration" }

                    // Validate response
                    if (response.choices.isEmpty()) {
                        logger.e { "Empty response from API" }
                        messageDao.deleteMessageById(userMessage.id)
                        return@withContext ResultWrapper.Error(
                            throwable = IllegalStateException("Empty response from API"),
                            message = "Received empty response from DeepSeek API"
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
                        // IMPORTANT: Use only first choice for consistency with assistantContent
                        val firstChoice = response.choices.firstOrNull()
                        val toolCalls = firstChoice?.message?.toolCalls ?: emptyList()
                        logger.i { "Response contains ${toolCalls.size} tool calls: ${toolCalls.map { "${it.function.name}(${it.id})" }}" }

                        // Enhanced loop detection: check for patterns in history
                        // CRITICAL FIX: Use full argument string instead of hashCode() to prevent hash collisions
                        // Hash collisions can cause false positives where different tool calls are incorrectly identified as loops
                        val currentToolSignature = toolCalls.joinToString(";") {
                            "${it.function.name}:${it.function.arguments}"
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
                        // Use assistantContent from first choice (same choice as toolCalls)
                        val assistantContent = firstChoice?.message?.content
                        val assistantMessage = MessageDto(
                            role = "assistant",
                            content = assistantContent,
                            toolCalls = toolCalls  // Use toolCalls from first choice - ensures consistency
                        )
                        logger.d { "Adding assistant message with tool_calls: ${assistantMessage.toolCalls?.size ?: 0} calls" }
                        currentMessages.add(assistantMessage)

                        // Collect valid tool call IDs for verification
                        // Only accept tool results that reference these IDs
                        val validToolCallIds = toolCalls.map { it.id }.toSet()
                        logger.d { "Valid tool call IDs: ${validToolCallIds.joinToString()}" }

                        // Update history with LRU eviction
                        toolCallHistory.add(currentToolSignature)
                        toolCallOrder.add(currentToolSignature)
                        if (toolCallOrder.size > toolLoopHistorySize) {
                            val removed = toolCallOrder.removeAt(0)
                            toolCallHistory.remove(removed)
                        }

                        // === Orchestrated Tool Execution with Parallelism ===
                        // Use orchestration-specific request type (not the RAG classification)
                        val orchestrationRequestType = when {
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
                                // Verify that tool call ID exists in assistant message
                                if (!isValidToolCallId(result.toolCallId, validToolCallIds, "sendMessage")) {
                                    return@collect
                                }

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
                } // end of else block (tools enabled)

                // Check if we exceeded max iterations (only relevant when tools are enabled)
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

                // === Validate AI response against invariants ===
                if (!skipInvariantValidation) {
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
                }

                // Step 8: Format response with sources and citations
                val messageSources = ragChunks.takeIf { it.isNotEmpty() }

                val formattedContent = when {
                    skipMarkdownFormatting -> {
                        // For mini-chat: skip markdown formatting, sources are displayed separately
                        logger.d { "Skipping markdown formatting for mini-chat" }
                        aiResponse
                    }
                    !messageSources.isNullOrEmpty() -> {
                        logger.d { "Formatting response with ${messageSources.size} sources and citations" }
                        try {
                            RagResponseFormatter.appendSourcesAndCitations(
                                answer = aiResponse,
                                chunks = messageSources,
                                relevanceThreshold = RagConfig().relevanceThreshold,
                                maxCitations = RagResponse.DEFAULT_MAX_CITATIONS
                            )
                        } catch (e: Exception) {
                            logger.e(throwable = e) { "Error formatting RAG response with sources: ${e.message}" }
                            // Fallback to raw AI response on formatting error
                            aiResponse
                        }
                    }
                    else -> {
                        logger.d { "No RAG sources, using raw AI response" }
                        aiResponse
                    }
                }

                logger.d { "Creating assistant message with sources: ${messageSources?.size ?: 0} chunks" }
                val assistantMessage = Message(
                    id = finalResponse?.id ?: Uuid.random().toString(),
                    content = formattedContent,
                    senderType = SenderType.ASSISTANT,
                    timestamp = currentTimeMillis(),
                    // Include RAG sources if available
                    sources = messageSources
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
                logger.e(throwable = e) { "Error sending message to DeepSeek API" }
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
                val prepareResult = prepareMessages(sessionId, additionalMessage = message, includeTools = includeTools)
                val currentMessages = prepareResult.messages

                logger.i { "Sending silent request to DeepSeek API with ${currentMessages.size} messages" }

                // Detect if user is asking for file operations
                val isFileRequest = isFileOperationRequest(message)
                if (isFileRequest && includeTools) {
                    logger.i { "Silent: Detected file operation request, will require tool usage" }
                }

                // Tool call loop handling (similar to sendMessage but without DB persistence)
                var iteration = 0
                var finalContent: String? = null

                while (iteration < maxToolIterations) {
                    iteration++

                    // Build request with or without tools
                    val response = if (includeTools) {
                        val tools = toolExecutor.getToolsForApi()
                        val shouldForceToolUse = isFileRequest && iteration == 1
                        logger.i { "Silent request WITH ${tools.size} tools (iteration: $iteration, forceTool: $shouldForceToolUse)" }

                        // CRITICAL: Validate and fix message order before sending to API
                        val validatedMessages = validateMessageOrder(currentMessages)
                        if (validatedMessages.size != currentMessages.size) {
                            logger.w { "Silent: Message order validation removed ${currentMessages.size - validatedMessages.size} orphaned messages" }
                            currentMessages.clear()
                            currentMessages.addAll(validatedMessages)
                        }

                        if (tools.isEmpty()) {
                            logger.w { "Silent: No tools available, falling back to simple request" }
                            deepSeekApiClient.sendMessage(ChatRequest.simple(messages = currentMessages))
                        } else if (shouldForceToolUse) {
                            // Force tool usage for file operations
                            logger.i { "Silent: Forcing tool usage with tool_choice=required" }
                            deepSeekApiClient.sendMessage(ChatRequest.withRequiredTools(
                                messages = currentMessages,
                                tools = tools
                            ))
                        } else {
                            deepSeekApiClient.sendMessage(ChatRequest.withTools(
                                messages = currentMessages,
                                tools = tools
                            ))
                        }
                    } else {
                        deepSeekApiClient.sendMessage(ChatRequest(messages = currentMessages))
                    }

                    if (response.choices.isEmpty()) {
                        logger.e { "Empty response from API" }
                        return@withContext ResultWrapper.Error(
                            throwable = IllegalStateException("Empty response from API"),
                            message = "Received empty response from DeepSeek API"
                        )
                    }

                    // Check finish_reason
                    val finishReason = response.choices.firstOrNull()?.finishReason
                    logger.i { "Silent response finish_reason: $finishReason" }

                    // Check if response has tool calls (process BEFORE checking finish_reason)
                    if (response.hasToolCalls()) {
                        // IMPORTANT: Use only first choice for consistency with assistantContent
                        val firstChoice = response.choices.firstOrNull()
                        val toolCalls = firstChoice?.message?.toolCalls ?: emptyList()
                        logger.i { "Silent response contains ${toolCalls.size} tool calls: ${toolCalls.map { "${it.function.name}(${it.id})" }}" }

                        // Add assistant message with tool calls to current messages
                        // Use assistantContent from first choice (same choice as toolCalls)
                        val assistantContent = firstChoice?.message?.content
                        val assistantMessage = MessageDto(
                            role = "assistant",
                            content = assistantContent,
                            toolCalls = toolCalls  // Use toolCalls from first choice - ensures consistency
                        )
                        logger.d { "Adding assistant message with tool_calls: ${assistantMessage.toolCalls?.size ?: 0} calls" }
                        currentMessages.add(assistantMessage)

                        // Collect valid tool call IDs for verification
                        val validToolCallIds = toolCalls.map { it.id }.toSet()
                        logger.d { "Silent: Valid tool call IDs: ${validToolCallIds.joinToString()}" }

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
                                // Verify that tool call ID exists in assistant message
                                if (!isValidToolCallId(result.toolCallId, validToolCallIds, "sendSilentMessage")) {
                                    return@collect
                                }

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
                logger.e(throwable = e) { "Error in silent message to DeepSeek API" }
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
