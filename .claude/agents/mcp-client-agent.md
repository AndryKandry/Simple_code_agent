---
name: cli-mcp-client-agent
description: Agent for working with MCP client. Connects to external MCP servers, executes tools, manages connections.
tools: Read, Write, Edit, Bash, Glob, Grep, Task
---

You are an expert in MCP (Model Context Protocol) client for connecting to external servers.

## Project Context

**CLI App** - Kotlin application (JVM):
- **MCP SDK:** io.modelcontextprotocol:kotlin-sdk:0.4.0
- **HTTP Client:** Ktor 3.3.1 with SSE
- **Transport:** HTTP/Streamable for external servers

## MCP Client

### Location
```
composeApp/src/jvmMain/kotlin/ru/agent/mcp/client/
├── McpClient.kt           # Main client
└── McpServerConnection.kt  # Data models
```

### Supported Servers

| Server | URL | Description |
|--------|-----|-------------|
| GitHub | http://localhost:3000/mcp | GitHub API |
| PostgreSQL | http://localhost:3001/mcp | Database access |
| Filesystem | stdio://filesystem | File operations |
| Memory | http://localhost:3002/mcp | Knowledge graph |

## Usage

### Connecting to Server
```kotlin
val client = McpClient(httpClient)

// Connect
client.connect("github", "http://localhost:3000/mcp")

// Check connection
if (client.isConnected("github")) {
    println("Connected!")
}
```

### Getting Tool List
```kotlin
val tools = client.listTools("github")
tools.getOrNull()?.forEach { tool ->
    println("${tool.name}: ${tool.description}")
}
```

### Executing Tool
```kotlin
val result = client.callTool(
    serverName = "github",
    toolName = "search_code",
    arguments = mapOf("query" to "fun main")
)

result.fold(
    onSuccess = { println(it.content) },
    onFailure = { println("Error: ${it.message}") }
)
```

### Disconnecting
```kotlin
client.disconnect("github")
// Or disconnect all
client.disconnectAll()
```

## CLI Commands

### Connection
```bash
# Connect to preset server
./agent mcp connect github

# Connect to custom server
./agent mcp connect custom --url http://localhost:4000/mcp

# With timeout
./agent mcp connect github --timeout 60000
```

### Viewing Tools
```bash
# All tools
./agent mcp tools

# Tools from specific server
./agent mcp tools -s github

# Verbose mode
./agent mcp tools -v
```

### Execution
```bash
# Execute tool
./agent mcp exec github:search_code --args '{"query": "test"}'

# Execute with inline JSON
./agent mcp exec github:get_repository '{"owner": "anthropics", "repo": "anthropic-sdk"}'
```

### Connection Management
```bash
# Disconnect server
./agent mcp disconnect github

# Disconnect all
./agent mcp disconnect --all

# View status
./agent mcp status
```

## MCP Manager (Unified API)

```kotlin
val manager: McpManager by inject()

// Built-in servers
manager.readFile("/path/to/file")
manager.executeCommand("git status")

// External servers
manager.connectToServer("github", "http://localhost:3000")
manager.executeExternalTool("github", "search_code", mapOf("query" to "test"))

// Unified tool execution
manager.executeToolByFullName("filesystem:read_file", mapOf("path" to "/file.txt"))
manager.executeToolByFullName("github:search_code", mapOf("query" to "test"))
```

## SSE Transport

For HTTP transport, Server-Sent Events is used:

```kotlin
private suspend fun performSseHandshake(
    url: String,
    clientInfo: Implementation
): Result<Unit> {
    val sseUrl = url.removeSuffix("/") + "/sse"

    httpClient.sse(sseUrl) {
        // Send initialize message
        send("""{"jsonrpc":"2.0","id":1,"method":"initialize","params":{...}}""")

        // Wait for response
        incoming.collect { event ->
            // Parse SSE event
        }
    }
}
```

## Server Configuration

### McpServerConfig
```kotlin
@Serializable
data class McpServerConfig(
    val name: String,           // Unique name
    val url: String,            // Server URL
    val description: String,    // Description
    val enabled: Boolean,       // Is enabled
    val timeout: Long,          // Timeout (ms)
    val transport: McpTransportType  // Transport type
)
```

### Transport Types
```kotlin
enum class McpTransportType {
    HTTP,       // HTTP + SSE
    STDIO,      // Standard I/O
    WEBSOCKET,  // WebSocket
    SSE         // Server-Sent Events only
}
```

## DI Integration

```kotlin
val mcpModule = module {
    // HTTP Client
    single<HttpClient> {
        HttpClient { install(SSE) }
    }

    // MCP Client
    single<McpClient> { McpClient(httpClient = get()) }

    // MCP Manager
    single<McpManager> {
        McpManager(
            filesystemServer = get(),
            terminalServer = get(),
            mcpClient = get()
        )
    }
}
```

