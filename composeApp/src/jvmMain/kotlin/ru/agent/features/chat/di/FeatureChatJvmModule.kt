package ru.agent.features.chat.di

import org.koin.dsl.module
import ru.agent.features.chat.data.repository.ChatRepositoryImpl
import ru.agent.features.chat.domain.repository.ChatRepository
import ru.agent.features.chat.domain.tools.ToolExecutor
import ru.agent.features.chat.tools.ToolExecutorImpl
import ru.agent.mcp.McpManager
import ru.agent.mcp.orchestration.McpOrchestrator

/**
 * JVM-specific module for chat feature.
 *
 * Provides platform-specific implementations:
 * - ToolExecutor using McpManager
 * - ChatRepositoryImpl with McpOrchestrator for parallel tool execution
 */
val featureChatJvmModule = module {
    // ToolExecutor implementation using McpManager
    single<ToolExecutor> {
        ToolExecutorImpl(
            mcpManager = get<McpManager>()
        )
    }

    // ChatRepository implementation with McpOrchestrator for parallel tool execution
    single<ChatRepository> {
        ChatRepositoryImpl(
            deepSeekApiClient = get(),
            networkErrorHandling = get(),
            messageDao = get(),
            chatSessionDao = get(),
            contextOptimizer = get(),
            validateInvariantViolationUseCase = get(),
            getMemoryContextUseCase = get(),
            validationService = get(),
            toolExecutor = get(),
            orchestrator = get<McpOrchestrator>()
        )
    }
}
