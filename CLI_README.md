# Simple Code Agent - CLI Version

**AI-powered coding assistant with Command Line Interface**

## Overview

Simple Code Agent работает как CLI приложение (Command Line Interface) для работы в терминале. Приложение предоставляет:

- **REPL интерактивный режим** - полноценный терминальный интерфейс для общения с AI-агентом
- **Выполнение системных команд** - работа с файловой системой через whitelist
- **Управление чатом** - отправка сообщений AI-агенту (DeepSeek)
- **Профиль пользователя** - настройка персонализации
- **Система памяти** - управление контекстом и памятью агента
- **Task State Machine** - управление задачами
- **MCP (Model Context Protocol)** - интеграция с внешними инструментами и серверами

## Запуск

### Рекомендуемый способ (через скрипт):

```bash
# Интерактивный REPL режим (по умолчанию)
./agent

# Прямые команды
./agent chat send "Привет"
./agent profile show
./agent task list
./agent shell ls -la

# Справка
./agent --help
./agent chat --help
```

### Альтернативный способ (напрямую через Gradle):

```bash
# ВАЖНО: Всегда добавляйте --no-configuration-cache для избежания ошибок!

# Интерактивный REPL режим
./gradlew :composeApp:run --no-configuration-cache

# Прямые команды
./gradlew :composeApp:run --args="chat send \"Привет\"" --no-configuration-cache
./gradlew :composeApp:run --args="profile show" --no-configuration-cache
```

### Команды

#### Основные команды:

```bash
# Показать справку
./gradlew :composeApp:run --args="--help"

# Chat команды
./gradlew :composeApp:run --args="chat --help"
./gradlew :composeApp:run --args="chat send \"Твое сообщение\""
./gradlew :composeApp:run --args="chat history"

# Profile команды
./gradlew :composeApp:run --args="profile show"
./gradlew :composeApp:run --args="profile update --name \"Имя\" --email \"email@example.com\""

# Memory команды
./gradlew :composeApp:run --args="memory list"
./gradlew :composeApp:run --args="memory clear"

# Task команды
./gradlew :composeApp:run --args="task list"
./gradlew :composeApp:run --args="task status <task-id>"
./gradlew :composeApp:run --args="task create \"Название задачи\""

# Shell команды
./gradlew :composeApp:run --args="shell ls -la"
./gradlew :composeApp:run --args="shell find . -name \"*.kt\""
./gradlew :composeApp:run --args="shell cat README.md"

# MCP команды
./gradlew :composeApp:run --args="mcp list"
./gradlew :composeApp:run --args="mcp status"
./gradlew :composeApp:run --args="mcp tools"
./gradlew :composeApp:run --args="mcp read README.md"
```

## REPL интерактивный режим

При запуске без аргументов открывается REPL режим:

```
> Привет, помоги с кодом
[Agent] Привет! Я готов помочь. Что именно нужно?
> /shell ls -la
[OUTPUT]
total 48
drwxr-xr-x  12 user  staff   384 Jan 15 10:23 .
-rw-r--r--   1 user  staff  1234 Jan 15 10:20 build.gradle.kts
...
> /profile show
[PROFILE]
Name: Andrey
Email: user@example.com
> exit
```

### REPL команды:
- `/chat <message>` - отправить сообщение
- `/profile [show|update]` - управление профилем
- `/memory [list|clear]` - управление памятью
- `/task [list|create|status]` - управление задачами
- `/shell <command>` - выполнить системную команду
- `/mcp [list|status|tools|...]` - MCP команды
- `/help` - показать справку
- `/exit` или `Ctrl+D` - выход

## MCP (Model Context Protocol)

MCP - это протокол для подключения AI-агентов к внешним инструментам и серверам.

### Встроенные серверы:

| Server | Description |
|--------|-------------|
| `filesystem` | File operations (read, write, list, search) |
| `terminal` | Command execution (safe commands only) |

### MCP команды:

```bash
# Список доступных серверов
./agent mcp list

# Статус MCP инфраструктуры
./agent mcp status

# Список всех инструментов
./agent mcp tools

# Инструменты конкретного сервера
./agent mcp tools -s filesystem
./agent mcp tools -s terminal

# Быстрое чтение файла
./agent mcp read /path/to/file.txt

# Быстрая запись файла
./agent mcp write /path/to/file.txt "Content"

# Выполнение команды
./agent mcp run git status
./agent mcp run --timeout 60000 npm test

# Выполнение инструмента
./agent mcp exec filesystem:list_directory '{"path": "/project"}'
./agent mcp exec filesystem:search_files '{"path": "/project", "pattern": "*.kt"}'
./agent mcp exec terminal:execute_command '{"command": "ls -la"}'
./agent mcp exec terminal:list_allowed_commands

# Подключение к внешнему серверу
./agent mcp connect github --url http://localhost:3000/mcp
./agent mcp connect github  # использует preset

# Отключение
./agent mcp disconnect github
./agent mcp disconnect --all
```

