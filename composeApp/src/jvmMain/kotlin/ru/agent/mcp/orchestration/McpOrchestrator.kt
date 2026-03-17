package ru.agent.mcp.orchestration

import kotlinx.coroutines.flow.Flow
import ru.agent.features.chat.data.remote.dto.ToolCallDto
import ru.agent.features.chat.data.remote.dto.ToolDefinitionDto
import ru.agent.mcp.ServerType

/**
 * Orchestrator for managing MCP tool execution across multiple servers.
 *
 * Responsibilities:
 * - Analyzing user requests and creating execution plans
 * - Routing tool calls to appropriate MCP servers
 * - Managing parallel and sequential execution
 * - Handling dependencies between tool calls
 *
 * Usage:
 * ```kotlin
 * val orchestrator: McpOrchestrator by inject()
 *
 * // Analyze request and create plan
 * val plan = orchestrator.analyzeAndPlan(message, context)
 *
 * // Execute tools with dependencies
 * orchestrator.executeWithDependencies(toolCalls, plan)
 *     .collect { result -> println(result) }
 * ```
 */
interface McpOrchestrator {

    /**
     * Analyze user message and create execution plan.
     *
     * @param message User message to analyze
     * @param context Orchestration context with session info
     * @return Result containing execution plan or error
     */
    suspend fun analyzeAndPlan(
        message: String,
        context: OrchestrationContext
    ): Result<ExecutionPlan>

    /**
     * Get tools relevant for a request type.
     *
     * Filters available tools to only those that match the request type.
     * For example, FILE_OPERATION returns only filesystem tools.
     *
     * @param requestType Type of request
     * @return List of relevant tool definitions
     */
    suspend fun getRelevantTools(requestType: RequestType): List<ToolDefinitionDto>

    /**
     * Execute tool calls respecting dependencies.
     *
     * Uses the execution plan to determine:
     * - Which tools can run in parallel
     * - Which tools must wait for others
     * - Error handling and recovery
     *
     * @param toolCalls List of tool calls to execute
     * @param plan Execution plan with dependencies
     * @return Flow of execution results as they complete
     */
    fun executeWithDependencies(
        toolCalls: List<ToolCallDto>,
        plan: ExecutionPlan
    ): Flow<ToolExecutionResult>

    /**
     * Execute multiple tool calls in parallel.
     *
     * All tools are executed simultaneously without waiting for each other.
     * Use when tools have no dependencies between them.
     *
     * @param toolCalls List of tool calls to execute
     * @return Map of tool call ID to execution result
     */
    suspend fun executeParallel(
        toolCalls: List<ToolCallDto>
    ): Map<String, Result<String>>

    /**
     * Determine which server should handle a tool call.
     *
     * Routes tool calls based on naming convention:
     * - "filesystem_xxx" -> filesystem server
     * - "terminal_xxx" -> terminal server
     * - "scheduler_xxx" -> scheduler server
     * - External server name -> external server
     *
     * @param toolCall Tool call to route
     * @return Routing decision with server info
     */
    fun routeToServer(toolCall: ToolCallDto): ServerRoutingDecision

    /**
     * Get current status of all MCP servers.
     *
     * @return List of server information
     */
    fun getServerStatus(): List<ServerInfo>

    /**
     * Check if a server is available.
     *
     * @param serverName Name of the server to check
     * @return true if server is connected and available
     */
    fun isServerAvailable(serverName: String): Boolean

    /**
     * Get all available tools from all servers.
     *
     * @return List of all tool definitions
     */
    suspend fun getAllTools(): List<ToolDefinitionDto>

    /**
     * Execute a single tool call.
     *
     * @param toolCall Tool call to execute
     * @return Result of execution
     */
    suspend fun executeSingle(toolCall: ToolCallDto): Result<String>

    /**
     * Validate tool call arguments.
     *
     * @param toolCall Tool call to validate
     * @return Result with validation status
     */
    fun validateToolCall(toolCall: ToolCallDto): Result<Unit>
}
