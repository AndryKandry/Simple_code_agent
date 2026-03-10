# Simple Code Agent - CLI Version

**AI-powered coding assistant with Command Line Interface**

## Overview

Simple Code Agent теперь работает как CLI приложение (Command Line Interface), для работы в терминале. Приложение предоставляет:

- **REPL интерактивный режим** - полноценный терминальный интерфейс для общения с AI-агентом
- **Выполнение системных команд** - работа с файловой системой через whitelist
- **Управление чатом** - отправка сообщений AI-агенту (DeepSeek)
- **Профиль пользователя** - настройка персонализации
- **Система памяти** - управление контекстом и памятью агента
- **Task State Machine** - управление задачами

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
- `/help` - показать справку
- `/exit` или `Ctrl+D` - выход

## Безопасность Shell

### Whitelist разрешенных команд:
```
ls, dir, pwd, cd, tree, cat, head, tail, less, more,
find, locate, whereis, which, grep, egrep, fgrep, rg,
stat, file, du, df, ps, top, htop,
whoami, hostname, uname, date, echo
```

### Blacklist опасных паттернов:
- `rm -rf` - рекурсивное удаление
- `sudo` - выполнение от имени суперпользователя
- `chmod 777` - небезопасные права
- `mkfs` - форматирование диска
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
