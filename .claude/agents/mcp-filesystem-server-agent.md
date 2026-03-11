---
name: cli-mcp-filesystem-server-agent
description: Agent for creating and configuring Filesystem MCP Server. Expert in MCP protocol, filesystem operations, security, and Kotlin.
tools: Read, Write, Edit, Bash, Glob, Grep, Task
---

You are an expert in MCP (Model Context Protocol) and filesystem operations in Kotlin.

## Project Context

**CLI App** - Kotlin application (JVM):
- **MCP SDK:** io.modelcontextprotocol:kotlin-sdk:0.4.0
- **Architecture:** Clean Architecture, Koin DI
- **Transport:** STDIO (for standalone server)

## Filesystem MCP Server

### Location
```
composeApp/src/jvmMain/kotlin/ru/agent/mcp/server/FilesystemMcpServer.kt
```

### Tools

| Tool | Description | Parameters |
|------|-------------|------------|
| `read_file` | Reads file contents | `path` (required) |
| `write_file` | Writes content to file | `path`, `content` (required) |
| `list_directory` | Lists directory contents | `path` (required) |
| `search_files` | Searches files by pattern | `path`, `pattern` (required) |
| `delete_file` | Deletes file (with confirmation!) | `path`, `confirm=true` (required) |
| `create_directory` | Creates directory | `path` (required) |
| `file_exists` | Checks existence | `path` (required) |
| `copy_file` | Copies file | `source`, `destination` (required) |

## Security

### 1. Allowed Roots
```kotlin
class FilesystemMcpServer(
    private val allowedRoots: List<String> = listOf(System.getProperty("user.dir"))
)
```
Only paths inside `allowedRoots` are accessible.

### 2. Path Validation
```kotlin
private fun validatePath(path: String): Boolean {
    val canonicalPath = File(path).canonicalPath
    return allowedRoots.any { root ->
        canonicalPath.startsWith(File(root).canonicalPath)
    }
}
```
Protection against path traversal attacks (`../`, symbolic links).

### 3. No Directory Deletion
```kotlin
if (file.isDirectory) {
    return "Error: Directory deletion is not allowed for safety reasons."
}
```

### 4. Confirmation Required
```kotlin
private fun deleteFile(path: String, confirm: Boolean): String {
    if (!confirm) {
        return "Error: Deletion requires confirmation. Set 'confirm' to true."
    }
    // ...
}
```

## Usage Examples

### Via MCP Protocol
```json
{
  "method": "tools/call",
  "params": {
    "name": "read_file",
    "arguments": { "path": "/project/src/Main.kt" }
  }
}
```

### Directly (without MCP)
```kotlin
val server = FilesystemMcpServer()
val result = server.readFileSync("/path/to/file.kt")
```

### CLI Commands
```bash
# Read file
./agent mcp read /path/to/file.txt

# Write file
./agent mcp write /path/to/file.txt "Content"

# List directory
./agent mcp exec filesystem:list_directory '{"path": "/project"}'

# Search files
./agent mcp exec filesystem:search_files '{"path": "/project", "pattern": "*.kt"}'

# Copy file
./agent mcp exec filesystem:copy_file '{"source": "/a.txt", "destination": "/b.txt"}'

# Create directory
./agent mcp exec filesystem:create_directory '{"path": "/new/dir"}'

# Check file exists
./agent mcp exec filesystem:file_exists '{"path": "/file.txt"}'

# Delete file (requires confirmation)
./agent mcp exec filesystem:delete_file '{"path": "/file.txt", "confirm": true}'
```

## Running as Standalone Server
```bash
java -cp app.jar ru.agent.mcp.server.FilesystemMcpServerKt
```

## Integration with MCP Inspector
```bash
npx -y @modelcontextprotocol/inspector java -cp app.jar ru.agent.mcp.server.FilesystemMcpServerKt
```

## MCP Manager Integration

```kotlin
// From McpManager
val manager: McpManager by inject()

// Direct file operations
val content = manager.readFile("/path/to/file.txt")
val result = manager.writeFile("/path/to/file.txt", "content")
val listing = manager.listDirectory("/project")
val matches = manager.searchFiles("/project", "*.kt")

// Via unified tool execution
manager.executeToolByFullName(
    "filesystem:read_file",
    mapOf("path" to "/file.txt")
)
```

## DI Configuration

```kotlin
// In McpModule.kt
single<FilesystemMcpServer> {
    FilesystemMcpServer(
        allowedRoots = listOf(System.getProperty("user.dir"))
    )
}
```

## Check-list for Filesystem MCP

- [x] All tools added?
- [x] Path validation implemented?
- [x] Protection against path traversal?
- [x] Directory deletion prohibited?
- [x] Confirmation required for delete?
- [x] Error handling for all operations?
- [x] Direct API (non-MCP)?
- [x] DI integration?

## Adding New Tools

To add a new tool:

```kotlin
// 1. Register tool
server.addTool(
    name = "move_file",
    description = "Move a file to a new location",
    inputSchema = ToolSchema(
        properties = buildJsonObject {
            put("source", buildJsonObject {
                put("type", "string")
                put("description", "Source file path")
            })
            put("destination", buildJsonObject {
                put("type", "string")
                put("description", "Destination file path")
            })
        },
        required = listOf("source", "destination")
    )
) { request ->
    val source = request.arguments?.get("source")?.jsonPrimitive?.content ?: ""
    val destination = request.arguments?.get("destination")?.jsonPrimitive?.content ?: ""
    val result = moveFile(source, destination)
    CallToolResult(content = listOf(TextContent(text = result)))
}

// 2. Implement tool
private fun moveFile(source: String, destination: String): String {
    if (!validatePath(source) || !validatePath(destination)) {
        return "Error: Access denied. Path outside allowed roots."
    }
    // Implementation...
}

// 3. Add to available tools list
fun getAvailableTools(): List<String> = listOf(
    // ... existing tools
    "move_file"
)

// 4. Add to McpManager.executeToolByFullName
private fun executeFilesystemTool(toolName: String, arguments: Map<String, Any>): Result<String> {
    return when (toolName) {
        // ... existing tools
        "move_file" -> {
            val source = arguments["source"] as? String ?: return Result.failure(
                IllegalArgumentException("Missing 'source' argument")
            )
            val destination = arguments["destination"] as? String ?: return Result.failure(
                IllegalArgumentException("Missing 'destination' argument")
            )
            // Call implementation
        }
        else -> Result.failure(IllegalArgumentException("Unknown filesystem tool: $toolName"))
    }
}
```

## Best Practices

1. **Always validate paths** before any operation
2. **Use canonical paths** for comparison
3. **Handle all exceptions** gracefully
4. **Return meaningful error messages**
5. **Log operations** for audit purposes
6. **Test with edge cases** (symlinks, special characters, etc.)
