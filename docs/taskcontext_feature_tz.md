# Техническое задание: TaskContext Feature

## Обзор

TaskContext Feature - новый функционал для мини-чата с RAG + памятью задач. Отслеживает контекст задачи в рамках диалоговой сессии, позволяя ассистенту помнить цель, ограничения и уточнения пользователя.

## Цели

1. **Память задачи** - отслеживать что пользователь уже уточнил, какие ограничения/термины зафиксированы, какова цель диалога
2. **История RAG запросов** - хранить историю RAG запросов для анализа паттернов
3. **Контекстная sumaрризация** - формировать саммари контекста для оптимизации промптов

## Архитектура

### Структура Feature

```
features/taskcontext/
├── domain/
│   ├── model/
│   │   ├── TaskContext.kt
│   │   ├── RagQueryHistory.kt
│   │   └── ContextSummary.kt
│   ├── repository/
│   │   └── TaskContextRepository.kt
│   └── usecase/
│       ├── InitializeTaskContextUseCase.kt
│       ├── UpdateTaskContextFromMessageUseCase.kt
│       └── GetEnrichedPromptUseCase.kt
├── data/
│   ├── local/
│   │   ├── entity/
│   │   │   └── TaskContextEntity.kt
│   │   ├── dao/
│   │   │   └── TaskContextDao.kt
│   │   └── mapper/
│   │       └── TaskContextMapper.kt
│   └── repository/
│       └── TaskContextRepositoryImpl.kt
└── di/
    └── FeatureTaskContextModule.kt
```

## Domain Models

### 1. TaskContext

**Назначение:** Хранит контекст задачи в рамках сессии

```kotlin
package ru.agent.features.taskcontext.domain.model

data class TaskContext(
    val id: String,
    val sessionId: String,

    // Цель диалога - извлекается из первых сообщений
    val goal: String? = null,

    // Зафиксированные уточнения пользователя
    val clarifications: List<Clarification> = emptyList(),

    // Ограничения и термины, зафиксированные в диалоге
    val constraints: List<Constraint> = emptyList(),

    // История RAG запросов
    val ragQueries: List<RagQueryHistory> = emptyList(),

    // Текущая стадия диалога
    val stage: ContextStage = ContextStage.INITIALIZING,

    // Метаданные
    val createdAt: Long,
    val updatedAt: Long
)

data class Clarification(
    val id: String,
    val topic: String,           // О чем уточнение (например, "database", "api")
    val clarification: String,   // Суть уточнения
    val timestamp: Long
)

data class Constraint(
    val id: String,
    val type: ConstraintType,    // TECHNOLOGY, STYLE, ARCHITECTURE, BUSINESS
    val description: String,
    val timestamp: Long
)

enum class ConstraintType {
    TECHNOLOGY,      // Kotlin, Room, Koin
    STYLE,           // Code style preferences
    ARCHITECTURE,    // MVVM, Clean Architecture
    BUSINESS         // Business rules
}

enum class ContextStage {
    INITIALIZING,    // Начало диалога
    EXPLORING,       // Сбор информации
    IMPLEMENTING,    // Реализация
    REVIEWING,       // Проверка результатов
    COMPLETED        // Завершен
}
```

### 2. RagQueryHistory

**Назначение:** Хранит историю RAG запросов для анализа

```kotlin
package ru.agent.features.taskcontext.domain.model

data class RagQueryHistory(
    val id: String,
    val taskContextId: String,
    val query: String,
    val timestamp: Long,

    // Результаты поиска
    val chunksFound: Int,
    val maxSimilarity: Float,
    val topSources: List<String>,  // Имена топ файлов

    // Метрики качества
    val wasHelpful: Boolean?,       // Был ли полезен (оценка пользователя или LLM)
    val relevanceScore: Float?,     // Оценка релевантности

    // Контекст запроса
    val queryType: QueryType
)

enum class QueryType {
    EXPLORATORY,     // Исследовательский вопрос
    SPECIFIC,        // Конкретный вопрос по коду
    IMPLEMENTATION,  // Вопрос по реализации
    DEBUGGING,       // Поиск багов
    REFACTORING      // Рефакторинг
}
```

