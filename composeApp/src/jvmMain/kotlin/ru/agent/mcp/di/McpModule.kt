package ru.agent.mcp.di

import io.ktor.client.HttpClient
import io.ktor.client.plugins.sse.SSE
import org.koin.dsl.module
import ru.agent.mcp.McpManager
import ru.agent.mcp.client.McpClient
import ru.agent.mcp.server.FilesystemMcpServer
import ru.agent.mcp.server.SchedulerMcpServer
import ru.agent.mcp.server.TerminalMcpServer
import ru.agent.features.scheduler.domain.repository.ScheduledTaskRepository
import ru.agent.features.scheduler.domain.repository.TaskExecutionRepository
import ru.agent.scheduler.CronParser
import ru.agent.scheduler.SchedulerEngine
import ru.agent.scheduler.TaskExecutor
import ru.agent.scheduler.createSchedulerEngine
import java.io.File

/**
 * Find project root directory by looking for settings.gradle.kts.
 * settings.gradle.kts only exists at project root, not in submodules.
 * Goes up from current directory until finds the project root.
 */
private fun findProjectRoot(): String {
    var currentDir = File(System.getProperty("user.dir")).absoluteFile

    // Go up maximum 10 levels to find project root
    // settings.gradle.kts only exists at root, not in submodules
    var attempts = 0
    while (attempts < 10 && currentDir.parentFile != null) {
        val settingsGradle = File(currentDir, "settings.gradle.kts")

        if (settingsGradle.exists()) {
            return currentDir.absolutePath
        }

        currentDir = currentDir.parentFile!!
        attempts++
    }

    // Fallback to current directory if project root not found
    return System.getProperty("user.dir")
}

/**
 * Koin DI module for MCP infrastructure.
 *
 * Provides:
 * - HTTP client with SSE support
 * - Filesystem MCP Server
 * - Terminal MCP Server
 * - MCP Client for external servers
 * - MCP Manager (unified interface)
 */
val mcpModule = module {
    // HTTP Client with SSE support for MCP
    single<HttpClient> {
        HttpClient {
            install(SSE)
        }
    }

    // Filesystem MCP Server
    // Find project root - go up from current dir until we find build.gradle.kts or settings.gradle.kts
    single<FilesystemMcpServer> {
        FilesystemMcpServer(
            allowedRoots = listOf(findProjectRoot())
        )
    }

    // Terminal MCP Server
    single<TerminalMcpServer> {
        TerminalMcpServer(
            projectRoot = findProjectRoot()
        )
    }

    // Scheduler MCP Server
    single<SchedulerMcpServer> {
        SchedulerMcpServer(
            taskRepository = get(),
            executionRepository = get()
        )
    }

    // Scheduler Engine Components
    single<CronParser> { CronParser() }

    single<TaskExecutor> {
        TaskExecutor(
            executionRepository = get(),
            mcpManager = null // Will be set later via setter if needed
        )
    }

    single<SchedulerEngine> {
        createSchedulerEngine(
            taskRepository = get(),
            executionRepository = get(),
            mcpManager = get()
        )
    }

    // MCP Client for external servers
    single<McpClient> {
        McpClient(httpClient = get())
    }

    // MCP Manager (unified interface for servers + client)
    single<McpManager> {
        McpManager(
            filesystemServer = get(),
            terminalServer = get(),
            schedulerServer = get(),
            mcpClient = get()
        )
    }
}