### Filesystem MCP Tools:

| Tool | Description |
|------|-------------|
| `read_file` | Чтение файла |
| `write_file` | Запись файла |
| `list_directory` | Листинг директории |
| `search_files` | Поиск файлов по паттерну |
| `delete_file` | Удаление файла (требует подтверждения) |
| `create_directory` | Создание директории |
| `file_exists` | Проверка существования |
| `copy_file` | Копирование файла |

### Terminal MCP Tools:

| Tool | Description |
|------|-------------|
| `execute_command` | Выполнение команды |
| `run_shell_script` | Запуск shell скрипта |
| `check_command_available` | Проверка доступности команды |
| `list_allowed_commands` | Список разрешенных команд |
| `get_environment_info` | Информация об окружении |

### Внешние MCP серверы:

```bash
# GitHub MCP Server
npx -y @modelcontextprotocol/server-github

# PostgreSQL MCP Server
export POSTGRES_CONNECTION_STRING=postgresql://user:pass@localhost:5432/db
npx -y @modelcontextprotocol/server-postgres

# Memory MCP Server
npx -y @modelcontextprotocol/server-memory
```

## MCP (Model Context Protocol)

### Обзор MCP

MCP (Model Context Protocol) - это стандартизированный способ подключения AI-агентов к внешним инструментам и ресурсам.

### Архитектура MCP

```
┌─────────────────────────────────────────────────────────────┐
│                    McpManager (Unified API)                  │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │              getAllAvailableTools()                      │ │
│  │              executeToolByFullName()                     │ │
│  └─────────────────────────────────────────────────────────┘ │
│                              │                               │
│           ┌──────────────────┼──────────────────┐           │
│           ▼                  ▼                  ▼           │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐ │
│  │ FilesystemMcp  │  │ TerminalMcp    │  │ McpClient      │ │
│  │ Server         │  │ Server         │  │                │ │
│  │ (Built-in)     │  │ (Built-in)     │  │ (External)     │ │
│  │                │  │                │  │                │ │
│  │ - read_file    │  │ - execute_cmd  │  │ - HTTP/SSE     │ │
│  │ - write_file   │  │ - run_script   │  │ - GitHub       │ │
│  │ - list_dir     │  │ - check_cmd    │  │ - PostgreSQL   │ │
│  │ - search_files │  │ - list_allowed │  │ - Memory       │ │
│  └────────────────┘  └────────────────┘  └────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### CLI команды MCP

```bash
# Список доступных серверов
./agent mcp list

# Статус MCP инфраструктуры
./agent mcp status

# Список всех инструментов
./agent mcp tools
./agent mcp tools -s filesystem  # Только filesystem
./agent mcp tools -v             # Подробно

# Быстрые команды
./agent mcp read /path/to/file.txt
./agent mcp write /path/to/file.txt "Content"
./agent mcp run git status

# Выполнение инструментов
./agent mcp exec filesystem:read_file '{"path": "/file.txt"}'
./agent mcp exec filesystem:write_file '{"path": "/file.txt", "content": "Hello"}'
./agent mcp exec filesystem:list_directory '{"path": "/project"}'
./agent mcp exec filesystem:search_files '{"path": "/project", "pattern": "*.kt"}'
./agent mcp exec terminal:execute_command '{"command": "ls -la"}'
./agent mcp exec terminal:list_allowed_commands

# Подключение к внешним серверам
./agent mcp connect github --url http://localhost:3000/mcp
./agent mcp connect custom --url http://localhost:4000/mcp

# Отключение
./agent mcp disconnect github
./agent mcp disconnect --all
```

### Built-in MCP Servers

#### Filesystem MCP Server

| Tool | Description | Parameters |
|------|-------------|------------|
| `read_file` | Читает файл | `path` (required) |
| `write_file` | Записывает в файл | `path`, `content` (required) |
| `list_directory` | Листинг директории | `path` (required) |
| `search_files` | Поиск файлов | `path`, `pattern` (required) |
| `delete_file` | Удаляет файл | `path`, `confirm=true` (required) |
| `create_directory` | Создает директорию | `path` (required) |
| `file_exists` | Проверяет существование | `path` (required) |
| `copy_file` | Копирует файл | `source`, `destination` (required) |

#### Terminal MCP Server

| Tool | Description | Parameters |
|------|-------------|------------|
| `execute_command` | Выполняет команду | `command`, `timeout`, `working_dir` |
| `run_shell_script` | Запускает скрипт | `script_path`, `args`, `timeout` |
| `check_command_available` | Проверяет команду | `command` |
| `list_allowed_commands` | Список разрешенных | - |
| `get_environment_info` | Информация об окружении | - |

### MCP Безопасность

#### Filesystem Security:
- **Allowed Roots** - доступ только к разрешенным директориям
- **Path Validation** - защита от path traversal атак
- **No Directory Deletion** - запрет удаления директорий
- **Confirmation Required** - подтверждение для удаления файлов

#### Terminal Security:
- **Command Whitelist** - только разрешенные команды
- **Dangerous Patterns Blacklist** - блокировка rm -rf, sudo и т.д.
- **Timeout Protection** - автоматическое завершение по таймауту
- **Project-only Scripts** - скрипты только из директории проекта

### Запуск внешних MCP серверов

```bash
# GitHub MCP (требует GITHUB_TOKEN)
export GITHUB_TOKEN=ghp_xxx
npx -y @modelcontextprotocol/server-github

