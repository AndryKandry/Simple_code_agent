# Ollama Quick Start

## Быстрый старт

### 1. Запуск с минимальными логами (рекомендуется)

```bash
./agent-ollama
```

Показывает только:
- Ответы агента
- Ошибки
- Статус операций

### 2. Запуск с отладочными логами

```bash
./agent-ollama-debug
```

Показывает подробные логи для troubleshooting.

### 3. Использование другой модели

```bash
OLLAMA_MODEL=qwen2.5-coder:3b-instruct ./agent-ollama
```

## Доступные скрипты

| Скрипт | Описание | Логи |
|--------|----------|-------|
| `./agent-ollama` | Обычный режим | Минимум |
| `./agent-ollama-debug` | Режим отладки | Подробно |

## Управление логами

### Через скрипты
- `./agent-ollama` - только ошибки
- `./agent-ollama-debug` - все логи

### Через переменные окружения
```bash
# Только ошибки
AGENT_LOG_LEVEL=Error ./gradlew :composeApp:runCli

# Предупреждения и выше
AGENT_LOG_LEVEL=Warn ./gradlew :composeApp:runCli

# Информационные логи
AGENT_LOG_LEVEL=Info ./gradlew :composeApp:runCli

# Отладочные логи
AGENT_LOG_LEVEL=Debug ./gradlew :composeApp:runCli

# Включить debug режим
AGENT_DEBUG=true ./gradlew :composeApp:runCli
```

## Рекомендуемые модели

| Модель | Скорость | Качество | Tools | Для чего |
|--------|----------|----------|-------|---------|
| `deepseek-r1:1.5b` | ⚡⚡⚡ | ⭐⭐⭐ | ❌ | Быстрый чат |
| `deepseek-r1:8b` | ⚡⚡ | ⭐⭐⭐⭐ | ❌ | Рассуждения |
| `qwen2.5-coder:3b` | ⚡⚡⚡ | ⭐⭐⭐ | ⚠️ | Код |
| `deepseek` (cloud) | ⚡⚡⚡⚡⚡ | ⭐⭐⭐⭐⭐ | ✅ | Максимум |

## Устранение проблем

### Ошибка: "does not support tools"
**Решение**: Автоматически исправляется! Приложение повторит запрос без tools.

### Ошибка: "Ollama server is not running"
**Решение**:
```bash
ollama serve
```

### Ошибка: "Model not found"
**Решение**:
```bash
ollama pull deepseek-r1:1.5b
```

### Медленные ответы
**Решение**: Используйте меньшую модель (`deepseek-r1:1.5b` вместо `deepseek-r1:8b`)
