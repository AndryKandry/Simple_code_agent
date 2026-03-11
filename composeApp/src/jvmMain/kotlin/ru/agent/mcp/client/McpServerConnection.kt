package ru.agent.mcp.client

import kotlinx.serialization.Serializable

/**
 * Configuration for connecting to an MCP server.
 */
@Serializable
data class McpServerConfig(
    val name: String,
    val url: String,
    val description: String = "",
    val enabled: Boolean = true,
    val timeout: Long = 30000,
    val transport: McpTransportType = McpTransportType.HTTP
)

/**
 * Transport type for MCP connection.
 */
@Serializable
enum class McpTransportType {
    HTTP,
    STDIO,
    WEBSOCKET,
    SSE
}

/**
 * Status of MCP server connection.
 */
enum class McpConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

/**
 * Information about an MCP server connection.
 */
data class McpServerConnection(
    val config: McpServerConfig,
    val status: McpConnectionStatus,
    val tools: List<McpToolInfo> = emptyList(),
    val resources: List<McpResourceInfo> = emptyList(),
    val error: String? = null,
    val connectedAt: Long? = null
) {
    val isConnected: Boolean
        get() = status == McpConnectionStatus.CONNECTED

    val uptime: Long?
        get() = connectedAt?.let { System.currentTimeMillis() - it }
}

/**
 * Information about an MCP tool.
 */
data class McpToolInfo(
    val name: String,
    val description: String,
    val serverName: String,
    val inputSchema: String? = null
) {
    val fullName: String
        get() = "$serverName:$name"
}

/**
 * Information about an MCP resource.
 */
data class McpResourceInfo(
    val uri: String,
    val name: String,
    val serverName: String,
    val description: String? = null,
    val mimeType: String? = null
)

/**
 * Result of tool execution.
 */
data class McpExecuteResult(
    val success: Boolean,
    val content: String,
    val error: String? = null,
    val serverName: String,
    val toolName: String,
    val executionTimeMs: Long = 0
)

/**
 * Predefined configurations for common MCP servers.
 *
 * Note: External MCP servers can be connected dynamically via /mcp connect <name> <url>
 */
object McpServerPresets {
    val PRESETS = emptyList<McpServerConfig>()
}
