package ru.agent.features.chat.di

import org.koin.dsl.module
import ru.agent.features.chat.domain.tools.ToolExecutor
import ru.agent.features.chat.tools.ToolExecutorImpl
import ru.agent.mcp.McpManager

/**
 * JVM-specific module for chat feature.
 *
 * Provides platform-specific implementations like ToolExecutor.
 */
val featureChatJvmModule = module {
    // ToolExecutor implementation using McpManager
    single<ToolExecutor> {
        ToolExecutorImpl(
            mcpManager = get<McpManager>()
        )
    }
}
