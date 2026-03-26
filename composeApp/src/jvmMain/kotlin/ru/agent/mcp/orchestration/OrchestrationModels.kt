package ru.agent.mcp.orchestration

import ru.agent.features.chat.data.remote.dto.ToolCallDto
import ru.agent.features.chat.data.remote.dto.ToolDefinitionDto
import ru.agent.mcp.ServerType

/**
 * Type of request for routing to appropriate MCP servers.
 */
enum class RequestType {
    /** Uses filesystem server - file read/write/list/search operations */
    FILE_OPERATION,

    /** Uses terminal server - shell/bash command execution */
    TERMINAL_COMMAND,

    /** Uses scheduler server - reminders and scheduled tasks */
    SCHEDULING,

    /** Uses filesystem + terminal servers - code analysis, builds */
    CODE_ANALYSIS,

    /** Uses terminal server - git operations */
    GIT_OPERATION,

    /** Uses multiple servers - complex workflow spanning filesystem, terminal, scheduler */
    MULTI_TYPE,

    /** RAG queries - questions about codebase, architecture, documentation - uses semantic code search */
    RAG_REQUEST,

    /** Unable to classify - may need all tools */
    UNKNOWN
}

/**
 * Information about an MCP server.
 */
data class ServerInfo(
    val name: String,
    val type: ServerType,
    val toolCount: Int,
    val isConnected: Boolean
)

/**
 * Context for orchestration decisions.
 *
 * @property sessionId Current chat session ID
 * @property workingDirectory Current working directory for file operations
 * @property recentToolCalls Recent tool calls for context-aware decisions
 * @property availableServers List of available MCP servers
 */
data class OrchestrationContext(
    val sessionId: String,
    val workingDirectory: String,
    val recentToolCalls: List<ToolCallDto> = emptyList(),
    val availableServers: List<ServerInfo> = listOf(
        ServerInfo("filesystem", ServerType.BUILT_IN, 8, true),
        ServerInfo("terminal", ServerType.BUILT_IN, 5, true),
        ServerInfo("scheduler", ServerType.BUILT_IN, 10, true)
    )
)

/**
 * Execution plan for a multi-step tool workflow.
 *
 * @property id Unique identifier for this plan
 * @property steps Ordered list of execution steps
 * @property dependencies Map of step ID to list of step IDs it depends on
 * @property canParallelize Whether any steps can run in parallel
 * @property serversUsed Set of server names used in this plan
 */
data class ExecutionPlan(
    val id: String,
    val steps: List<ExecutionStep>,
    val dependencies: Map<String, List<String>>,
    val canParallelize: Boolean,
    val serversUsed: Set<String>
) {
    companion object {
        /**
         * Create a default empty execution plan.
         */
        fun default() = ExecutionPlan(
            id = "default",
            steps = emptyList(),
            dependencies = emptyMap(),
            canParallelize = false,
            serversUsed = emptySet()
        )
    }

    /**
     * Get steps grouped by execution batch.
     * Steps in the same batch have no dependencies on each other
     * and can be executed in parallel.
     *
     * @return List of batches, where each batch contains steps that can run in parallel
     */
    fun getParallelBatches(): List<List<ExecutionStep>> {
        if (steps.isEmpty()) return emptyList()

        val batches = mutableListOf<List<ExecutionStep>>()
        val executed = mutableSetOf<String>()
        val remaining = steps.toMutableList()

        while (remaining.isNotEmpty()) {
            // Find all steps whose dependencies are satisfied
            val batch = remaining.filter { step ->
                val deps = dependencies[step.id] ?: emptyList()
                deps.all { it in executed }
            }

            if (batch.isEmpty()) {
                // Circular dependency detected - execute remaining sequentially
                batches.add(remaining.toList())
                break
            }

            batches.add(batch)
            executed.addAll(batch.map { it.id })
            remaining.removeAll(batch)
        }

        return batches
    }

    /**
     * Get total estimated execution time.
     */
    fun getEstimatedTotalTimeMs(): Long {
        val batches = getParallelBatches()
        return batches.sumOf { batch ->
            // For parallel batch, use max time; for sequential, sum all
            if (batch.size == 1) batch.first().estimatedTimeMs
            else batch.maxOf { it.estimatedTimeMs }
        }
    }
}

/**
 * Single step in an execution plan.
 *
 * @property id Unique identifier for this step
 * @property toolCall The tool call to execute
 * @property serverName Target MCP server (filesystem, terminal, scheduler)
 * @property toolName Full tool name like "filesystem:read_file"
 * @property estimatedTimeMs Estimated execution time in milliseconds
 * @property priority Priority for ordering (higher = more important)
 */
data class ExecutionStep(
    val id: String,
    val toolCall: ToolCallDto,
    val serverName: String,
    val toolName: String,
    val estimatedTimeMs: Long = 1000,
    val priority: Int = 0
)

/**
 * Result of a tool execution.
 *
 * @property toolCallId ID of the tool call
 * @property toolName Name of the tool that was executed
 * @property serverName Which MCP server executed the tool
 * @property success Whether execution was successful
 * @property result Tool output if successful
 * @property error Error message if failed
 * @property executionTimeMs Actual execution time in milliseconds
 */
data class ToolExecutionResult(
    val toolCallId: String,
    val toolName: String,
    val serverName: String,
    val success: Boolean,
    val result: String?,
    val error: String?,
    val executionTimeMs: Long
)

/**
 * Decision about routing a tool call to a server.
 *
 * @property serverName Target MCP server name
 * @property serverType Type of the server (built-in or external)
 * @property toolName Full tool name
 * @property reason Explanation for the routing decision
 */
data class ServerRoutingDecision(
    val serverName: String,
    val serverType: ServerType,
    val toolName: String,
    val reason: String
)

/**
 * Planning request for LLM-driven planning.
 *
 * @property userMessage Original user message
 * @property context Orchestration context
 * @property availableTools Tools available for planning
 */
data class PlanningRequest(
    val userMessage: String,
    val context: OrchestrationContext,
    val availableTools: List<ToolDefinitionDto>
)

/**
 * Parsed response from planning service.
 *
 * @property steps Planned execution steps
 * @property canParallelize Whether steps can run in parallel
 * @property reasoning Explanation of the plan
 */
data class PlanningResponse(
    val steps: List<PlannedStep>,
    val canParallelize: Boolean = false,
    val reasoning: String = ""
)

/**
 * A single planned step from the planning service.
 *
 * @property tool Full tool name (e.g., "filesystem_read_file")
 * @property arguments Tool arguments as map
 * @property dependsOn List of step indices this depends on
 */
data class PlannedStep(
    val tool: String,
    val arguments: Map<String, Any?>,
    val dependsOn: List<Int> = emptyList()
)
