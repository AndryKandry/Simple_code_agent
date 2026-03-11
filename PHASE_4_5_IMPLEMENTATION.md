# Phase 4.5: MCP Implementation Plan

## Overview

This document outlines the implementation plan for MCP (Model Context Protocol) feature integration in Simple Code Agent CLI.

## Current Status: COMPLETED

### Implemented Components

#### 1. MCP Servers (Built-in)

**Filesystem MCP Server** (`composeApp/src/jvmMain/kotlin/ru/agent/mcp/server/FilesystemMcpServer.kt`)
- Tools: read_file, write_file, list_directory, search_files, delete_file, create_directory, file_exists, copy_file
- Security: Path validation, allowed roots protection, directory deletion prohibition
- Direct API for non-MCP usage

**Terminal MCP Server** (`composeApp/src/jvmMain/kotlin/ru/agent/mcp/server/TerminalMcpServer.kt`)
- Tools: execute_command, run_shell_script, check_command_available, list_allowed_commands, get_environment_info
- Security: Command whitelist, dangerous patterns blacklist, timeout protection
- Project-only script execution

#### 2. MCP Client (External Servers)

**MCP Client** (`composeApp/src/jvmMain/kotlin/ru/agent/mcp/client/McpClient.kt`)
- HTTP/SSE transport support
- Tool discovery and execution
- Multiple simultaneous connections
- Connection state management

**Data Models** (`composeApp/src/jvmMain/kotlin/ru/agent/mcp/client/McpServerConnection.kt`)
- McpServerConfig
- McpConnectionStatus
- McpServerConnection
- McpToolInfo
- McpResourceInfo
- McpExecuteResult
- McpServerPresets

#### 3. MCP Manager (Unified Interface)

**MCP Manager** (`composeApp/src/jvmMain/kotlin/ru/agent/mcp/McpManager.kt`)
- Unified API for built-in and external servers
- Tool discovery across all servers
- Tool execution by full name (server:tool)
- Connection management

#### 4. CLI Commands

**MCP Command Group** (`composeApp/src/jvmMain/kotlin/ru/agent/cli/commands/McpCommand.kt`)
```
agent mcp list              List available MCP servers
agent mcp status            Show MCP status
agent mcp tools             List all available tools
agent mcp connect <server>  Connect to external server
agent mcp disconnect <server> Disconnect from server
agent mcp exec <server:tool> <args>  Execute tool
agent mcp read <path>       Quick file read
agent mcp write <path> <content> Quick file write
agent mcp run <command>     Quick command execution
```

#### 5. Dependency Injection

**MCP Module** (`composeApp/src/jvmMain/kotlin/ru/agent/mcp/di/McpModule.kt`)
- HttpClient with SSE support
- FilesystemMcpServer singleton
- TerminalMcpServer singleton
- McpClient singleton
- McpManager singleton

**CLI Module Integration** (`composeApp/src/jvmMain/kotlin/ru/agent/cli/di/CliModule.kt`)
- Includes mcpModule

#### 6. Agent Definitions

**MCP Filesystem Server Agent** (`.claude/agents/mcp-filesystem-server-agent.md`)
- Expert in MCP protocol and filesystem operations
- Kotlin implementation guidelines
- Security best practices

**MCP Terminal Server Agent** (`.claude/agents/mcp-terminal-server-agent.md`)
- Expert in MCP protocol and terminal commands
- Security guidelines for command execution
- Whitelist/blacklist patterns

**MCP Client Agent** (`.claude/agents/mcp-client-agent.md`)
- Expert in external MCP server connections
- HTTP/SSE transport handling
- Tool discovery and execution

---

## Architecture

