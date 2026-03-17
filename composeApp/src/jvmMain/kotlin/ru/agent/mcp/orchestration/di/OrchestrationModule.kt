package ru.agent.mcp.orchestration.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.agent.mcp.McpManager
import ru.agent.mcp.orchestration.McpOrchestrator
import ru.agent.mcp.orchestration.McpOrchestratorImpl
import ru.agent.mcp.orchestration.PlanningService
import ru.agent.mcp.orchestration.RequestClassifier
import ru.agent.features.chat.domain.tools.ToolExecutor

/**
 * Koin DI module for Orchestration layer.
 *
 * Provides:
 * - RequestClassifier as singleton (stateless classifier)
 * - PlanningService as singleton (LLM-driven planning)
 * - McpOrchestrator as singleton (coordinates MCP tool execution)
 */
val orchestrationModule = module {
    // RequestClassifier - singleton (stateless)
    single<RequestClassifier> { RequestClassifier() }

    // PlanningService - singleton (LLM-driven planning)
    single<PlanningService> {
        PlanningService(
            httpClient = get(),
            apiKey = get(named("deepseek_api_key"))
        )
    }

    // McpOrchestrator - singleton (coordinates MCP tool execution)
    single<McpOrchestrator> {
        McpOrchestratorImpl(
            mcpManager = get<McpManager>(),
            toolExecutor = get<ToolExecutor>(),
            requestClassifier = get(),
            planningService = get()
        )
    }
}
