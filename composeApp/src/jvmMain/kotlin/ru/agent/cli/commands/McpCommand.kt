package ru.agent.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.mordant.rendering.TextColors.brightBlue
import com.github.ajalt.mordant.rendering.TextColors.brightGreen
import com.github.ajalt.mordant.rendering.TextColors.brightRed
import com.github.ajalt.mordant.rendering.TextColors.brightYellow
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.longOrNull
import ru.agent.mcp.McpManager

/**
 * MCP command group for managing MCP servers.
 *
 * Usage:
 * ```
 * agent mcp list              List available MCP servers
 * agent mcp status            Show MCP status
 * agent mcp tools             List all available tools
 * agent mcp connect <server>  Connect to external server
 * agent mcp disconnect <server> Disconnect from server
 * agent mcp exec <server> <tool> <args>  Execute tool
 * ```
 */
class McpCommand : CliktCommand(
    name = "mcp",
    help = """
        Manage MCP (Model Context Protocol) servers

        MCP provides a standardized way to connect AI agents to external tools and resources.

        Built-in servers:
          - filesystem: File operations (read, write, list, search)
          - terminal: Command execution (safe commands only)

        External servers can be connected via HTTP/SSE transport.
    """.trimIndent()
) {
    init {
        subcommands(
            McpListCommand(),
            McpStatusCommand(),
            McpToolsCommand(),
            McpConnectCommand(),
            McpDisconnectCommand(),
            McpExecCommand(),
            McpReadCommand(),
            McpWriteCommand(),
            McpRunCommand()
        )
    }

    override fun run() {
        if (currentContext.invokedSubcommand == null) {
            echo("Please specify a subcommand: list, status, tools, connect, disconnect, exec, read, write, run")
        }
    }
}

/**
 * List available MCP servers.
 */
class McpListCommand : CliktCommand(
    name = "list",
    help = "List available MCP servers"
) {
    private val terminal = Terminal()

    override fun run() {
        terminal.println(cyan("\nMCP Servers"))
        terminal.println("=".repeat(50))

        // Built-in servers
        terminal.println(brightBlue("\nBuilt-in Servers:"))
        terminal.println("  ${brightGreen("filesystem")}  - File operations (read, write, list, search)")
        terminal.println("  ${brightGreen("terminal")}    - Command execution (safe commands only)")

        terminal.println(gray("\nUse 'mcp connect <name> <url>' to connect to external MCP servers."))
    }
}

/**
 * Show MCP infrastructure status.
 */
class McpStatusCommand : CliktCommand(
    name = "status",
    help = "Show MCP infrastructure status"
) {
    private val terminal = Terminal()

    override fun run() {
        val manager: McpManager by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }

        terminal.println(cyan("\nMCP Infrastructure Status"))
        terminal.println("=".repeat(50))

        // Built-in servers
        terminal.println(brightBlue("\nBuilt-in Servers:"))
        manager.getBuiltInServers().forEach { server ->
            terminal.println("  ${brightGreen(server.name)}: ${server.tools.size} tools")
            server.tools.take(5).forEach { tool ->
                terminal.println(gray("    - $tool"))
            }
            if (server.tools.size > 5) {
                terminal.println(gray("    ... and ${server.tools.size - 5} more"))
            }
        }

        // External connections
        terminal.println(brightBlue("\nExternal Connections:"))
        val connected = manager.getConnectedExternalServers()
        if (connected.isEmpty()) {
            terminal.println(gray("  (no connections)"))
        } else {
            connected.forEach { server ->
                val status = if (manager.isExternalServerConnected(server)) {
                    brightGreen("connected")
                } else {
                    brightRed("disconnected")
                }
                terminal.println("  ${brightYellow(server)}: $status")
            }
        }

        // Summary
        val totalTools = manager.getAllAvailableTools().size
        terminal.println(brightBlue("\nTotal Tools Available: $totalTools"))
    }
}

/**
 * List all available MCP tools.
 */
class McpToolsCommand : CliktCommand(
    name = "tools",
    help = "List all available MCP tools"
) {
    private val server by option("-s", "--server", help = "Filter by server name")
    private val verbose by option("-v", "--verbose", help = "Show full details").flag(default = false)

    private val terminal = Terminal()

    override fun run() {
        val manager: McpManager by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }

        val tools = manager.getAllAvailableTools()
            .filter { server == null || it.serverName == server }

        terminal.println(cyan("\nAvailable MCP Tools (${tools.size})"))
        terminal.println("=".repeat(50))

        tools.groupBy { it.serverName }.forEach { (serverName, serverTools) ->
            terminal.println(brightBlue("\n$serverName:"))
            serverTools.forEach { tool ->
                if (verbose) {
                    terminal.println("  ${brightGreen(tool.name)}")
                    terminal.println(gray("    ${tool.description}"))
                } else {
                    terminal.println("  ${brightGreen(tool.fullName)}")
                }
            }
        }
    }
}

/**
 * Connect to an external MCP server.
 */
