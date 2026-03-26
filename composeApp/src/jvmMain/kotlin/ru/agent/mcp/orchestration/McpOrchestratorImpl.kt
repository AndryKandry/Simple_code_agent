package ru.agent.mcp.orchestration

import co.touchlab.kermit.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import ru.agent.features.chat.data.remote.dto.FunctionCallDto
import ru.agent.features.chat.data.remote.dto.ToolCallDto
import ru.agent.features.chat.data.remote.dto.ToolDefinitionDto
import ru.agent.features.chat.domain.tools.ToolExecutor
import ru.agent.mcp.McpManager
import ru.agent.mcp.ServerType
import java.util.UUID

/**
 * Implementation of McpOrchestrator.
 *
 * Coordinates MCP tool execution across multiple servers using:
 * - RequestClassifier for determining request type
 * - PlanningService for LLM-driven planning
 * - ToolExecutor for actual tool execution
 * - McpManager for server management
 *
 * @property mcpManager Central MCP manager
 * @property toolExecutor Tool executor for running tools
 * @property requestClassifier Classifier for request types
 * @property planningService LLM-driven planning service
 */
class McpOrchestratorImpl(
    private val mcpManager: McpManager,
    private val toolExecutor: ToolExecutor,
    private val requestClassifier: RequestClassifier,
    private val planningService: PlanningService
) : McpOrchestrator {

    private val logger = Logger.withTag("McpOrchestrator")
    private val json = Json { ignoreUnknownKeys = true }

    // Server name prefixes for built-in servers
    private val builtInServerPrefixes = mapOf(
        "filesystem" to ServerType.BUILT_IN,
        "terminal" to ServerType.BUILT_IN,
        "scheduler" to ServerType.BUILT_IN
    )

    override suspend fun analyzeAndPlan(
        message: String,
        context: OrchestrationContext
    ): Result<ExecutionPlan> {
        logger.i { "Analyzing message for session: ${context.sessionId}" }

        return try {
            // Step 1: Classify the request
            val requestType = requestClassifier.classify(message)
            logger.d { "Request classified as: $requestType" }

            // Step 2: Get relevant tools
            val relevantTools = getRelevantTools(requestType)
            logger.d { "Found ${relevantTools.size} relevant tools" }

            // Step 3: Create plan using LLM
            val planningRequest = PlanningRequest(
                userMessage = message,
                context = context,
                availableTools = relevantTools
            )

            val planningResult = planningService.createPlan(planningRequest)

            planningResult.fold(
                onSuccess = { response ->
                    logger.i { "Plan created with ${response.steps.size} steps, parallelize=${response.canParallelize}" }

                    // Build execution plan from response
                    val plan = buildExecutionPlan(response, context)
                    Result.success(plan)
                },
                onFailure = { error ->
                    logger.w { "Planning failed, using default plan: ${error.message}" }
                    // Return default plan but log the issue
                    Result.success(ExecutionPlan.default())
                }
            )
        } catch (e: Exception) {
            logger.e(e) { "Error analyzing and planning" }
            Result.failure(e)
        }
    }

    override suspend fun getRelevantTools(requestType: RequestType): List<ToolDefinitionDto> {
        val allTools = toolExecutor.getToolsForApi()

        val requiredServers = requestClassifier.getRequiredServers(requestType)
        logger.d { "Required servers for $requestType: $requiredServers" }

        return if (requestType == RequestType.UNKNOWN) {
            // For unknown requests, return all tools
            allTools
        } else {
            // Filter tools by server
            allTools.filter { tool ->
                val toolName = tool.function.name
                val serverPrefix = extractServerPrefix(toolName)
                serverPrefix in requiredServers
            }
        }
    }

    override fun executeWithDependencies(
        toolCalls: List<ToolCallDto>,
        plan: ExecutionPlan
    ): Flow<ToolExecutionResult> = flow {
        logger.i { "Executing ${toolCalls.size} tools with dependencies" }

        if (plan.steps.isEmpty()) {
            logger.w { "Empty execution plan, executing tools in parallel" }
            val results = executeParallel(toolCalls)
            results.forEach { (toolCallId, result) ->
                val toolCall = toolCalls.find { it.id == toolCallId } ?: return@forEach
                emit(
                    ToolExecutionResult(
                        toolCallId = toolCallId,
                        toolName = toolCall.function.name,
                        serverName = extractServerPrefix(toolCall.function.name),
                        success = result.isSuccess,
                        result = result.getOrNull(),
                        error = result.exceptionOrNull()?.message,
                        executionTimeMs = 0
                    )
                )
            }
            return@flow
        }

        // CRITICAL FIX: Map API tool calls by name for quick lookup
        // We must use the ACTUAL tool calls from API response (with correct IDs),
        // not the plan's tool calls (which have different generated IDs)
        val apiToolCallsByName = toolCalls.associateBy { it.function.name }
        val executedToolCallIds = mutableSetOf<String>()

        // Get parallel batches from plan for ordering only
        val batches = plan.getParallelBatches()
        logger.d { "Execution split into ${batches.size} batches" }

        for ((batchIndex, batch) in batches.withIndex()) {
            // For each step in batch, find the matching API tool call by tool name
            val batchApiToolCalls = batch.mapNotNull { step ->
                apiToolCallsByName[step.toolName]?.also {
                    logger.d { "Matched plan step '${step.toolName}' to API tool call ${it.id}" }
                }
            }

            if (batchApiToolCalls.isEmpty()) {
                logger.w { "Batch ${batchIndex + 1}: No matching API tool calls found" }
                continue
            }

            // Log if any expected tools weren't found in the API response
            // This is normal - the planner may suggest tools that the main LLM doesn't call
            val missingTools = batch.map { it.toolName } - batchApiToolCalls.map { it.function.name }.toSet()
            if (missingTools.isNotEmpty()) {
                logger.d { "Planner suggested tools not used by LLM: $missingTools (this is normal)" }
            }

            logger.d { "Executing batch ${batchIndex + 1}/${batches.size} with ${batchApiToolCalls.size} tools" }

            // Execute batch in parallel using ACTUAL API tool calls (with correct IDs)
            val batchResults = executeBatch(batchApiToolCalls)

            // Emit results with CORRECT tool call IDs from API
            batchResults.forEach { (toolCallId, result) ->
                val apiToolCall = batchApiToolCalls.find { it.id == toolCallId } ?: return@forEach
                executedToolCallIds.add(toolCallId)
                emit(
                    ToolExecutionResult(
                        toolCallId = toolCallId,  // This is now the CORRECT ID from API
                        toolName = apiToolCall.function.name,
                        serverName = extractServerPrefix(apiToolCall.function.name),
                        success = result.isSuccess,
                        result = result.getOrNull(),
                        error = result.exceptionOrNull()?.message,
                        executionTimeMs = 0
                    )
                )
            }
        }

        // Also execute any API tool calls that weren't in the plan
        // This ensures we don't miss any tool calls from the API response
        val plannedToolNames = plan.steps.map { it.toolName }.toSet()
        val unplannedToolCalls = toolCalls.filter { it.function.name !in plannedToolNames }

        if (unplannedToolCalls.isNotEmpty()) {
            logger.d { "Executing ${unplannedToolCalls.size} unplanned tools in parallel" }
            val results = executeParallel(unplannedToolCalls)
            results.forEach { (toolCallId, result) ->
                val toolCall = unplannedToolCalls.find { it.id == toolCallId } ?: return@forEach
                executedToolCallIds.add(toolCallId)
                emit(
                    ToolExecutionResult(
                        toolCallId = toolCallId,
                        toolName = toolCall.function.name,
                        serverName = extractServerPrefix(toolCall.function.name),
                        success = result.isSuccess,
                        result = result.getOrNull(),
                        error = result.exceptionOrNull()?.message,
                        executionTimeMs = 0
                    )
                )
            }
        }

        // Log summary
        val missedToolCalls = toolCalls.filter { it.id !in executedToolCallIds }
        if (missedToolCalls.isNotEmpty()) {
            logger.w { "WARNING: ${missedToolCalls.size} API tool calls were not executed: ${missedToolCalls.map { it.function.name }}" }
        }
    }

    override suspend fun executeParallel(toolCalls: List<ToolCallDto>): Map<String, Result<String>> {
        logger.d { "Executing ${toolCalls.size} tools in parallel" }

        return coroutineScope {
            val deferredResults = toolCalls.map { toolCall ->
                async {
                    toolCall.id to executeSingle(toolCall)
                }
            }
            deferredResults.awaitAll().toMap()
        }
    }

    override fun routeToServer(toolCall: ToolCallDto): ServerRoutingDecision {
        val toolName = toolCall.function.name
        val serverPrefix = extractServerPrefix(toolName)
        val serverType = builtInServerPrefixes[serverPrefix] ?: ServerType.EXTERNAL

        val reason = when (serverType) {
            ServerType.BUILT_IN -> "Built-in server for ${serverPrefix} tools"
            ServerType.EXTERNAL -> "External server connection"
        }

        logger.d { "Routing $toolName to server: $serverPrefix ($serverType)" }

        return ServerRoutingDecision(
            serverName = serverPrefix,
            serverType = serverType,
            toolName = toolName,
            reason = reason
        )
    }

    override fun getServerStatus(): List<ServerInfo> {
        val builtInServers = mcpManager.getBuiltInServers()

        val builtInInfos = builtInServers.map { server ->
            ServerInfo(
                name = server.name,
                type = ServerType.BUILT_IN,
                toolCount = server.tools.size,
                isConnected = true
            )
        }

        // External servers info - tool count is 0 for synchronous access
        // Use suspend version separately if needed
        val externalConnections = mcpManager.getConnectedExternalServers()
        val externalInfos = externalConnections.map { serverName ->
            ServerInfo(
                name = serverName,
                type = ServerType.EXTERNAL,
                toolCount = 0, // Tool count requires suspend call
                isConnected = mcpManager.isExternalServerConnected(serverName)
            )
        }

        return builtInInfos + externalInfos
    }

    override fun isServerAvailable(serverName: String): Boolean {
        return when (serverName) {
            "filesystem", "terminal", "scheduler" -> true // Built-in servers always available
            else -> mcpManager.isExternalServerConnected(serverName)
        }
    }

    override suspend fun getAllTools(): List<ToolDefinitionDto> {
        return toolExecutor.getToolsForApi()
    }

    override suspend fun executeSingle(toolCall: ToolCallDto): Result<String> {
        logger.d { "Executing tool: ${toolCall.function.name}" }

        // Validate first
        val validation = validateToolCall(toolCall)
        if (validation.isFailure) {
            return validation.map { "" }
        }

        // Execute through tool executor
        return toolExecutor.executeToolCall(toolCall)
    }

    override fun validateToolCall(toolCall: ToolCallDto): Result<Unit> {
        val toolName = toolCall.function.name

        // Check tool name is not empty
        if (toolName.isBlank()) {
            return Result.failure(IllegalArgumentException("Tool name cannot be empty"))
        }

        // Check arguments are valid JSON
        try {
            if (toolCall.function.arguments.isNotBlank()) {
                json.parseToJsonElement(toolCall.function.arguments)
            }
        } catch (e: Exception) {
            return Result.failure(IllegalArgumentException("Invalid arguments JSON: ${e.message}"))
        }

        // Check server is available
        val serverName = extractServerPrefix(toolName)
        if (!isServerAvailable(serverName)) {
            return Result.failure(IllegalStateException("Server '$serverName' is not available"))
        }

        return Result.success(Unit)
    }

    // === Private Helpers ===

    /**
     * Extract server prefix from tool name.
     *
     * Tool names follow convention: "server_tool" (e.g., "filesystem_read_file")
     *
     * @param toolName Full tool name
     * @return Server name prefix
     */
    private fun extractServerPrefix(toolName: String): String {
        val underscoreIndex = toolName.indexOf('_')
        return if (underscoreIndex > 0) {
            toolName.substring(0, underscoreIndex)
        } else {
            toolName
        }
    }

    /**
     * Build execution plan from planning response.
     *
     * @param response Planning service response
     * @param context Orchestration context
     * @return Complete execution plan
     */
    private fun buildExecutionPlan(
        response: PlanningResponse,
        context: OrchestrationContext
    ): ExecutionPlan {
        val steps = response.steps.mapIndexed { index, plannedStep ->
            val serverName = extractServerPrefix(plannedStep.tool)
            ExecutionStep(
                id = "step_${index}_${UUID.randomUUID()}",
                toolCall = ToolCallDto(
                    id = "call_${index}_${UUID.randomUUID()}",
                    type = "function",
                    function = FunctionCallDto(
                        name = plannedStep.tool,
                        arguments = json.encodeToString(
                            kotlinx.serialization.json.JsonObject(
                                plannedStep.arguments.mapValues { (_, value) ->
                                    kotlinx.serialization.json.JsonPrimitive(value.toString())
                                }
                            )
                        )
                    )
                ),
                serverName = serverName,
                toolName = plannedStep.tool,
                estimatedTimeMs = 1000L,
                priority = index
            )
        }

        val dependencies = mutableMapOf<String, List<String>>()
        response.steps.forEachIndexed { index, plannedStep ->
            val stepId = steps.getOrNull(index)?.id ?: return@forEachIndexed
            val depIds = plannedStep.dependsOn.mapNotNull { depIndex ->
                steps.getOrNull(depIndex)?.id
            }
            dependencies[stepId] = depIds
        }

        return ExecutionPlan(
            id = "plan_${UUID.randomUUID()}",
            steps = steps,
            dependencies = dependencies,
            canParallelize = response.canParallelize,
            serversUsed = steps.map { it.serverName }.toSet()
        )
    }

    /**
     * Execute a batch of tool calls in parallel.
     *
     * @param toolCalls Tool calls to execute
     * @return Map of tool call ID to result
     */
    private suspend fun executeBatch(toolCalls: List<ToolCallDto>): Map<String, Result<String>> {
        return executeParallel(toolCalls)
    }
}