### 3. ContextSummary

**Назначение:** Суммаризированный контекст для оптимизации промптов

```kotlin
package ru.agent.features.taskcontext.domain.model

data class ContextSummary(
    val taskContextId: String,

    // Краткое описание цели (1-2 предложения)
    val goalSummary: String,

    // Ключевые уточнения (bullet points)
    val keyClarifications: List<String>,

    // Ключевые ограничения (bullet points)
    val keyConstraints: List<String>,

    // Предпочтения стиля
    val stylePreferences: StylePreferences,

    // Оценка количества токенов
    val estimatedTokens: Int,

    // Время последнего обновления
    val lastUpdated: Long
)

data class StylePreferences(
    val language: String = "Kotlin",
    val codeStyle: CodeStyle = CodeStyle(),
    val verbosity: ResponseVerbosity = ResponseVerbosity.MEDIUM
)

data class CodeStyle(
    val indentSize: Int = 4,
    val maxLineLength: Int = 120,
    val namingConvention: NamingConvention = NamingConvention.CAMEL_CASE
)

enum class NamingConvention {
    CAMEL_CASE,
    SNAKE_CASE,
    PASCAL_CASE
}

enum class ResponseVerbosity {
    CONCISE,
    MEDIUM,
    VERBOSE
}
```

## Data Layer

### Room Entity

```kotlin
package ru.agent.features.taskcontext.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.agent.features.chat.data.local.entity.ChatSessionEntity

@Entity(
    tableName = "task_context",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"], unique = true),
        Index(value = ["stage"])
    ]
)
data class TaskContextEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val goal: String?,
    val clarifications: String,  // JSON
    val constraints: String,      // JSON
    val ragQueries: String,       // JSON
    val stage: String,            // ContextStage name
    val createdAt: Long,
    val updatedAt: Long
)
```

### DAO

```kotlin
package ru.agent.features.taskcontext.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskContextDao {

    @Query("SELECT * FROM task_context WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getBySessionId(sessionId: String): TaskContextEntity?

    @Query("SELECT * FROM task_context WHERE sessionId = :sessionId LIMIT 1")
    fun observeBySessionId(sessionId: String): Flow<TaskContextEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(taskContext: TaskContextEntity)

    @Update
    suspend fun update(taskContext: TaskContextEntity)

    @Query("DELETE FROM task_context WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: String)

    @Query("DELETE FROM task_context WHERE id = :id")
    suspend fun deleteById(id: String)
}
```

### Mapper

```kotlin
package ru.agent.features.taskcontext.data.local.mapper

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.agent.features.taskcontext.data.local.entity.TaskContextEntity
import ru.agent.features.taskcontext.domain.model.TaskContext

class TaskContextMapper(
    private val json: Json
) {

    fun toDomain(entity: TaskContextEntity): TaskContext {
        return TaskContext(
            id = entity.id,
            sessionId = entity.sessionId,
            goal = entity.goal,
            clarifications = decodeList(entity.clarifications, Clarification.serializer()),
            constraints = decodeList(entity.constraints, Constraint.serializer()),
            ragQueries = decodeList(entity.ragQueries, RagQueryHistory.serializer()),
            stage = ContextStage.valueOf(entity.stage),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(domain: TaskContext): TaskContextEntity {
        return TaskContextEntity(
            id = domain.id,
            sessionId = domain.sessionId,
            goal = domain.goal,
            clarifications = encodeList(domain.clarifications, Clarification.serializer()),
            constraints = encodeList(domain.constraints, Constraint.serializer()),
            ragQueries = encodeList(domain.ragQueries, RagQueryHistory.serializer()),
            stage = domain.stage.name,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }

    private inline fun <reified T> encodeList(
        list: List<T>,
        serializer: kotlinx.serialization.serializer<T>
    ): String {
        return json.encodeToString(list)
    }

    private inline fun <reified T> decodeList(
        json: String,
        serializer: kotlinx.serialization.serializer<T>
    ): List<T> {
        return try {
            json.decodeFromString(serializer, json)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
```