### Clean Architecture Layers

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ McpCommand  │  │ CliApp      │  │ McpListCommand      │  │
│  │             │  │             │  │ McpStatusCommand    │  │
│  │             │  │             │  │ McpToolsCommand     │  │
│  │             │  │             │  │ McpConnectCommand   │  │
│  │             │  │             │  │ McpExecCommand      │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                            │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │                    McpManager                            │ │
│  │  - Unified API for all MCP operations                    │ │
│  │  - Tool discovery and execution                          │ │
│  │  - Connection management                                 │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       Data Layer                             │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐ │
│  │ FilesystemMcp  │  │ TerminalMcp    │  │ McpClient      │ │
│  │ Server         │  │ Server         │  │                │ │
│  │                │  │                │  │                │ │
│  │ - read_file    │  │ - execute_cmd  │  │ - HTTP/SSE     │ │
│  │ - write_file   │  │ - run_script   │  │ - Tool disc.   │ │
│  │ - list_dir     │  │ - check_cmd    │  │ - Tool exec    │ │
│  │ - search_files │  │ - list_allowed │  │                │ │
│  └────────────────┘  └────────────────┘  └────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Dependency Injection

```
┌─────────────────────────────────────────────────────────────┐
│                        Koin DI                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │                     mcpModule                            │ │
│  │  HttpClient (SSE) ──────────────────────────────────────┼─┐
│  │  FilesystemMcpServer ───────────────────────────────────┼─┤
│  │  TerminalMcpServer ─────────────────────────────────────┼─┤
│  │  McpClient ─────────────────────────────────────────────┼─┤
│  │  McpManager ────────────────────────────────────────────┼─┘
│  └─────────────────────────────────────────────────────────┘  │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │                     cliModule                            │  │
│  │  includes(mcpModule)                                     │  │
│  │  CliChatController ─────────────────────────────────────┼──┘
│  │  ProgressTracker                                         │
│  │  CliAnimator                                             │
│  └─────────────────────────────────────────────────────────┘
└─────────────────────────────────────────────────────────────┘
```

---

## Usage Examples

### 1. Using MCP Commands

```bash
# List available servers
./agent mcp list

# Check MCP status
./agent mcp status

# List all tools
./agent mcp tools

# List tools from specific server
./agent mcp tools -s filesystem

# Read a file
./agent mcp read /path/to/file.txt

# Write a file
./agent mcp write /path/to/file.txt "Hello, World!"

# List directory
./agent mcp exec filesystem:list_directory '{"path": "/project"}'

# Execute terminal command
./agent mcp run git status

# Connect to external server
./agent mcp connect github --url http://localhost:3000/mcp

# Execute tool on external server
./agent mcp exec github:search_code '{"query": "fun main"}'

# Disconnect
./agent mcp disconnect github
```

### 2. Programmatic Usage

```kotlin
// Get MCP Manager from DI
val manager: McpManager by inject()

// Built-in servers
val content = manager.readFile("/path/to/file.txt")
val result = manager.writeFile("/path/to/file.txt", "content")
val output = manager.executeCommand("git status")

// External servers
manager.connectToServer("github", "http://localhost:3000/mcp")
val tools = manager.listExternalTools("github")
val execResult = manager.executeExternalTool(
    "github",
    "search_code",
    mapOf("query" to "test")
)

// Unified tool execution
manager.executeToolByFullName("filesystem:read_file", mapOf("path" to "/file.txt"))
manager.executeToolByFullName("terminal:execute_command", mapOf("command" to "ls"))
manager.executeToolByFullName("github:search_code", mapOf("query" to "test"))
```

### 3. Standalone MCP Server

```bash
# Run Filesystem MCP Server as standalone
java -cp app.jar ru.agent.mcp.server.FilesystemMcpServerKt

# Run Terminal MCP Server as standalone
java -cp app.jar ru.agent.mcp.server.TerminalMcpServerKt

# Use with MCP Inspector
npx -y @modelcontextprotocol/inspector java -cp app.jar ru.agent.mcp.server.FilesystemMcpServerKt
```

---

## Security

### Filesystem MCP Server

1. **Allowed Roots**: Only paths within configured directories are accessible
2. **Path Validation**: Canonical path comparison prevents traversal attacks
3. **No Directory Deletion**: Directory deletion is prohibited for safety
4. **Confirmation Required**: File deletion requires explicit confirmation

### Terminal MCP Server

1. **Command Whitelist**: Only explicitly allowed commands can be executed
2. **Dangerous Patterns**: Blacklist blocks rm -rf, sudo, chmod 777, etc.
3. **Timeout Protection**: Commands are terminated after timeout
4. **Project-only Scripts**: Shell scripts must be within project directory