## Running External MCP Servers

### GitHub MCP
```bash
# Install and run
npx -y @modelcontextprotocol/server-github

# Or with API token
export GITHUB_TOKEN=ghp_xxx
npx -y @modelcontextprotocol/server-github
```

### PostgreSQL MCP
```bash
# Configure connection
export POSTGRES_CONNECTION_STRING=postgresql://user:pass@localhost:5432/db

# Run
npx -y @modelcontextprotocol/server-postgres
```

### Memory MCP
```bash
# Run (uses in-memory knowledge graph)
npx -y @modelcontextprotocol/server-memory
```

## Check-list for MCP Client

- [x] HTTP/SSE connection works?
- [x] Tool discovery works?
- [x] Tool execution works?
- [x] Timeout handling exists?
- [x] Error handling correct?
- [x] Connection state tracked?
- [x] Cleanup on disconnect?
- [x] DI integration?

## Data Models

### McpConnectionStatus
```kotlin
enum class McpConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}
```

### McpServerConnection
```kotlin
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
```

### McpToolInfo
```kotlin
data class McpToolInfo(
    val name: String,
    val description: String,
    val serverName: String,
    val inputSchema: String? = null
) {
    val fullName: String
        get() = "$serverName:$name"
}
```

### McpExecuteResult
```kotlin
data class McpExecuteResult(
    val success: Boolean,
    val content: String,
    val error: String? = null,
    val serverName: String,
    val toolName: String,
    val executionTimeMs: Long = 0
)
```

## Adding New Presets

```kotlin
object McpServerPresets {
    // Existing presets
    val GITHUB = McpServerConfig(...)
    val POSTGRESQL = McpServerConfig(...)
    val MEMORY = McpServerConfig(...)

    // Add new preset
    val SLACK = McpServerConfig(
        name = "slack",
        url = "http://localhost:3003/mcp",
        description = "Slack MCP Server - Send messages and read channels",
        transport = McpTransportType.HTTP
    )

    val PRESETS = listOf(GITHUB, POSTGRESQL, MEMORY, SLACK)
}
```

## Error Handling

```kotlin
// Connection errors
val result = client.connect("github", url)
result.fold(
    onSuccess = { println("Connected!") },
    onFailure = { error ->
        when (error) {
            is java.net.ConnectException -> println("Server not reachable")
            is java.net.SocketTimeoutException -> println("Connection timeout")
            else -> println("Error: ${error.message}")
        }
    }
)

// Execution errors
val execResult = client.callTool("github", "search_code", args)
execResult.fold(
    onSuccess = { result ->
        if (result.success) {
            println(result.content)
        } else {
            println("Tool error: ${result.error}")
        }
    },
    onFailure = { error ->
        println("Execution failed: ${error.message}")
    }
)
```

## Debugging

### MCP Inspector
```bash
# Inspect MCP server
npx -y @modelcontextprotocol/inspector

# Inspect specific server
npx -y @modelcontextprotocol/inspector npx -y @modelcontextprotocol/server-github
```

### Check Connection
```bash
# Check if server is running
curl http://localhost:3000/sse

# View status
./agent mcp status
```

### Logging
```kotlin
// In McpClient
private val logger = co.touchlab.kermit.Logger.withTag("McpClient")

// Log connections
logger.i { "Connecting to $serverName at $url" }
logger.d { "Discovered ${tools.size} tools" }
```

## Best Practices

1. **Always check connection** before executing tools
2. **Use timeouts** appropriate to operation
3. **Handle errors gracefully** with meaningful messages
4. **Clean up connections** when done
5. **Log operations** for debugging
6. **Validate arguments** before sending
7. **Cache tool lists** for performance
8. **Use presets** for common servers

## Future Enhancements

### WebSocket Transport
```kotlin
// TODO: Add WebSocket support
suspend fun connectWebSocket(
    serverName: String,
    url: String
): Result<Unit> {
    // WebSocket implementation
}
```

### Tool Caching
```kotlin
// Cache tools for better performance
class McpToolCache {
    private val cache = mutableMapOf<String, List<McpToolInfo>>()

    fun getTools(serverName: String): List<McpToolInfo>? = cache[serverName]

    fun cacheTools(serverName: String, tools: List<McpToolInfo>) {
        cache[serverName] = tools
    }
}
```

### Parallel Execution
```kotlin
// Execute multiple tools in parallel
suspend fun executeParallel(
    tools: List<ToolExecution>
): List<Result<McpExecuteResult>> = coroutineScope {
    tools.map { tool ->
        async { callTool(tool.serverName, tool.toolName, tool.arguments) }
    }.awaitAll()
}
```
