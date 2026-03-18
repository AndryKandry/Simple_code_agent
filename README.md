# Simple Code Agent

**AI-powered coding assistant with CLI interface, MCP integration, and orchestration capabilities**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.20-blue.svg)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.9.0-purple.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![MCP](https://img.shields.io/badge/MCP-0.4.0-green.svg)](https://modelcontextprotocol.io)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## Overview

Simple Code Agent — это AI-ассистент для разработки с мощным CLI интерфейсом, построенный на Clean Architecture с поддержкой Multiplatform (JVM, Android, iOS).

### Key Features

- **CLI First** — полноценный терминальный интерфейс с REPL режимом
- **MCP Protocol** — интеграция с Model Context Protocol для работы с инструментами
- **Orchestration** — LLM-управляемое планирование и выполнение задач
- **Memory System** — трехуровневая система памяти (working/short/long-term)
- **Task State Machine** — контролируемые переходы состояний с инвариантами
- **Scheduler** — cron-based планировщик для фоновых задач
- **Multiplatform** — общий код для JVM/Android/iOS

---

## Table of Contents

- [Quick Start](#quick-start)
- [Installation](#installation)
- [Usage](#usage)
- [Architecture](#architecture)
- [MCP Integration](#mcp-integration)
- [CLI Commands](#cli-commands)
- [Agent System](#agent-system)
- [Security](#security)
- [Development](#development)
- [Contributing](#contributing)

---

## Quick Start

```bash
# Clone repository
git clone https://github.com/yourusername/Simple_code_agent.git
cd Simple_code_agent

# Run interactive REPL mode
./agent

# Or via Gradle (always use --no-configuration-cache!)
./gradlew :composeApp:run --no-configuration-cache
```

---

## Installation

### Prerequisites

- JDK 17+
- Gradle 8.x

### Setup

```bash
# Set DeepSeek API key
export DEEPSEEK_API_KEY=your_api_key_here

# Build project
./gradlew build

# Run CLI
./agent
```

### Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `DEEPSEEK_API_KEY` | API key for DeepSeek LLM | Yes |
| `MCP_ALLOWED_ROOTS` | Allowed directories for filesystem MCP (comma-separated) | No |

---

## Usage

### CLI Modes

#### 1. Interactive REPL (Recommended)

```bash
./agent
```

```
╭──────────────────────────────────────────╮
│        Simple Code Agent REPL            │
│  Type /help for commands, /exit to quit  │
╰──────────────────────────────────────────╯

> Привет, помоги с кодом
[Agent] Привет! Я готов помочь. Что именно нужно?

> /shell ls -la
[OUTPUT]
total 48
drwxr-xr-x  12 user  staff   384 Jan 15 10:23 .
-rw-r--r--   1 user  staff  1234 Jan 15 10:20 build.gradle.kts

> /mcp tools
[TOOLS] filesystem: read_file, write_file, list_directory...
        terminal: execute_command, run_shell_script...

> /exit
Goodbye!
```

#### 2. Direct Commands

```bash
# Chat
./agent chat send "Привет, как дела?"

# Profile management
./agent profile show
./agent profile update --name "Andrey" --email "user@example.com"

# Memory operations
./agent memory list
./agent memory clear

# Task management
./agent task list
./agent task create "Новая задача"

# Shell commands
./agent shell ls -la
./agent shell git status

# MCP operations
./agent mcp list
./agent mcp tools
./agent mcp read README.md
```

### REPL Commands

| Command | Description |
|---------|-------------|
| `/chat <message>` | Send message to AI |
| `/profile [show\|update]` | Manage user profile |
| `/memory [list\|clear]` | Manage agent memory |
| `/task [list\|create\|status]` | Manage tasks |
| `/shell <command>` | Execute shell command |
| `/mcp [list\|tools\|exec]` | MCP operations |
| `/scheduler [list\|status]` | Scheduler operations |
| `/invariant [list\|toggle]` | Manage invariants |
| `/help` | Show help |
| `/exit` | Exit REPL |

---

## Architecture

### Clean Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Presentation Layer                            │
│  ┌──────────────────────┐    ┌───────────────────────────────┐  │
│  │    CLI (JVM only)    │    │   Compose UI (Multiplatform)  │  │
│  │  - CliApp.kt         │    │   - App.kt                    │  │
│  │  - Commands/         │    │   - Screens/                  │  │
│  │  - REPL/             │    │   - ViewModels/               │  │
│  └──────────────────────┘    └───────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Domain Layer                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │   Use Cases  │  │ Repositories │  │      Models          │  │
│  │  (business   │  │ (interfaces) │  │  - ChatSession       │  │
│  │   logic)     │  │              │  │  - Message           │  │
│  └──────────────┘  └──────────────┘  │  - TaskState         │  │
│                                      │  - Invariant         │  │
│                                      │  - ScheduledTask     │  │
│                                      └──────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Data Layer                                 │
│  ┌──────────────────┐  ┌───────────────────┐  ┌──────────────┐  │
│  │ Room Database    │  │ DeepSeek API      │  │ MCP Servers  │  │
│  │ - DAOs           │  │ - HTTP Client     │  │ - Filesystem │  │
│  │ - Entities       │  │ - Streaming       │  │ - Terminal   │  │
│  │ - Migrations     │  │ - Tool Calling    │  │ - Scheduler  │  │
│  └──────────────────┘  └───────────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Project Structure

```
composeApp/src/
├── commonMain/kotlin/ru/agent/
│   ├── App.kt                      # Compose entry point
│   ├── core/
│   │   ├── database/               # Room database
│   │   ├── network/                # HTTP client
│   │   └── di/                     # Koin modules
│   ├── features/
│   │   ├── chat/                   # Chat feature
│   │   │   ├── domain/             # Use cases, repository interface
│   │   │   ├── data/               # Repository implementation
│   │   │   └── presentation/       # ViewModels, Screens
│   │   ├── memory/                 # Memory system
│   │   ├── task/                   # Task management
│   │   ├── invariant/              # Invariant validation
│   │   ├── scheduler/              # Task scheduler
│   │   ├── profile/                # User profile
│   │   └── main/                   # Main screen
│   ├── design/                     # UI components
│   └── navigation/                 # Navigation
│
├── jvmMain/kotlin/
│   ├── main.kt                     # CLI entry point
│   └── ru/agent/
│       ├── cli/                    # CLI implementation
│       │   ├── CliApp.kt
│       │   ├── commands/           # CLIkt commands
│       │   ├── repl/               # REPL controller
│       │   ├── formatters/         # Output formatting
│       │   └── visualization/      # Terminal UI
│       ├── mcp/                    # MCP implementation
│       │   ├── server/             # Built-in servers
│       │   ├── client/             # MCP client
│       │   ├── manager/            # Server manager
│       │   └── orchestration/      # Orchestration layer
│       └── scheduler/              # Scheduler engine
│
├── androidMain/kotlin/             # Android-specific
└── iosMain/kotlin/                 # iOS-specific
```

### Feature Modules

| Module | Description | Components |
|--------|-------------|------------|
| **Chat** | AI chat functionality | `ChatRepository`, `SendMessageUseCase`, `ChatViewModel` |
| **Memory** | Three-level memory system | `WorkingMemory`, `ShortTermMemory`, `LongTermMemory` |
| **Task** | Task state management | `TaskStateMachine`, `TaskStateDao` |
| **Invariant** | State validation | `ValidationService`, `InvariantDao` |
| **Scheduler** | Background tasks | `SchedulerEngine`, `CronParser`, `TaskExecutor` |
| **Profile** | User personalization | `UserProfile`, preferences |

---

## MCP Integration

### Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    McpOrchestrator                               │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  analyzeAndPlan() → executeWithDependencies()              │ │
│  │  - Request classification (rule-based)                     │ │
│  │  - LLM-driven planning (DeepSeek)                          │ │
│  │  - Parallel execution with dependencies                    │ │
│  └────────────────────────────────────────────────────────────┘ │
│                              │                                   │
│           ┌──────────────────┼──────────────────┐               │
│           ▼                  ▼                  ▼               │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐     │
│  │ Filesystem MCP │  │ Terminal MCP   │  │ Scheduler MCP  │     │
│  │ Server         │  │ Server         │  │ Server         │     │
│  │ (Built-in)     │  │ (Built-in)     │  │ (Built-in)     │     │
│  └────────────────┘  └────────────────┘  └────────────────┘     │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                   McpClient (External)                     │  │
│  │  - GitHub MCP Server                                       │  │
│  │  - PostgreSQL MCP Server                                   │  │
│  │  - Custom MCP servers                                      │  │
│  └────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Built-in MCP Servers

#### Filesystem MCP Server

| Tool | Description | Parameters |
|------|-------------|------------|
| `read_file` | Read file contents | `path` (required) |
| `write_file` | Write to file | `path`, `content` (required) |
| `list_directory` | List directory contents | `path` (required) |
| `search_files` | Search files by pattern | `path`, `pattern` (required) |
| `delete_file` | Delete file (with confirmation) | `path`, `confirm=true` |
| `create_directory` | Create directory | `path` (required) |
| `file_exists` | Check file existence | `path` (required) |
| `copy_file` | Copy file | `source`, `destination` |

#### Terminal MCP Server

| Tool | Description | Parameters |
|------|-------------|------------|
| `execute_command` | Execute shell command | `command`, `timeout`, `working_dir` |
| `run_shell_script` | Run shell script | `script_path`, `args`, `timeout` |
| `check_command_available` | Check command availability | `command` |
| `list_allowed_commands` | List allowed commands | - |
| `get_environment_info` | Get environment info | - |

#### Scheduler MCP Server

| Tool | Description |
|------|-------------|
| `schedule_reminder` | Create reminder |
| `schedule_command` | Schedule command |
| `schedule_mcp_tool` | Schedule MCP tool execution |
| `list_scheduled_tasks` | List scheduled tasks |
| `get_task` | Get task details |
| `cancel_task` | Cancel task |
| `pause_task` / `resume_task` | Pause/resume task |
| `get_task_history` | Get execution history |

### MCP CLI Commands

```bash
# List available servers
./agent mcp list

# Show MCP status
./agent mcp status

# List all tools
./agent mcp tools
./agent mcp tools -s filesystem  # Specific server
./agent mcp tools -v             # Verbose

# Quick operations
./agent mcp read /path/to/file.txt
./agent mcp write /path/to/file.txt "Content"
./agent mcp run git status

# Execute tools
./agent mcp exec filesystem:read_file '{"path": "/file.txt"}'
./agent mcp exec terminal:execute_command '{"command": "ls -la"}'

# Connect to external servers
./agent mcp connect github --url http://localhost:3000/mcp
./agent mcp disconnect github
```

### Orchestration

MCP Orchestration обеспечивает:

1. **Request Classification** — автоматическое определение типа запроса
2. **LLM Planning** — генерация плана выполнения через DeepSeek
3. **Parallel Execution** — параллельное выполнение независимых инструментов
4. **Dependency Management** — управление зависимостями между шагами

```
Request → Classification → Planning → Parallel Execution → Response
           (rule-based)    (LLM)       (with deps)
```

---

## CLI Commands

### Chat Commands

```bash
# Send message
./agent chat send "Привет"

# Show history
./agent chat history

# Clear session
./agent chat clear
```

### Profile Commands

```bash
# Show profile
./agent profile show

# Update profile
./agent profile update --name "Andrey" --email "user@example.com"
```

### Task Commands

```bash
# List tasks
./agent task list

# Create task
./agent task create "Implement feature X"

# Show task status
./agent task status <task-id>

# Update task state
./agent task update <task-id> --state IN_PROGRESS
```

### Scheduler Commands

```bash
# List scheduled tasks
./agent scheduler list

# Schedule reminder
./agent scheduler remind "Meeting in 5 min" --in 5m

# Schedule command
./agent scheduler command "./gradlew build" --cron "0 9 * * 1-5"

# Show scheduler status
./agent scheduler status
```

### Invariant Commands

```bash
# List invariants
./agent invariant list

# Toggle invariant
./agent invariant toggle <invariant-id>

# Validate state
./agent invariant validate <task-id>
```

---

## Agent System

Project включает 15+ специализированных AI-агентов (`.claude/agents/`):

### Development Agents

| Agent | Description |
|-------|-------------|
| `orchestrator-agent` | Главный оркестратор разработки |
| `compose-desktop-developer-agent` | Compose Multiplatform разработка |
| `code-agent-developer-agent` | General code development |
| `room-database-agent` | Room database specialist |
| `koin-di-agent` | Koin DI configuration |
| `refactoring-agent` | Code refactoring |

### Research & Analysis Agents

| Agent | Description |
|-------|-------------|
| `researcher-agent` | Codebase research |
| `business-analyst-agent` | Business requirements analysis |
| `qa-expert-agent` | QA testing |

### MCP Agents

| Agent | Description |
|-------|-------------|
| `mcp-client-agent` | MCP client configuration |
| `mcp-filesystem-server-agent` | Filesystem MCP server |
| `mcp-terminal-server-agent` | Terminal MCP server |

### Quality Agents

| Agent | Description |
|-------|-------------|
| `code-reviewer-agent` | **MANDATORY** code review after development |

### Development Workflow

```
1. Business Analysis (optional)
   └─ business-analyst-agent → Technical specification

2. Development
   └─ developer-agent → Implementation

3. Code Review (MANDATORY!)
   └─ code-reviewer-agent → Quality report

4. Fixes
   └─ Fix critical/important issues

5. Re-review (optional)
   └─ code-reviewer-agent → Verify fixes
```

---

## Security

### Terminal Security

**Whitelist:**
```
ls, dir, pwd, cd, tree, cat, head, tail, less, more,
find, locate, whereis, which, grep, egrep, fgrep, rg,
stat, file, du, df, ps, top, htop,
whoami, hostname, uname, date, echo,
gradlew, gradle, mvn, npm, yarn, cargo, go, make,
python, python3, node, ruby, java, kotlin,
git, svn, hg, curl, wget, docker, kubectl
```

**Blacklist:**
- `rm -rf` — recursive deletion
- `sudo` — superuser execution
- `chmod 777` — insecure permissions
- `mkfs` — disk formatting
- Shell injection (`;`, `|`, `&`, `$()`)

### Filesystem Security

- **Allowed Roots** — only whitelisted directories
- **Path Validation** — path traversal protection
- **No Directory Deletion** — directories cannot be deleted
- **Confirmation Required** — file deletion requires confirmation

---

## Technology Stack

### Core

| Technology | Version | Purpose |
|------------|---------|---------|
| Kotlin | 2.2.20 | Primary language |
| Compose Multiplatform | 1.9.0 | UI framework |
| Koin | 4.1.1 | Dependency Injection |
| Room | 2.8.3 | Database |
| Ktor | 3.3.1 | HTTP client |

### CLI (JVM only)

| Technology | Version | Purpose |
|------------|---------|---------|
| CLIkt | 4.4.0 | CLI framework |
| Mordant | 2.2.0 | Terminal UI |
| JLine | 3.25.1 | Readline support |

### MCP

| Technology | Version | Purpose |
|------------|---------|---------|
| MCP SDK | 0.4.0 | Model Context Protocol |
| Cron Utils | 9.2.1 | Cron expressions |

---

## Development

### Building

```bash
# Compile
./gradlew compileKotlinJvm

# Run tests
./gradlew test

# Run CLI
./gradlew :composeApp:run --no-configuration-cache

# Build distribution
./gradlew :composeApp:packageDistributionForCurrentOS
```

### Database

**Current version:** 9

**Tables:**
- `chat_sessions` — chat sessions
- `messages` — messages
- `working_memory` — working memory
- `knowledge_entries` — knowledge base
- `user_profiles` — user profiles
- `context_anchors` — context anchors
- `task_states` — task states
- `invariants` — invariants
- `scheduled_tasks` — scheduled tasks
- `task_executions` — task execution history

### Project Stats

- **Kotlin files:** 206
- **Lines of code:** ~32,900
- **Feature modules:** 7
- **MCP servers:** 3 built-in + external
- **CLI commands:** 8 major commands
- **Agents:** 15+ specialized agents

---

## Platforms

| Platform | Support Level | Notes |
|----------|---------------|-------|
| macOS | Full | Terminal.app, iTerm2 |
| Linux | Full | gnome-terminal, konsole, xterm |
| Windows | Partial | PowerShell, CMD (limited ANSI) |
| Android | Full | Compose UI |
| iOS | Full | Compose UI |

---

## Troubleshooting

### Configuration Cache Error

```bash
# Always use this flag
./gradlew :composeApp:run --no-configuration-cache
```

### SLF4J Warnings

Can be safely ignored — doesn't affect functionality.

### Terminal Colors Not Working

Ensure your terminal supports ANSI colors:
- macOS: Terminal.app, iTerm2 (full support)
- Linux: gnome-terminal, konsole (full support)
- Windows: Use Windows Terminal for better support

---

## Roadmap

- [ ] RAG integration with Ollama
- [ ] External MCP server marketplace
- [ ] Web UI
- [ ] Plugin system
- [ ] Multi-agent orchestration

---

## Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Make changes following the architecture
4. Run code review with `code-reviewer-agent`
5. Commit changes (`git commit -m 'Add amazing feature'`)
6. Push to branch (`git push origin feature/amazing-feature`)
7. Open Pull Request

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Authors

**Simple Code Agent Team**

---

## Acknowledgments

- [Anthropic](https://www.anthropic.com) for Claude
- [DeepSeek](https://www.deepseek.com) for LLM API
- [JetBrains](https://www.jetbrains.com) for Kotlin and Compose Multiplatform
- [Model Context Protocol](https://modelcontextprotocol.io) for MCP specification
