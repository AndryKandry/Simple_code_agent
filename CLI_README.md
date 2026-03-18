# Simple Code Agent - CLI Documentation

**Comprehensive guide to the Command Line Interface**

---

## Table of Contents

- [Overview](#overview)
- [Installation & Setup](#installation--setup)
- [Quick Start](#quick-start)
- [REPL Mode](#repl-mode)
- [CLI Commands Reference](#cli-commands-reference)
- [MCP Integration](#mcp-integration)
- [Scheduler](#scheduler)
- [Invariants & State Machine](#invariants--state-machine)
- [Memory System](#memory-system)
- [Security](#security)
- [Advanced Usage](#advanced-usage)
- [Configuration](#configuration)
- [Troubleshooting](#troubleshooting)

---

## Overview

Simple Code Agent CLI — это AI-powered ассистент для разработки с полноценным терминальным интерфейсом.

### Key Capabilities

| Feature | Description |
|---------|-------------|
| **REPL Mode** | Interactive session with AI agent |
| **Direct Commands** | One-shot command execution |
| **MCP Protocol** | Integration with external tools |
| **Orchestration** | LLM-driven task planning |
| **Scheduler** | Cron-based background tasks |
| **State Machine** | Controlled task state transitions |
| **Memory System** | Three-level memory management |

---

## Installation & Setup

### Prerequisites

- **JDK 17+**
- **Gradle 8.x**
- **DeepSeek API Key**

### Environment Setup

```bash
# Clone repository
git clone https://github.com/yourusername/Simple_code_agent.git
cd Simple_code_agent

# Set API key (required)
export DEEPSEEK_API_KEY=your_api_key_here

# Optional: Set allowed directories for filesystem MCP
export MCP_ALLOWED_ROOTS=/Users/you/projects,/tmp
```

### Running

```bash
# Recommended: Using wrapper script
./agent                          # REPL mode
./agent chat send "Привет"       # Direct command

# Alternative: Using Gradle directly
./gradlew :composeApp:run --no-configuration-cache
```

> **IMPORTANT:** Always use `--no-configuration-cache` flag with Gradle!

---

## Quick Start

### 5-Minute Tutorial

```bash
# 1. Start REPL
./agent

# 2. Chat with AI
> Привет, напиши hello world на Kotlin
[Agent] Вот пример на Kotlin:
```kotlin
fun main() {
    println("Hello, World!")
}
```

# 3. Execute shell command
> /shell ls -la
[OUTPUT]
total 48
drwxr-xr-x  12 user  staff   384 Jan 15 10:23 .
-rw-r--r--   1 user  staff  1234 Jan 15 10:20 build.gradle.kts

# 4. Use MCP tools
> /mcp read README.md
[FILE CONTENT]
# Simple Code Agent
...

# 5. Check profile
> /profile show
[PROFILE]
Name: Andrey
Email: user@example.com

# 6. Exit
> /exit
Goodbye!
```

---

## REPL Mode

REPL (Read-Eval-Print Loop) — интерактивный режим для работы с AI-агентом.

### Starting REPL

```bash
./agent
```

```
╭──────────────────────────────────────────────────────────────────╮
│                    Simple Code Agent REPL                        │
│                                                                  │
│  Version: 1.0.0                                                  │
│  Type /help for commands, /exit or Ctrl+D to quit               │
│  Arrow keys: navigate history, Tab: autocomplete                │
╰──────────────────────────────────────────────────────────────────╯

>
```

### REPL Commands

#### Core Commands

| Command | Alias | Description | Example |
|---------|-------|-------------|---------|
| `/help` | `/h`, `/?` | Show help | `/help` |
| `/exit` | `/quit`, `/q` | Exit REPL | `/exit` |
| `/clear` | `/cls` | Clear screen | `/clear` |

#### Chat Commands

| Command | Description | Example |
|---------|-------------|---------|
| `/chat <message>` | Send message to AI | `/chat Напиши функцию сортировки` |
| `/history` | Show chat history | `/history` |
| `/clear-history` | Clear chat history | `/clear-history` |

> **Note:** You can also type message directly without `/chat` prefix.

#### Profile Commands

| Command | Description | Example |
|---------|-------------|---------|
| `/profile show` | Display current profile | `/profile show` |
| `/profile update` | Update profile | `/profile update --name "Andrey"` |

#### Memory Commands

| Command | Description | Example |
|---------|-------------|---------|
| `/memory list` | List memory entries | `/memory list` |
| `/memory search <query>` | Search memory | `/memory search "Kotlin"` |
| `/memory clear` | Clear memory | `/memory clear` |

#### Task Commands

| Command | Description | Example |
|---------|-------------|---------|
| `/task list` | List all tasks | `/task list` |
| `/task create <title>` | Create new task | `/task create "Implement feature X"` |
| `/task show <id>` | Show task details | `/task show abc123` |
| `/task update <id>` | Update task state | `/task update abc123 --state IN_PROGRESS` |

#### Shell Commands

| Command | Description | Example |
|---------|-------------|---------|
| `/shell <command>` | Execute shell command | `/shell git status` |
| `/shell! <command>` | Execute without confirmation | `/shell! ls -la` |

#### MCP Commands

| Command | Description | Example |
|---------|-------------|---------|
| `/mcp list` | List MCP servers | `/mcp list` |
| `/mcp tools` | List all tools | `/mcp tools` |
| `/mcp read <path>` | Read file via MCP | `/mcp read src/main.kt` |
| `/mcp write <path>` | Write file via MCP | `/mcp write test.txt "Hello"` |
| `/mcp run <cmd>` | Run command via MCP | `/mcp run git status` |
| `/mcp exec <tool>` | Execute specific tool | `/mcp exec filesystem:list_directory '{"path":"."}' ` |

#### Scheduler Commands

| Command | Description | Example |
|---------|-------------|---------|
| `/scheduler list` | List scheduled tasks | `/scheduler list` |
| `/scheduler remind <msg>` | Create reminder | `/scheduler remind "Meeting" --in 5m` |
| `/scheduler command <cmd>` | Schedule command | `/scheduler command "./build.sh" --cron "0 9 * * 1-5"` |
| `/scheduler status` | Show scheduler status | `/scheduler status` |

#### Invariant Commands

| Command | Description | Example |
|---------|-------------|---------|
| `/invariant list` | List invariants | `/invariant list` |
| `/invariant toggle <id>` | Toggle invariant | `/invariant toggle inv1` |
| `/invariant validate <id>` | Validate state | `/invariant validate task123` |

### Keyboard Shortcuts

| Key | Action |
|-----|--------|
| `↑` / `↓` | Navigate command history |
| `Tab` | Autocomplete command |
| `Ctrl+D` | Exit REPL |
| `Ctrl+C` | Cancel current input |
| `Ctrl+L` | Clear screen |
| `Ctrl+R` | Search history |

### REPL Features

#### 1. Command History

```bash
> /shell git status
> /chat Привет
> /history
[1] /shell git status
[2] /chat Привет
```

#### 2. Autocomplete

```bash
> /sch<Tab>        # Completes to /scheduler
> /mcp r<Tab>      # Completes to /mcp read
```

#### 3. Multi-line Input

```bash
> /chat Напиши функцию, которая:
> 1. Принимает список чисел
> 2. Возвращает их сумму
> 3. Обрабатывает ошибки
> <Enter><Enter>  # Double Enter to send
```

---

## CLI Commands Reference

### Direct Command Mode

Execute commands without entering REPL:

```bash
./agent <command> [options]
```

### Chat Commands

```bash
# Send message
./agent chat send "Привет, как дела?"

# Send message with session
./agent chat send "Продолжим" --session abc123

# Show history
./agent chat history
./agent chat history --limit 20

# Clear session
./agent chat clear
./agent chat clear --session abc123

# Create new session
./agent chat new
```

### Profile Commands

```bash
# Show profile
./agent profile show

# Update profile
./agent profile update --name "Andrey"
./agent profile update --email "user@example.com"
./agent profile update --preferences '{"language": "ru", "theme": "dark"}'

# Reset profile
./agent profile reset
```

### Memory Commands

```bash
# List memory
./agent memory list
./agent memory list --type working
./agent memory list --limit 50

# Search memory
./agent memory search "Kotlin"
./agent memory search "MCP" --type knowledge

# Add memory entry
./agent memory add "Important note about project" --type working

# Clear memory
./agent memory clear
./agent memory clear --type short_term
```

### Task Commands

```bash
# List tasks
./agent task list
./agent task list --state PENDING
./agent task list --limit 20

# Create task
./agent task create "Implement authentication"
./agent task create "Write tests" --priority HIGH

# Show task details
./agent task show <task-id>

# Update task
./agent task update <task-id> --state IN_PROGRESS
./agent task update <task-id> --priority HIGH
./agent task update <task-id> --assignee "user@example.com"

# Delete task
./agent task delete <task-id>
```

### Shell Commands

```bash
# Execute command
./agent shell ls -la
./agent shell git status
./agent shell find . -name "*.kt"

# With timeout
./agent shell "./gradlew build" --timeout 120000

# With working directory
./agent shell npm test --cwd /path/to/project

# Skip confirmation
./agent shell rm temp.txt --force
```

### Scheduler Commands

```bash
# List scheduled tasks
./agent scheduler list
./agent scheduler list --active

# Schedule reminder
./agent scheduler remind "Meeting in 5 min" --in 5m
./agent scheduler remind "Daily standup" --at "09:00"

# Schedule command
./agent scheduler command "./gradlew test" --cron "0 9 * * 1-5"
./agent scheduler command "npm run build" --in 1h

# Schedule MCP tool
./agent scheduler mcp filesystem:read_file '{"path": "/logs/app.log"}' --cron "0 0 * * *"

# Task management
./agent scheduler show <task-id>
./agent scheduler pause <task-id>
./agent scheduler resume <task-id>
./agent scheduler cancel <task-id>
./agent scheduler delete <task-id>

# Scheduler status
./agent scheduler status
```

### Invariant Commands

```bash
# List invariants
./agent invariant list
./agent invariant list --active

# Toggle invariant
./agent invariant toggle <invariant-id>

# Validate state
./agent invariant validate <task-id>
./agent invariant validate-all

# Show invariant details
./agent invariant show <invariant-id>
```

### MCP Commands

```bash
# Server management
./agent mcp list
./agent mcp status
./agent mcp connect <name> --url <url>
./agent mcp disconnect <name>
./agent mcp disconnect --all

# Tool discovery
./agent mcp tools
./agent mcp tools -s filesystem
./agent mcp tools -v

# Quick file operations
./agent mcp read <path>
./agent mcp write <path> "content"
./agent mcp ls <path>
./agent mcp find <path> <pattern>

# Quick terminal operations
./agent mcp run <command>
./agent mcp run <command> --timeout 60000

# Direct tool execution
./agent mcp exec <server:tool> '<json-args>'
./agent mcp exec filesystem:read_file '{"path": "/file.txt"}'
./agent mcp exec terminal:execute_command '{"command": "ls -la"}'
./agent mcp exec scheduler:schedule_reminder '{"message": "Test", "delayMinutes": 5}'
```

---

## MCP Integration

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                      McpOrchestrator                             │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  analyzeAndPlan() → executeWithDependencies()              │ │
│  └────────────────────────────────────────────────────────────┘ │
│                              │                                   │
│           ┌──────────────────┼──────────────────┐               │
│           ▼                  ▼                  ▼               │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐     │
│  │ Filesystem MCP │  │ Terminal MCP   │  │ Scheduler MCP  │     │
│  │ Server         │  │ Server         │  │ Server         │     │
│  └────────────────┘  └────────────────┘  └────────────────┘     │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                   McpClient (External)                     │  │
│  │  GitHub • PostgreSQL • Memory • Custom servers             │  │
│  └────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Built-in MCP Servers

#### Filesystem MCP Server

| Tool | Description | Parameters |
|------|-------------|------------|
| `read_file` | Read file contents | `path` (required) |
| `write_file` | Write to file | `path`, `content` (required) |
| `list_directory` | List directory | `path` (required) |
| `search_files` | Search by pattern | `path`, `pattern` (required) |
| `delete_file` | Delete file | `path`, `confirm` (required) |
| `create_directory` | Create directory | `path` (required) |
| `file_exists` | Check existence | `path` (required) |
| `copy_file` | Copy file | `source`, `destination` |

**Examples:**

```bash
# Read file
./agent mcp exec filesystem:read_file '{"path": "/project/build.gradle.kts"}'

# Write file
./agent mcp exec filesystem:write_file '{"path": "/project/test.kt", "content": "fun main() = println(\"Hello\")"}'

# List directory
./agent mcp exec filesystem:list_directory '{"path": "/project/src"}'

# Search files
./agent mcp exec filesystem:search_files '{"path": "/project", "pattern": "*.kt"}'

# Check existence
./agent mcp exec filesystem:file_exists '{"path": "/project/README.md"}'

# Copy file
./agent mcp exec filesystem:copy_file '{"source": "/a.txt", "destination": "/b.txt"}'

# Delete file (requires confirmation)
./agent mcp exec filesystem:delete_file '{"path": "/temp.txt", "confirm": true}'
```

#### Terminal MCP Server

| Tool | Description | Parameters |
|------|-------------|------------|
| `execute_command` | Execute command | `command`, `timeout`, `working_dir` |
| `run_shell_script` | Run script | `script_path`, `args`, `timeout` |
| `check_command_available` | Check availability | `command` |
| `list_allowed_commands` | List allowed | - |
| `get_environment_info` | Environment info | - |

**Examples:**

```bash
# Execute command
./agent mcp exec terminal:execute_command '{"command": "git status"}'
./agent mcp exec terminal:execute_command '{"command": "./gradlew build", "timeout": 120000}'

# Run script
./agent mcp exec terminal:run_shell_script '{"script_path": "./scripts/deploy.sh", "args": ["prod"]}'

# Check command
./agent mcp exec terminal:check_command_available '{"command": "docker"}'

# List allowed commands
./agent mcp exec terminal:list_allowed_commands

# Environment info
./agent mcp exec terminal:get_environment_info
```

#### Scheduler MCP Server

| Tool | Description |
|------|-------------|
| `schedule_reminder` | Create reminder |
| `schedule_command` | Schedule command |
| `schedule_mcp_tool` | Schedule MCP tool |
| `list_scheduled_tasks` | List tasks |
| `get_task` | Get task details |
| `cancel_task` | Cancel task |
| `delete_task` | Delete task |
| `pause_task` | Pause task |
| `resume_task` | Resume task |
| `get_task_history` | Execution history |

**Examples:**

```bash
# Schedule reminder
./agent mcp exec scheduler:schedule_reminder '{"message": "Standup meeting", "delayMinutes": 5}'

# Schedule command with cron
./agent mcp exec scheduler:schedule_command '{"command": "./backup.sh", "cronExpression": "0 2 * * *"}'

# Schedule MCP tool
./agent mcp exec scheduler:schedule_mcp_tool '{"serverName": "filesystem", "toolName": "read_file", "arguments": {"path": "/logs/app.log"}, "cronExpression": "0 0 * * *"}'

# List tasks
./agent mcp exec scheduler:list_scheduled_tasks

# Cancel task
./agent mcp exec scheduler:cancel_task '{"taskId": "task-123"}'
```

### Orchestration

MCP Orchestration обеспечивает интеллектуальное планирование и выполнение:

```
User Request
     │
     ▼
┌─────────────────┐
│ Classification  │  Rule-based pattern matching
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Planning      │  LLM-driven (DeepSeek)
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Execution      │  Parallel with dependencies
└────────┬────────┘
         │
         ▼
    Response
```

**Request Types:**

| Type | Servers Used | Examples |
|------|--------------|----------|
| `FILE_OPERATION` | filesystem | "Read file X", "Create directory Y" |
| `TERMINAL_COMMAND` | terminal | "Run git status", "Execute npm test" |
| `SCHEDULING` | scheduler | "Remind me in 5 min", "Schedule backup" |
| `CODE_ANALYSIS` | filesystem + terminal | "Analyze project structure and run tests" |
| `GIT_OPERATION` | terminal | "Commit changes", "Push to remote" |
| `MULTI_TYPE` | multiple | Complex workflows |

---

## Scheduler

### Overview

Cron-based планировщик для фоновых задач.

### Cron Expressions

```
┌───────────── minute (0 - 59)
│ ┌───────────── hour (0 - 23)
│ │ ┌───────────── day of month (1 - 31)
│ │ │ ┌───────────── month (1 - 12)
│ │ │ │ ┌───────────── day of week (0 - 6) (Sunday to Saturday)
│ │ │ │ │
* * * * *
```

**Examples:**

| Expression | Description |
|------------|-------------|
| `* * * * *` | Every minute |
| `0 * * * *` | Every hour |
| `0 9 * * *` | Every day at 9:00 |
| `0 9 * * 1-5` | Weekdays at 9:00 |
| `0 0 1 * *` | First day of month |
| `0 2 * * 0` | Every Sunday at 2:00 |

### Task Types

1. **Reminders** — простые напоминания
2. **Commands** — выполнение shell команд
3. **MCP Tools** — выполнение MCP инструментов

### Usage Examples

```bash
# Reminder in 5 minutes
./agent scheduler remind "Meeting" --in 5m

# Reminder at specific time
./agent scheduler remind "Lunch" --at "12:00"

# Daily build at 9 AM on weekdays
./agent scheduler command "./gradlew build" --cron "0 9 * * 1-5"

# Hourly log check
./agent scheduler mcp terminal:execute_command '{"command": "tail -n 10 /var/log/app.log"}' --cron "0 * * * *"

# Weekly backup
./agent scheduler command "./backup.sh" --cron "0 2 * * 0"
```

### Task Management

```bash
# List all tasks
./agent scheduler list

# Show task details
./agent scheduler show <task-id>

# Pause/Resume
./agent scheduler pause <task-id>
./agent scheduler resume <task-id>

# Cancel/Delete
./agent scheduler cancel <task-id>
./agent scheduler delete <task-id>

# View execution history
./agent scheduler history <task-id>
```

---

## Invariants & State Machine

### Task States

```
┌─────────┐     create      ┌─────────┐
│  (none) │ ──────────────► │ PENDING │
└─────────┘                 └────┬────┘
                                 │
                    start        │
                 ┌───────────────┘
                 ▼
           ┌──────────┐
           │ IN_PROGRESS │
           └─────┬──────┘
                 │
      ┌──────────┼──────────┐
      │          │          │
      ▼          ▼          ▼
┌──────────┐ ┌─────────┐ ┌─────────────┐
│ COMPLETED│ │ BLOCKED │ │  CANCELLED  │
└──────────┘ └─────────┘ └─────────────┘
```

### Valid Transitions

| From | To | Allowed |
|------|-----|---------|
| PENDING | IN_PROGRESS | ✓ |
| PENDING | CANCELLED | ✓ |
| IN_PROGRESS | COMPLETED | ✓ |
| IN_PROGRESS | BLOCKED | ✓ |
| IN_PROGRESS | CANCELLED | ✓ |
| BLOCKED | IN_PROGRESS | ✓ |
| BLOCKED | CANCELLED | ✓ |

### Invariants

Invariants — это правила валидации состояний:

```bash
# List invariants
./agent invariant list

# Output:
┌──────────────────────────────────────────────────────┐
│ Invariants                                           │
├──────────────────┬────────────┬──────────────────────┤
│ ID               │ Active     │ Description          │
├──────────────────┼────────────┼──────────────────────┤
│ inv_priority     │ ✓          │ High priority tasks  │
│ inv_deadline     │ ✓          │ Deadline check       │
│ inv_assignee     │ ✗          │ Assignee required    │
└──────────────────┴────────────┴──────────────────────┘

# Toggle invariant
./agent invariant toggle inv_assignee

# Validate task state
./agent invariant validate task-123
```

---

## Memory System

### Three-Level Memory

```
┌─────────────────────────────────────────────────────────────┐
│                      Memory System                           │
├─────────────────┬─────────────────┬─────────────────────────┤
│  Working Memory │  Short-Term     │  Long-Term (Knowledge)  │
│                 │  Memory         │                         │
├─────────────────┼─────────────────┼─────────────────────────┤
│ Current context │ Recent items    │ Permanent knowledge     │
│ Session data    │ Last N messages │ Project documentation   │
│ Temp data       │ Recent queries  │ User preferences        │
├─────────────────┼─────────────────┼─────────────────────────┤
│ Auto-cleared    │ Time-based      │ Persistent              │
│ on session end  │ expiration      │ across sessions         │
└─────────────────┴─────────────────┴─────────────────────────┘
```

### Memory Commands

```bash
# List working memory
./agent memory list --type working

# Search knowledge base
./agent memory search "Kotlin coroutines" --type knowledge

# Add to working memory
./agent memory add "Current task: implement auth" --type working

# Add to knowledge base
./agent memory add "Project uses Koin for DI" --type knowledge --permanent

# Clear short-term memory
./agent memory clear --type short_term
```

---

## Security

### Terminal Security

#### Whitelist (Allowed Commands)

```
File Operations:
  ls, dir, pwd, cd, tree, cat, head, tail, less, more,
  find, locate, whereis, which, grep, egrep, fgrep, rg,
  stat, file, du, df

Process Management:
  ps, top, htop

System Info:
  whoami, hostname, uname, date, echo

Build Tools:
  gradlew, gradle, mvn, npm, yarn, cargo, go, make

Runtimes:
  python, python3, node, ruby, java, kotlin

Version Control:
  git, svn, hg

Network:
  curl, wget

Containers:
  docker, kubectl
```

#### Blacklist (Blocked Patterns)

| Pattern | Reason |
|---------|--------|
| `rm -rf` | Recursive deletion |
| `sudo` | Superuser execution |
| `chmod 777` | Insecure permissions |
| `mkfs` | Disk formatting |
| `dd if=` | Disk operations |
| `> /dev/` | Device access |
| `curl \| bash` | Remote code execution |
| Shell injection (`;`, `|`, `&`, `$()`) | Command injection |

### Filesystem Security

- **Allowed Roots**: Only whitelisted directories accessible
- **Path Validation**: Protection against path traversal (`../`)
- **No Directory Deletion**: Directories cannot be deleted via MCP
- **Confirmation Required**: File deletion needs explicit confirmation

### Configuration

```bash
# Set allowed directories
export MCP_ALLOWED_ROOTS=/Users/you/projects,/tmp,/var/logs

# In ~/.agent/config.yaml
filesystem:
  allowed_roots:
    - /Users/you/projects
    - /tmp
  max_file_size: 10MB
```

---

## Advanced Usage

### 1. Chaining Commands

```bash
# Multiple operations
./agent shell "git status" && ./agent shell "git add ." && ./agent chat send "Review my changes"
```

### 2. Session Management

```bash
# Create named session
./agent chat new --name "code-review"

# Continue session
./agent chat send "Continue review" --session code-review

# List sessions
./agent chat sessions
```

### 3. Batch Operations

```bash
# Execute multiple MCP operations
./agent mcp exec filesystem:read_file '{"path": "a.txt"}' && \
./agent mcp exec filesystem:read_file '{"path": "b.txt"}'
```

### 4. Output Redirection

```bash
# Save output to file
./agent shell "git log --oneline" > git_history.txt

# Pipe to other commands
./agent mcp tools | grep filesystem
```

### 5. Using with Scripts

```bash
#!/bin/bash
# daily_report.sh

echo "=== Git Status ==="
./agent shell "git status"

echo "=== Recent Commits ==="
./agent shell "git log --oneline -10"

echo "=== TODO Tasks ==="
./agent task list --state PENDING
```

### 6. Automation with Scheduler

```bash
# Daily report at 6 PM
./agent scheduler command "./daily_report.sh" --cron "0 18 * * 1-5"

# Hourly health check
./agent scheduler mcp terminal:execute_command '{"command": "curl -s http://localhost:8080/health"}' --cron "0 * * * *"
```

---

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DEEPSEEK_API_KEY` | DeepSeek API key | (required) |
| `MCP_ALLOWED_ROOTS` | Allowed directories | `.` (current dir) |
| `AGENT_PROFILE` | Profile name | `default` |
| `AGENT_LOG_LEVEL` | Log level | `INFO` |
| `AGENT_DB_PATH` | Database path | `~/.agent/agent.db` |

### Configuration File

`~/.agent/config.yaml`:

```yaml
# Profile settings
profile:
  name: "Andrey"
  email: "user@example.com"
  preferences:
    language: "ru"
    theme: "dark"

# LLM settings
llm:
  provider: "deepseek"
  model: "deepseek-chat"
  temperature: 0.7
  max_tokens: 4096

# MCP settings
mcp:
  filesystem:
    enabled: true
    allowed_roots:
      - /Users/you/projects
      - /tmp
    max_file_size: 10MB

  terminal:
    enabled: true
    timeout: 60000
    confirm_dangerous: true

  scheduler:
    enabled: true
    max_concurrent: 10

# Memory settings
memory:
  working_memory_limit: 100
  short_term_ttl: 3600  # 1 hour
  knowledge_base_enabled: true

# Logging
logging:
  level: INFO
  file: ~/.agent/logs/agent.log
```

---

## Troubleshooting

### Common Issues

#### 1. Configuration Cache Error

**Problem:**
```
Configuration cache problems found in this build.
```

**Solution:**
```bash
./gradlew :composeApp:run --no-configuration-cache
```

#### 2. SLF4J Warnings

**Problem:**
```
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder"
```

**Solution:** These warnings can be safely ignored.

#### 3. Terminal Colors Not Working

**Problem:** No colors in output

**Solution:**
- macOS/Linux: Use Terminal.app, iTerm2, or gnome-terminal
- Windows: Use Windows Terminal or enable ANSI support in PowerShell

#### 4. Command Not Allowed

**Problem:**
```
Error: Command 'rm' is not in the whitelist
```

**Solution:** Use only whitelisted commands, or contact admin to update whitelist.

#### 5. MCP Connection Failed

**Problem:**
```
Error: Failed to connect to MCP server
```

**Solution:**
```bash
# Check server status
./agent mcp status

# Restart connection
./agent mcp disconnect github
./agent mcp connect github --url http://localhost:3000/mcp
```

#### 6. Database Locked

**Problem:**
```
Error: database is locked
```

**Solution:**
```bash
# Close other instances
pkill -f "agent"

# Or use different database
export AGENT_DB_PATH=/tmp/agent.db
```

### Debug Mode

```bash
# Enable debug logging
export AGENT_LOG_LEVEL=DEBUG
./agent

# View logs
tail -f ~/.agent/logs/agent.log
```

---

## Architecture

### Clean Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    CLI Presentation Layer                        │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  cli/                                                       │ │
│  │  ├── CliApp.kt          - Entry point                      │ │
│  │  ├── commands/          - CLIkt command implementations     │ │
│  │  │   ├── ChatCommand.kt                                    │ │
│  │  │   ├── ProfileCommand.kt                                 │ │
│  │  │   ├── TaskCommand.kt                                    │ │
│  │  │   ├── McpCommand.kt                                     │ │
│  │  │   ├── ShellCommand.kt                                   │ │
│  │  │   └── SchedulerCommand.kt                               │ │
│  │  ├── repl/              - REPL controller                  │ │
│  │  │   ├── ReplController.kt                                 │ │
│  │  │   └── ReplCommands.kt                                   │ │
│  │  ├── formatters/        - Output formatting                │ │
│  │  └── visualization/     - Terminal UI components           │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Domain Layer (commonMain)                   │
│  Use Cases • Repository Interfaces • Domain Models              │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Data Layer                                 │
│  Room Database • DeepSeek API • MCP Servers • Scheduler Engine  │
└─────────────────────────────────────────────────────────────────┘
```

### Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| CLI Framework | CLIkt | 4.4.0 |
| Terminal UI | Mordant | 2.2.0 |
| Readline | JLine | 3.25.1 |
| DI | Koin | 4.1.1 |
| Database | Room | 2.8.3 |
| HTTP | Ktor | 3.3.1 |
| MCP | MCP SDK | 0.4.0 |

---

## Platforms

| Platform | Support | Terminal | Notes |
|----------|---------|----------|-------|
| macOS | Full | Terminal.app, iTerm2, Alacritty | Best experience |
| Linux | Full | gnome-terminal, konsole, xterm, Alacritty | Full ANSI support |
| Windows | Partial | Windows Terminal, PowerShell | Limited ANSI colors in CMD |
| Android | UI Only | - | Compose UI |
| iOS | UI Only | - | Compose UI |

---

## License

MIT License

---

## Authors

Simple Code Agent Team

---

## See Also

- [README.md](README.md) - General project documentation
- [docs/day20-orchestration-mcp.md](docs/day20-orchestration-mcp.md) - MCP Orchestration details
- [CLI_README.md](CLI_README.md) - This document