# PostgreSQL MCP
export POSTGRES_CONNECTION_STRING=postgresql://user:pass@localhost:5432/db
npx -y @modelcontextprotocol/server-postgres

# Memory MCP (knowledge graph)
npx -y @modelcontextprotocol/server-memory
```

### MCP Inspector (Debugging)

```bash
# Проверка Filesystem MCP
npx -y @modelcontextprotocol/inspector java -cp app.jar ru.agent.mcp.server.FilesystemMcpServerKt

# Проверка Terminal MCP
npx -y @modelcontextprotocol/inspector java -cp app.jar ru.agent.mcp.server.TerminalMcpServerKt
```

---

## Безопасность Shell

### Whitelist разрешенных команд:
```
ls, dir, pwd, cd, tree, cat, head, tail, less, more,
find, locate, whereis, which, grep, egrep, fgrep, rg,
stat, file, du, df, ps, top, htop,
whoami, hostname, uname, date, echo,
gradlew, gradle, mvn, npm, yarn, cargo, go, make,
python, python3, node, ruby, java, kotlin,
git, svn, hg,
curl, wget, docker, kubectl
```

### Blacklist опасных паттернов:
- `rm -rf` - рекурсивное удаление
- `sudo` - выполнение от имени суперпользователя
- `chmod 777` - небезопасные права
- `mkfs` - форматирование диска
- `curl | bash` - удаленное выполнение кода
- Shell injection символы (`;`, `|`, `&`, ```, `$()`)

## Архитектура

### Clean Architecture:

```
CLI Presentation Layer:
├── cli/
│   ├── CliApp.kt - Entry point
│   ├── commands/ - CLIkt команды
│   ├── repl/ - REPL контроллер
│   └── formatters/ - Форматирование вывода

Domain Layer (ПЕРЕИСПОЛЬЗОВАНИЕ):
├── features/
│   ├── chat/domain/
│   ├── profile/domain/
│   ├── memory/domain/
│   └── task/domain/

Data Layer (ПЕРЕИСПОЛЬЗОВАНИЕ):
├── core/database/
├── core/network/
└── features/*/data/
```

### Технологии:
- **CLIkt** - CLI фреймворк
- **Mordant** - терминальный UI (цвета, таблицы)
- **JLine** - readline функциональность (history, autocomplete)
- **Koin** - Dependency Injection
- **Room** - Database
- **Ktor** - HTTP клиент
- **Kotlin Coroutines** - асинхронность

## Миграция с Compose Desktop

### Что изменилось:
1. **Entry point** - `main.kt` теперь запускает CLI вместо Compose Window
2. **UI Layer** - Compose UI заменен на CLI presentation
3. **Domain/Data** - полностью переиспользованы без изменений

### Что сохранилось:
- Вся бизнес-логика (UseCases, Repositories)
- База данных (Room, DAOs, Entities)
- Сетевой слой (DeepSeek API клиент)
- DI конфигурация (Koin modules)

## Кроссплатформенность

CLI работает на:
- **macOS** - Terminal.app, iTerm2
- **Linux** - gnome-terminal, konsole, xterm
- **Windows** - PowerShell, CMD (с ограничениями ANSI colors)

## Development

### Сборка:
```bash
# Компиляция
./gradlew compileKotlinJvm

# Запуск
./gradlew :composeApp:run

# Создание native distribution (опционально)
./gradlew :composeApp:packageDistributionForCurrentOS
```

### Структура проекта:
```
composeApp/src/
├── commonMain/kotlin/ru/agent/
│   ├── features/ - бизнес-логика
│   └── core/ - инфраструктура
└── jvmMain/kotlin/
    ├── main.kt - CLI entry point
    └── ru/agent/cli/ - CLI presentation
```

## Troubleshooting

### Configuration Cache ошибка:
Если получаете ошибку configuration cache, используйте флаг:
```bash
./gradlew :composeApp:run --no-configuration-cache
```

### SLF4J warnings:
Предупреждения SLF4J можно игнорировать - они не влияют на работу приложения.

## License

MIT

## Authors

Simple Code Agent Team