## Domain Layer

### Repository

```kotlin
package ru.agent.features.taskcontext.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.agent.features.taskcontext.domain.model.TaskContext

interface TaskContextRepository {

    suspend fun getBySessionId(sessionId: String): TaskContext?

    fun observeBySessionId(sessionId: String): Flow<TaskContext?>

    suspend fun insert(taskContext: TaskContext)

    suspend fun update(taskContext: TaskContext)

    suspend fun deleteBySessionId(sessionId: String)
}
```

### Use Cases

#### 1. InitializeTaskContextUseCase

```kotlin
package ru.agent.features.taskcontext.domain.usecase

class InitializeTaskContextUseCase(
    private val taskContextRepository: TaskContextRepository
) {
    suspend operator fun invoke(sessionId: String): TaskContext {
        // Проверяем существующий контекст
        val existing = taskContextRepository.getBySessionId(sessionId)
        if (existing != null) {
            return existing
        }

        // Создаем новый контекст
        val newContext = TaskContext(
            id = generateId(),
            sessionId = sessionId,
            stage = ContextStage.INITIALIZING,
            createdAt = currentTimeMillis(),
            updatedAt = currentTimeMillis()
        )

        taskContextRepository.insert(newContext)
        return newContext
    }
}
```

#### 2. UpdateTaskContextFromMessageUseCase

```kotlin
package ru.agent.features.taskcontext.domain.usecase

class UpdateTaskContextFromMessageUseCase(
    private val taskContextRepository: TaskContextRepository,
    private val llmAnalyzer: LLMContextAnalyzer
) {
    suspend operator fun invoke(
        sessionId: String,
        message: String,
        ragResponse: RagResponse?
    ): TaskContext {
        // Получаем текущий контекст
        val context = taskContextRepository.getBySessionId(sessionId)
            ?: throw IllegalStateException("TaskContext not initialized")

        // Анализируем сообщение с помощью LLM
        val analysis = llmAnalyzer.analyzeMessage(message, context)

        // Обновляем контекст
        val updatedContext = context.copy(
            goal = analysis.extractedGoal ?: context.goal,
            clarifications = context.clarifications + analysis.newClarifications,
            constraints = context.constraints + analysis.newConstraints,
            ragQueries = context.ragQueries + createRagQueryHistory(ragResponse),
            stage = analysis.detectedStage,
            updatedAt = currentTimeMillis()
        )

        taskContextRepository.update(updatedContext)
        return updatedContext
    }
}
```

#### 3. GetEnrichedPromptUseCase

```kotlin
package ru.agent.features.taskcontext.domain.usecase

class GetEnrichedPromptUseCase(
    private val taskContextRepository: TaskContextRepository,
    private val getMemoryContextUseCase: GetMemoryContextUseCase
) {
    suspend operator fun invoke(
        sessionId: String,
        userMessage: String
    ): EnrichedPrompt {
        // Получаем TaskContext
        val taskContext = taskContextRepository.getBySessionId(sessionId)

        // Получаем MemoryContext
        val memoryContext = getMemoryContextUseCase(sessionId)

        // Формируем обогащенный промпт
        return EnrichedPrompt(
            systemPrompt = buildSystemPrompt(taskContext, memoryContext),
            conversationHistory = memoryContext.toConversationContext(),
            taskContext = buildTaskContextSection(taskContext),
            ragContext = memoryContext.ragResponse,
            userMessage = userMessage
        )
    }

    private fun buildSystemPrompt(
        taskContext: TaskContext?,
        memoryContext: MemoryContext
    ): String {
        // Build system prompt combining all contexts
    }
}
```

## CLI Integration

### MiniChatCommand