class McpConnectCommand : CliktCommand(
    name = "connect",
    help = "Connect to an external MCP server"
) {
    private val serverName by argument(help = "Server name (custom identifier)")
    private val url by option("-u", "--url", help = "Server URL (required)")
    private val timeout by option("-t", "--timeout", help = "Connection timeout in ms").long().default(30000)

    private val terminal = Terminal()

    override fun run() {
        val manager: McpManager by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }

        if (url == null) {
            terminal.println(brightRed("Error: URL required. Use --url option."))
            terminal.println(gray("Example: mcp connect myserver --url http://localhost:8080/mcp"))
            return
        }

        val serverUrl = url!!
        terminal.println(cyan("Connecting to $serverName at $serverUrl..."))

        runBlocking {
            try {
                withTimeout(timeout) {
                    val result = manager.connectToServer(serverName, serverUrl, timeout)

                    result.fold(
                        onSuccess = {
                            terminal.println(brightGreen("Successfully connected to $serverName"))

                            // Discover tools
                            val tools = manager.getConnectedExternalServers()
                            terminal.println("Discovered ${tools.size} connection(s)")
                        },
                        onFailure = { error ->
                            terminal.println(brightRed("Failed to connect: ${error.message}"))
                        }
                    )
                }
            } catch (e: Exception) {
                terminal.println(brightRed("Connection timeout: ${e.message}"))
            }
        }
    }
}

/**
 * Disconnect from an external MCP server.
 */
class McpDisconnectCommand : CliktCommand(
    name = "disconnect",
    help = "Disconnect from an external MCP server"
) {
    private val serverName by argument(help = "Server name to disconnect from")
    private val all by option("-a", "--all", help = "Disconnect from all servers").flag(default = false)

    private val terminal = Terminal()

    override fun run() {
        val manager: McpManager by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }

        runBlocking {
            if (all) {
                manager.disconnectAll()
                terminal.println(brightGreen("Disconnected from all servers."))
            } else {
                val result = manager.disconnectFromServer(serverName)
                result.fold(
                    onSuccess = {
                        terminal.println(brightGreen("Disconnected from $serverName."))
                    },
                    onFailure = { error ->
                        terminal.println(brightRed("Failed to disconnect: ${error.message}"))
                    }
                )
            }
        }
    }
}

/**
 * Execute an MCP tool.
 */
class McpExecCommand : CliktCommand(
    name = "exec",
    help = "Execute an MCP tool"
) {
    private val fullName by argument(help = "Full tool name (server:tool)")
    private val argsJson by option("-a", "--args", help = "Arguments as JSON")

    private val terminal = Terminal()

    override fun run() {
        val manager: McpManager by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }

        // Parse arguments
        val arguments = if (argsJson != null) {
            try {
                kotlinx.serialization.json.Json.decodeFromString<
                    kotlinx.serialization.json.JsonObject>(argsJson!!)
                    .mapValues { (_, value) ->
                        when (value) {
                            is kotlinx.serialization.json.JsonPrimitive -> {
                                value.booleanOrNull ?: value.longOrNull ?: value.content
                            }
                            else -> value.toString()
                        }
                    }
            } catch (e: Exception) {
                terminal.println(brightRed("Invalid JSON arguments: ${e.message}"))
                return
            }
        } else {
            emptyMap()
        }

        terminal.println(cyan("Executing $fullName..."))

        runBlocking {
            val result = manager.executeToolByFullName(fullName, arguments)

            result.fold(
                onSuccess = { output ->
                    terminal.println(brightGreen("Result:"))
                    terminal.println(output)
                },
                onFailure = { error ->
                    terminal.println(brightRed("Error: ${error.message}"))
                }
            )
        }
    }
}

/**
 * Quick file read command using filesystem MCP.
 */
class McpReadCommand : CliktCommand(
    name = "read",
    help = "Read a file using filesystem MCP"
) {
    private val path by argument(help = "File path to read")

    private val terminal = Terminal()

    override fun run() {
        val manager: McpManager by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }

        val result = manager.readFile(path)

        result.fold(
            onSuccess = { content ->
                terminal.println(content)
            },
            onFailure = { error ->
                terminal.println(brightRed("Error: ${error.message}"))
            }
        )
    }
}

/**
 * Quick file write command using filesystem MCP.
 */
class McpWriteCommand : CliktCommand(
    name = "write",
    help = "Write content to a file using filesystem MCP"
) {
    private val path by argument(help = "File path to write")
    private val content by argument(help = "Content to write")

    private val terminal = Terminal()

    override fun run() {
        val manager: McpManager by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }

        val result = manager.writeFile(path, content)

        result.fold(
            onSuccess = { message ->
                terminal.println(brightGreen(message))
            },
            onFailure = { error ->
                terminal.println(brightRed("Error: ${error.message}"))
            }
        )
    }
}

/**
 * Quick command execution using terminal MCP.
 */
class McpRunCommand : CliktCommand(
    name = "run",
    help = "Execute a terminal command using terminal MCP"
) {
    private val command by argument(help = "Command to execute")
    private val timeout by option("-t", "--timeout", help = "Timeout in ms").long().default(30000)

    private val terminal = Terminal()

    override fun run() {
        val manager: McpManager by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }

        // Check if command is allowed
        if (!manager.isCommandAllowed(command)) {
            terminal.println(brightRed("Error: Command not allowed by security policy."))
            terminal.println(gray("Use 'agent mcp exec terminal:list_allowed_commands' to see allowed commands."))
            return
        }

        terminal.println(cyan("Executing: $command"))
        terminal.println("-".repeat(50))

        val result = manager.executeCommand(command, timeout)

        result.fold(
            onSuccess = { output ->
                terminal.println(output)
            },
            onFailure = { error ->
                terminal.println(brightRed("Error: ${error.message}"))
            }
        )
    }
}
