package ru.agent.mcp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.agent.mcp.client.McpClient
import ru.agent.mcp.client.McpConnectionStatus
import ru.agent.mcp.client.McpExecuteResult
import ru.agent.mcp.client.McpServerConfig
import ru.agent.mcp.client.McpServerConnection
import ru.agent.mcp.client.McpToolInfo
import ru.agent.mcp.server.FilesystemMcpServer
import ru.agent.mcp.server.TerminalMcpServer

/**
 * Central manager for MCP infrastructure.
 *
 * Manages:
 * - Built-in MCP servers (Filesystem, Terminal)
 * - External MCP server connections via client
 * - Tool discovery and execution
 *
 * Usage:
 * ```kotlin
 * // Get manager from DI
 * val manager: McpManager by inject()
 *
 * // Use built-in servers directly
 * val result = manager.readFile("/path/to/file.txt")
 *
 * // Connect to external server
 * manager.connectToServer("github", "http://localhost:3000/mcp")
 *
 * // Execute tool on external server
 * manager.executeExternalTool("github", "search_code", mapOf("query" to "test"))
 * ```
 */
class McpManager(
    private val filesystemServer: FilesystemMcpServer,
    private val terminalServer: TerminalMcpServer,
    private val mcpClient: McpClient
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    // Connection state
    private val _connections = MutableStateFlow<List<McpServerConnection>>(emptyList())
    val connections: StateFlow<List<McpServerConnection>> = _connections.asStateFlow()

    // === Built-in Server Methods ===

    /**
     * Get list of built-in MCP servers.
     */
    fun getBuiltInServers(): List<BuiltInServer> = listOf(
        BuiltInServer(
            name = "filesystem",
            description = "File system operations",
            tools = filesystemServer.getAvailableTools()
        ),
        BuiltInServer(
            name = "terminal",
            description = "Terminal command execution",
            tools = terminalServer.getAvailableTools()
        )
    )

    // === Filesystem Operations ===

    /**
     * Read file contents.
     */
    fun readFile(path: String): Result<String> {
        return filesystemServer.readFileSync(path)
    }

    /**
     * Write content to file.
     */
    fun writeFile(path: String, content: String): Result<String> {
        return filesystemServer.writeFileSync(path, content)
    }

    /**
     * List directory contents.
     */
    fun listDirectory(path: String): Result<String> {
        return filesystemServer.listDirectorySync(path)
    }

    /**
     * Search for files matching pattern.
     */
    fun searchFiles(path: String, pattern: String): Result<String> {
        return filesystemServer.searchFilesSync(path, pattern)
    }

    /**
     * Delete a file (requires confirmation).
     */
    fun deleteFile(path: String, confirm: Boolean = false): Result<String> {
        return filesystemServer.deleteFileSync(path, confirm)
    }

    /**
     * Create a directory.
     */
    fun createDirectory(path: String): Result<String> {
        return filesystemServer.createDirectorySync(path)
    }

    /**
     * Check if file or directory exists.
     */
    fun fileExists(path: String): Result<String> {
        return filesystemServer.fileExistsSync(path)
    }

    /**
     * Copy a file.
     */
    fun copyFile(source: String, destination: String): Result<String> {
        return filesystemServer.copyFileSync(source, destination)
    }

    // === Terminal Operations ===

    /**
     * Execute a terminal command.
     */
    fun executeCommand(
        command: String,
        timeout: Long = 30000,
        workingDir: String? = null
    ): Result<String> {
        return terminalServer.executeCommandSync(command, timeout, workingDir)
    }

    /**
     * Run a shell script.
     */
    fun runShellScript(
        scriptPath: String,
        args: List<String> = emptyList(),
        timeout: Long = 60000
    ): Result<String> {
        return terminalServer.runShellScriptSync(scriptPath, args, timeout)
    }

    /**
     * Check if a command is available on the system.
     */
    fun checkCommandAvailable(command: String): Result<String> {
        return terminalServer.checkCommandAvailableSync(command)
    }

    /**
     * Get list of allowed commands.
     */
    fun listAllowedCommands(): Result<String> {
        return terminalServer.listAllowedCommandsSync()
    }

    /**
     * Get environment information.
     */
    fun getEnvironmentInfo(): Result<String> {
        return terminalServer.getEnvironmentInfoSync()
    }

    /**
     * Check if a command is allowed.
     */
    fun isCommandAllowed(command: String): Boolean {
        return terminalServer.isCommandAllowed(command)
    }

    // === External Server Methods (via Client) ===

    /**
     * Connect to an external MCP server.
     */
    suspend fun connectToServer(
        name: String,
        url: String,
        timeout: Long = 30000
    ): Result<Unit> {
        val result = mcpClient.connect(name, url, timeout = timeout)
        updateConnectionState()
        return result
    }

    /**
     * Connect using predefined config.
     */
    suspend fun connectToServer(config: McpServerConfig): Result<Unit> {
        val result = mcpClient.connect(config.name, config)
        updateConnectionState()
        return result
    }

    /**
     * Disconnect from an external MCP server.
     */
    suspend fun disconnectFromServer(name: String): Result<Unit> {
        val result = mcpClient.disconnect(name)
        updateConnectionState()
        return result
    }

    /**
     * Disconnect from all external servers.
     */
    suspend fun disconnectAll() {
        mcpClient.disconnectAll()
        updateConnectionState()
    }

    /**
     * Execute a tool on an external server.
     */
    suspend fun executeExternalTool(
        serverName: String,
        toolName: String,
        arguments: Map<String, Any> = emptyMap()
    ): Result<McpExecuteResult> {
        return mcpClient.callTool(serverName, toolName, arguments)
    }

    /**
     * List tools on an external server.
     */
    suspend fun listExternalTools(serverName: String) = mcpClient.listTools(serverName)

    /**
     * Get list of connected external servers.
     */
    fun getConnectedExternalServers() = mcpClient.getConnectedServers()

    /**
     * Check if connected to external server.
     */
    fun isExternalServerConnected(serverName: String) = mcpClient.isConnected(serverName)

    // === Combined Methods ===

    /**
     * Get all available tools from built-in and external servers.
     */
    fun getAllAvailableTools(): List<ToolInfo> {
        val builtInTools = getBuiltInServers().flatMap { server ->
            server.tools.map { tool ->
                ToolInfo(
                    name = tool,
                    fullName = "${server.name}:$tool",
                    serverName = server.name,
                    serverType = ServerType.BUILT_IN,
                    description = "Built-in tool: $tool"
                )
            }
        }

        val externalTools = mcpClient.getAllDiscoveredTools().map { tool ->
            ToolInfo(
                name = tool.name,
                fullName = tool.fullName,
                serverName = tool.serverName,
                serverType = ServerType.EXTERNAL,
                description = tool.description
            )
        }

        return builtInTools + externalTools
    }

    /**
     * Execute a tool by its full name (server:tool).
     */
    suspend fun executeToolByFullName(
        fullName: String,
        arguments: Map<String, Any> = emptyMap()
    ): Result<String> {
        val parts = fullName.split(":", limit = 2)
        if (parts.size != 2) {
            return Result.failure(IllegalArgumentException("Invalid tool name format. Use 'server:tool'"))
        }

        val (serverName, toolName) = parts

        return when (serverName) {
            "filesystem" -> executeFilesystemTool(toolName, arguments)
            "terminal" -> executeTerminalTool(toolName, arguments)
            else -> {
                if (mcpClient.isConnected(serverName)) {
                    val result = mcpClient.callTool(serverName, toolName, arguments)
                    result.map { it.content }
                } else {
                    Result.failure(IllegalArgumentException("Server '$serverName' not connected"))
                }
            }
        }
    }

    private fun executeFilesystemTool(toolName: String, arguments: Map<String, Any>): Result<String> {
        return when (toolName) {
            "read_file" -> {
                val path = arguments["path"] as? String ?: return Result.failure(
                    IllegalArgumentException("Missing 'path' argument")
                )
                readFile(path)
            }
            "write_file" -> {
                val path = arguments["path"] as? String ?: return Result.failure(
                    IllegalArgumentException("Missing 'path' argument")
                )
                val content = arguments["content"] as? String ?: return Result.failure(
                    IllegalArgumentException("Missing 'content' argument")
                )
                writeFile(path, content)
            }
            "list_directory" -> {
                val path = arguments["path"] as? String ?: return Result.failure(
                    IllegalArgumentException("Missing 'path' argument")
                )
                listDirectory(path)
            }
            "search_files" -> {
                val path = arguments["path"] as? String ?: return Result.failure(
                    IllegalArgumentException("Missing 'path' argument")
                )
                val pattern = arguments["pattern"] as? String ?: "*"
                searchFiles(path, pattern)
            }
            "delete_file" -> {
                val path = arguments["path"] as? String ?: return Result.failure(
                    IllegalArgumentException("Missing 'path' argument")
                )
                val confirm = arguments["confirm"] as? Boolean ?: false
                deleteFile(path, confirm)
            }
            "create_directory" -> {
                val path = arguments["path"] as? String ?: return Result.failure(
                    IllegalArgumentException("Missing 'path' argument")
                )
                createDirectory(path)
            }
            "file_exists" -> {
                val path = arguments["path"] as? String ?: return Result.failure(
                    IllegalArgumentException("Missing 'path' argument")
                )
                fileExists(path)
            }
            "copy_file" -> {
                val source = arguments["source"] as? String ?: return Result.failure(
                    IllegalArgumentException("Missing 'source' argument")
                )
                val destination = arguments["destination"] as? String ?: return Result.failure(
                    IllegalArgumentException("Missing 'destination' argument")
                )
                copyFile(source, destination)
            }
            else -> Result.failure(IllegalArgumentException("Unknown filesystem tool: $toolName"))
        }
    }

    private fun executeTerminalTool(toolName: String, arguments: Map<String, Any>): Result<String> {
        return when (toolName) {
            "execute_command" -> {
                val command = arguments["command"] as? String ?: return Result.failure(
                    IllegalArgumentException("Missing 'command' argument")
                )
                val timeout = (arguments["timeout"] as? Number)?.toLong() ?: 30000L
                val workingDir = arguments["working_dir"] as? String
                executeCommand(command, timeout, workingDir)
            }
            "run_shell_script" -> {
                val scriptPath = arguments["script_path"] as? String ?: return Result.failure(
                    IllegalArgumentException("Missing 'script_path' argument")
                )
                val args = (arguments["args"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val timeout = (arguments["timeout"] as? Number)?.toLong() ?: 60000L
                runShellScript(scriptPath, args, timeout)
            }
            "check_command_available" -> {
                val command = arguments["command"] as? String ?: return Result.failure(
                    IllegalArgumentException("Missing 'command' argument")
                )
                checkCommandAvailable(command)
            }
            "list_allowed_commands" -> {
                listAllowedCommands()
            }
            "get_environment_info" -> {
                getEnvironmentInfo()
            }
            else -> Result.failure(IllegalArgumentException("Unknown terminal tool: $toolName"))
        }
    }

    // === Connection State ===

    private fun updateConnectionState() {
        scope.launch {
            _connections.value = mcpClient.getAllConnectionInfo()
        }
    }

    // === Status and Info ===

    /**
     * Get status summary of MCP infrastructure.
     */
    fun getStatusSummary(): String {
        return buildString {
            appendLine("MCP Infrastructure Status")
            appendLine("=" .repeat(50))

            appendLine("\nBuilt-in Servers:")
            getBuiltInServers().forEach { server ->
                appendLine("  - ${server.name}: ${server.tools.size} tools")
            }

            appendLine("\nExternal Connections:")
            val connected = mcpClient.getConnectedServers()
            if (connected.isEmpty()) {
                appendLine("  (none)")
            } else {
                connected.forEach { server ->
                    val info = mcpClient.getConnectionInfo(server)
                    appendLine("  - $server: ${info?.status ?: "unknown"}")
                }
            }

            appendLine("\nTotal Tools Available: ${getAllAvailableTools().size}")
        }
    }
}

// === Data Classes ===

/**
 * Information about a built-in MCP server.
 */
data class BuiltInServer(
    val name: String,
    val description: String,
    val tools: List<String>
)

/**
 * Type of MCP server.
 */
enum class ServerType {
    BUILT_IN,
    EXTERNAL
}

/**
 * Information about a tool.
 */
data class ToolInfo(
    val name: String,
    val fullName: String,
    val serverName: String,
    val serverType: ServerType,
    val description: String
)