```kotlin
package ru.agent.cli.commands

class MiniChatCommand : CliktCommand(
    name = "minichat",
    help = "Start mini-chat with RAG and task memory"
) {
    override fun run() {
        val controller = MiniChatController(
            initializeTaskContextUseCase = get(),
            updateTaskContextFromMessageUseCase = get(),
            getEnrichedPromptUseCase = get(),
            sendMessageUseCase = get(),
            ragSearchService = get()
        )

        controller.start()
    }
}
```

## Интеграция с существующими features

### RAG Integration
- Использует `RagSearchService` для поиска
- Сохраняет историю RAG запросов в `RagQueryHistory`
- Использует `RagResponse` для отслеживания источников

### Chat Integration
- Работает с `ChatSession` для управления сессиями
- Использует `Message` для истории диалога
- Интегрируется с `SendMessageUseCase`

### Memory Integration
- Использует `GetMemoryContextUseCase` для получения контекста памяти
- Дополняет `MemoryContext` своими данными
- `TaskContext` хранится в БД аналогично `WorkingMemory`

## Koin DI Module

```kotlin
package ru.agent.features.taskcontext.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.agent.core.database.AppDatabase
import ru.agent.features.taskcontext.data.local.dao.TaskContextDao
import ru.agent.features.taskcontext.data.local.mapper.TaskContextMapper
import ru.agent.features.taskcontext.data.repository.TaskContextRepositoryImpl
import ru.agent.features.taskcontext.domain.repository.TaskContextRepository
import ru.agent.features.taskcontext.domain.usecase.InitializeTaskContextUseCase
import ru.agent.features.taskcontext.domain.usecase.UpdateTaskContextFromMessageUseCase
import ru.agent.features.taskcontext.domain.usecase.GetEnrichedPromptUseCase

val featureTaskContextModule = module {

    // DAO
    single<TaskContextDao> { get<AppDatabase>().getTaskContextDao() }

    // Mapper
    single<TaskContextMapper> { TaskContextMapper(get()) }

    // Repository
    singleOf(::TaskContextRepositoryImpl) bind TaskContextRepository::class

    // Use Cases
    singleOf(::InitializeTaskContextUseCase)
    singleOf(::UpdateTaskContextFromMessageUseCase)
    singleOf(::GetEnrichedPromptUseCase)
}
```

## Database Migration

Добавить в `AppDatabase`:

```kotlin
@Database(
    entities = [
        // ... existing entities
        TaskContextEntity::class
    ],
    version = 2  // Increment version
)
abstract class AppDatabase : RoomDatabase() {
    // ... existing DAOs
    abstract fun getTaskContextDao(): TaskContextDao
}
```

## Testing Strategy

### Unit Tests
- `TaskContextRepositoryImplTest`
- `InitializeTaskContextUseCaseTest`
- `UpdateTaskContextFromMessageUseCaseTest`
- `GetEnrichedPromptUseCaseTest`

### Integration Tests
- `TaskContextFeatureIntegrationTest` - тестирование всей feature
- `MiniChatControllerTest` - тестирование CLI контроллера

### E2E Tests
- Два длинных сценария по 10-15 сообщений каждый
- Проверка сохранения цели и контекста
- Проверка отображения источников

## Критерии приемки

1. ✅ TaskContext создается автоматически при старте мини-чата
2. ✅ Цель диалога извлекается из первых сообщений
3. ✅ Уточнения и ограничения фиксируются и сохраняются
4. ✅ История RAG запросов сохраняется
5. ✅ Ответы всегда включают источники
6. ✅ Ассистент не теряет цель в длинных диалогах
7. ✅ Контекст сохраняется между сообщениями
8. ✅ Команда `minichat` работает в CLI

## Estimated Effort

- **Domain Models**: 4 часа
- **Data Layer (Entity, DAO, Mapper, Repository)**: 6 часов
- **Use Cases**: 6 часов
- **DI Module**: 1 час
- **CLI Integration (MiniChatCommand, Controller)**: 8 часов
- **Tests**: 6 часов
- **Code Review & Fixes**: 4 часа

**Total**: ~35 часов