---

## External MCP Servers

### Supported Presets

| Server | URL | Description |
|--------|-----|-------------|
| GitHub | http://localhost:3000/mcp | GitHub API access |
| PostgreSQL | http://localhost:3001/mcp | Database operations |
| Memory | http://localhost:3002/mcp | Knowledge graph |

### Running External Servers

```bash
# GitHub MCP
npx -y @modelcontextprotocol/server-github

# PostgreSQL MCP
npx -y @modelcontextprotocol/server-postgres

# Memory MCP
npx -y @modelcontextprotocol/server-memory
```

---

## Future Enhancements

### Phase 4.6: AI Agent Integration

- [ ] Integrate MCP tools with DeepSeek chat
- [ ] Automatic tool selection based on user queries
- [ ] Tool execution logging and audit
- [ ] Dynamic tool discovery from AI responses

### Phase 4.7: Advanced Features

- [ ] WebSocket transport support
- [ ] Tool caching and optimization
- [ ] Parallel tool execution
- [ ] Tool composition and workflows
- [ ] Custom tool creation UI

### Phase 4.8: Configuration

- [ ] MCP server configuration file (mcp-config.json)
- [ ] Environment-based configuration
- [ ] Per-project MCP settings
- [ ] Tool permission levels

---

## Check-list: MCP Implementation

- [x] Filesystem MCP Server
  - [x] read_file tool
  - [x] write_file tool
  - [x] list_directory tool
  - [x] search_files tool
  - [x] delete_file tool (with confirmation)
  - [x] create_directory tool
  - [x] file_exists tool
  - [x] copy_file tool
  - [x] Path validation
  - [x] Security measures

- [x] Terminal MCP Server
  - [x] execute_command tool
  - [x] run_shell_script tool
  - [x] check_command_available tool
  - [x] list_allowed_commands tool
  - [x] get_environment_info tool
  - [x] Command whitelist
  - [x] Dangerous patterns blacklist
  - [x] Timeout protection

- [x] MCP Client
  - [x] HTTP/SSE transport
  - [x] Connection management
  - [x] Tool discovery
  - [x] Tool execution
  - [x] Multiple connections

- [x] MCP Manager
  - [x] Unified API
  - [x] Built-in server management
  - [x] External server management
  - [x] Tool execution by name

- [x] CLI Commands
  - [x] mcp list
  - [x] mcp status
  - [x] mcp tools
  - [x] mcp connect
  - [x] mcp disconnect
  - [x] mcp exec
  - [x] mcp read
  - [x] mcp write
  - [x] mcp run

- [x] Dependency Injection
  - [x] mcpModule
  - [x] Integration with cliModule

- [x] Agent Definitions
  - [x] mcp-filesystem-server-agent.md
  - [x] mcp-terminal-server-agent.md
  - [x] mcp-client-agent.md

- [x] Documentation
  - [x] CLI_README.md updates
  - [x] PHASE_4_5_IMPLEMENTATION.md

---

## Testing

### Manual Testing

```bash
# Start CLI
./agent

# Test filesystem
./agent mcp read README.md
./agent mcp write test.txt "Hello"
./agent mcp exec filesystem:list_directory '{"path": "."}'

# Test terminal
./agent mcp run ls -la
./agent mcp run git status
./agent mcp exec terminal:list_allowed_commands

# Test external servers (if running)
./agent mcp connect github
./agent mcp tools -s github
```

### MCP Inspector Testing

```bash
# Test Filesystem MCP Server
npx -y @modelcontextprotocol/inspector java -cp app.jar ru.agent.mcp.server.FilesystemMcpServerKt

# Test Terminal MCP Server
npx -y @modelcontextprotocol/inspector java -cp app.jar ru.agent.mcp.server.TerminalMcpServerKt
```

---

## Conclusion

Phase 4.5 MCP implementation is complete. All built-in MCP servers (Filesystem and Terminal), the MCP client for external servers, the unified MCP Manager, and comprehensive CLI commands are fully implemented and integrated into the Simple Code Agent CLI application.

The implementation follows Clean Architecture principles, uses Koin for dependency injection, and provides both programmatic API and CLI commands for all MCP operations.
