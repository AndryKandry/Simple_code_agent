# Техническое задание: Context Management Strategies

## 1. Краткое описание

Система управления контекстом для Chat feature с тремя стратегиями: Sliding Window (хранение последних N сообщений), Sticky Facts (Key-Value Memory с извлечением важных данных из диалога) и Branching (создание веток диалога от checkpoint). Решает проблему ограничения контекстного окна LLM и позволяет сравнивать эффективность стратегий.

## 2. Функциональные требования

### 2.1 Основной функционал

- [ ] Стратегия 1: Sliding Window - хранить только последние N сообщений (по умолчанию 10)
- [ ] Стратегия 2: Sticky Facts - извлечение facts через LLM (goal, constraints, preferences, decisions, agreements)
- [ ] Стратегия 3: Branching - создание checkpoint и независимых веток диалога
- [ ] Переключение стратегий в UI (TopAppBar)
- [ ] Сохранение выбранной стратегии per-session
- [ ] Настройка параметров стратегии (N для sliding window)
- [ ] Режим сравнения: запуск 3 параллельных сессий с разными стратегиями
- [ ] Отображение текущих facts для Sticky Facts стратегии
- [ ] UI для создания checkpoint и переключения между ветками

### 2.2 Граничные случаи

- [ ] При переключении стратегии: показать предупреждение о сбросе контекста
- [ ] При превышении N=0: показать ошибку валидации
- [ ] При извлечении facts: обработать ошибку API (показать fallback)
- [ ] При создании checkpoint на пустом диалоге: показать warning
- [ ] При удалении checkpoint с дочерними ветками: cascade delete или запретить
- [ ] Обработка offline режима: использовать cached facts

## 3. Desktop Requirements

### 3.1 Keyboard Shortcuts

| Комбинация | Действие | Контекст |
|------------|----------|----------|
| Ctrl+Shift+S | Переключить стратегию | Глобальный |
| Ctrl+Shift+P | Создать checkpoint | Branching режим |
| Ctrl+Shift+B | Показать ветки | Branching режим |
| Ctrl+Shift+F | Показать facts | Sticky Facts режим |
| Escape | Закрыть панель настроек | Локальный |
| Ctrl+1/2/3 | Выбрать стратегию 1/2/3 | Локальный |
| Ctrl+Shift+C | Запустить сравнение | Глобальный |

### 3.2 Menu Structure

```
Файл
├── Новая сессия (Ctrl+N)
├── Сохранить checkpoint (Ctrl+Shift+P)
├── ─────────────
└── Экспорт сессии...

Редактирование
├── Очистить контекст
├── Сбросить facts
└── ─────────────

Контекст
├── Стратегия
│   ├── Sliding Window (Ctrl+1)
│   ├── Sticky Facts (Ctrl+2)
│   └── Branching (Ctrl+3)
├── ─────────────
├── Настройки стратегии... (Ctrl+Shift+,)
├── Показать facts (Ctrl+Shift+F)
├── Показать ветки (Ctrl+Shift+B)
└── Сравнить стратегии (Ctrl+Shift+C)

Вид
├── Показать sidebar
├── Показать статистику токенов
└── Компактный режим
```

### 3.3 Window Management

- Поведение при закрытии: Сохранить текущую стратегию и состояние
- Сохранение состояния: Размеры окна, последняя выбранная стратегия, collapsed/expanded состояние панелей

### 3.4 File Operations

- Открывает файлы: Нет
- Сохраняет файлы: Да - экспорт сессии в JSON формате (для сравнения и анализа)

## 4. UI Layout

### 4.1 Структура экрана (Стандартный режим)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Файл  Редактирование  Контекст  Вид          [_][□][×]                     │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │ 🗨️ Project Discussion        [Sliding Window ▼]  [Compare]  [⚙️]       ││
│  └─────────────────────────────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌──────────┐  ┌───────────────────────────────────────────────────────────┐│
│  │ Sessions │  │                                                           ││
│  │          │  │  ┌─────────────────────────────────────────────────────┐  ││
│  │  • Chat 1│  │  │ 👤 User: Мне нужно создать систему авторизации     │  ││
│  │  • Chat 2│  │  └─────────────────────────────────────────────────────┘  ││
│  │  + New   │  │  ┌─────────────────────────────────────────────────────┐  ││
│  │          │  │  │ 🤖 AI: Понял! Какой тип авторизации вам нужен...    │  ││
│  └──────────┘  │  └─────────────────────────────────────────────────────┘  ││
│                │                                                           ││
│                │  ┌─────────────────────────────────────────────────────┐  ││
│                │  │ 📊 Context: 8/10 msgs | ~1,200 tokens              │  ││
│                │  └─────────────────────────────────────────────────────┘  ││
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │ [📎] [Введите сообщение...                                    ] [Send] │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 Структура экрана (Sticky Facts режим)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Файл  Редактирование  Контекст  Вид          [_][□][×]                     │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │ 🗨️ Project Discussion        [Sticky Facts ▼]    [Compare]  [⚙️]       ││
│  └─────────────────────────────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌──────────┐  ┌───────────────────────────────────┐  ┌──────────────────┐ │
│  │ Sessions │  │                                   │  │ 📋 Facts         │ │
│  │          │  │  Chat Messages                    │  │                  │ │
│  │  • Chat 1│  │                                   │  │ 🎯 Goal:         │ │
│  │  • Chat 2│  │  [Message bubbles...]             │  │ Auth system      │ │
│  │  + New   │  │                                   │  │                  │ │
│  │          │  │                                   │  │ ⚠️ Constraints:  │ │
│  └──────────┘  │                                   │  │ • JWT tokens     │ │
│                │                                   │  │ • OAuth2         │ │
│                │                                   │  │                  │ │
│                │                                   │  │ 👤 Preferences:  │ │
│                │                                   │  │ React frontend   │ │
│                │                                   │  │                  │ │
│                │                                   │  │ ✅ Decisions:    │ │
│                │                                   │  │ Use PostgreSQL   │ │
│                │                                   │  │                  │ │
│                │                                   │  │ 📝 Agreements:   │ │
│                │                                   │  │ Weekly reviews   │ │
│                │                                   │  │                  │ │
│                │                                   │  │ [Edit] [Clear]   │ │
│                └───────────────────────────────────┘  └──────────────────┘ │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │ [📎] [Введите сообщение...                                    ] [Send] │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.3 Структура экрана (Branching режим)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Файл  Редактирование  Контекст  Вид          [_][□][×]                     │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │ 🗨️ Project Discussion        [Branching ▼]       [Compare]  [⚙️]       ││
│  └─────────────────────────────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌──────────┐  ┌───────────────────────────────────────────────────────────┐│
│  │ Sessions │  │  🌳 Branches                              [+Checkpoint]  ││
│  │          │  │  ┌────────────────────────────────────────────────────┐   ││
│  │  • Chat 1│  │  │ 📍 Initial (main)              [📍] 2 branches    │   ││
│  │  • Chat 2│  │  │   ├── After requirements       [📍] 1 branch     │   ││
│  │  + New   │  │  │   │   └── Current ★                               │   ││
│  │          │  │  │   └── Alternative approach                       │   ││
│  └──────────┘  │  └────────────────────────────────────────────────────┘   ││
│                │                                                           ││
│                │  Chat Messages (Current branch: "After requirements")     ││
│                │  [Message bubbles...]                                     ││
│                │                                                           ││
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │ [📎] [Введите сообщение...                                    ] [Send] │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.4 Структура экрана (Comparison Mode)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Файл  Редактирование  Контекст  Вид          [_][□][×]                     │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │ 📊 Comparison Mode                        [Stop Comparison]  [Export]   ││
│  └─────────────────────────────────────────────────────────────────────────┘│
├──────────────────────────┬──────────────────────────┬───────────────────────┤
│  Sliding Window          │  Sticky Facts            │  Branching            │
│  (N=10)                  │                          │                       │
├──────────────────────────┼──────────────────────────┼───────────────────────┤
│                          │                          │                       │
│  [Messages...]           │  [Messages...]           │  [Messages...]        │
│                          │                          │                       │
│                          │  Facts:                  │                       │
│                          │  🎯 Auth system          │                       │
│                          │                          │                       │
├──────────────────────────┼──────────────────────────┼───────────────────────┤
│  📊 Tokens: 1,234        │  📊 Tokens: 987          │  📊 Tokens: 1,456     │
│  📝 Messages: 8          │  📝 Messages: 8          │  📝 Messages: 8       │
│  ⚡ Response: 2.1s       │  ⚡ Response: 2.8s       │  ⚡ Response: 2.1s    │
│  ✓ Quality: Good         │  ✓ Quality: Excellent    │  ✓ Quality: Good     │
├──────────────────────────┴──────────────────────────┴───────────────────────┤
│  [📎] [Введите сообщение (отправится во все 3 сессии)...    ] [Send to All] │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.5 Компоненты UI

| Компонент | Тип | Описание |
|-----------|-----|----------|
| StrategySelector | ExposedDropdownMenu | Выпадающий список стратегий |
| FactsPanel | LazyColumn (SidePanel) | Панель с facts (Sticky Facts) |
| BranchTreeView | LazyColumn | Дерево веток (Branching) |
| CheckpointButton | FilledTonalButton | Создание checkpoint |
| ComparisonView | Row с 3 Columns | 3 параллельных чата |
| TokenStats | Surface с Text | Статистика токенов |
| StrategySettingsDialog | AlertDialog | Настройки параметров |
| BranchIndicator | Icon + Text | Индикатор текущей ветки |
| FactCategoryCard | Card | Карточка категории facts |
| CreateBranchDialog | AlertDialog | Создание новой ветки |

## 5. Техническая реализация

### 5.1 Архитектура (MVVM + Clean Architecture)

```
Presentation Layer:
├── ChatScreen.kt - Composable экран (модификация)
├── ChatViewModel.kt - ViewModel (модификация)
├── ChatViewState.kt - UI State (модификация)
├── ChatEvent.kt - UI Events (модификация)
├── ChatAction.kt - UI Actions (модификация)
│
├── components/
│   ├── StrategySelector.kt - Компонент выбора стратегии
│   ├── FactsPanel.kt - Панель facts
│   ├── BranchTreeView.kt - Дерево веток
│   ├── ComparisonView.kt - View для сравнения
│   └── TokenStats.kt - Статистика токенов
│
├── comparison/
│   ├── ComparisonViewModel.kt - ViewModel для режима сравнения
│   ├── ComparisonState.kt - State для сравнения
│   └── ComparisonEvent.kt - Events для сравнения

Domain Layer:
├── model/
│   ├── ContextStrategy.kt - Enum стратегий
│   ├── StrategyConfig.kt - Конфигурация стратегии
│   ├── Fact.kt - Модель fact
│   ├── FactCategory.kt - Категории facts
│   ├── Checkpoint.kt - Модель checkpoint
│   ├── Branch.kt - Модель ветки
│   └── ComparisonResult.kt - Результат сравнения
│
├── strategy/
│   ├── ContextStrategyProcessor.kt - Interface стратегии
│   ├── SlidingWindowStrategy.kt - Реализация Sliding Window
│   ├── StickyFactsStrategy.kt - Реализация Sticky Facts
│   └── BranchingStrategy.kt - Реализация Branching
│
├── usecase/
│   ├── SendMessageUseCase.kt - Модификация (добавить strategy)
│   ├── GetContextForStrategyUseCase.kt - NEW
│   ├── ExtractFactsUseCase.kt - NEW
│   ├── CreateCheckpointUseCase.kt - NEW
│   ├── CreateBranchUseCase.kt - NEW
│   ├── SwitchBranchUseCase.kt - NEW
│   ├── GetBranchesUseCase.kt - NEW
│   ├── RunComparisonUseCase.kt - NEW
│   └── ExportComparisonUseCase.kt - NEW
│
├── repository/
│   ├── ChatRepository.kt - Модификация (добавить методы)
│   ├── FactsRepository.kt - NEW
│   └── BranchRepository.kt - NEW
│
└── optimization/
    ├── ContextOptimizer.kt - Модификация
    └── FactsExtractor.kt - NEW

Data Layer:
├── entity/
│   ├── MessageEntity.kt - Модификация (добавить checkpointId)
│   ├── FactEntity.kt - NEW
│   ├── CheckpointEntity.kt - NEW
│   └── BranchEntity.kt - NEW
│
├── dao/
│   ├── MessageDao.kt - Модификация
│   ├── FactDao.kt - NEW
│   ├── CheckpointDao.kt - NEW
│   └── BranchDao.kt - NEW
│
├── repository/
│   ├── ChatRepositoryImpl.kt - Модификация
│   ├── FactsRepositoryImpl.kt - NEW
│   └── BranchRepositoryImpl.kt - NEW
│
└── remote/
    ├── DeepSeekApiClient.kt - Модификация
    └── dto/
        └── FactsExtractionRequest.kt - NEW
```

### 5.2 Новые модели данных

#### ContextStrategy.kt
```kotlin
enum class ContextStrategy {
    SLIDING_WINDOW,
    STICKY_FACTS,
    BRANCHING
}
```

#### StrategyConfig.kt
```kotlin
sealed class StrategyConfig {
    data class SlidingWindow(val messageCount: Int = 10) : StrategyConfig()
    data class StickyFacts(
        val categories: Set<FactCategory> = FactCategory.DEFAULT_CATEGORIES,
        val includeInContext: Boolean = true
    ) : StrategyConfig()
    data object Branching : StrategyConfig()
}
```

#### Fact.kt
```kotlin
data class Fact(
    val id: String,
    val sessionId: String,
    val category: FactCategory,
    val key: String,
    val value: String,
    val sourceMessageId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val confidence: Float = 1.0f
)
```

#### FactCategory.kt
```kotlin
enum class FactCategory(val displayName: String, val icon: String) {
    GOAL("Goal", "🎯"),
    CONSTRAINTS("Constraints", "⚠️"),
    PREFERENCES("Preferences", "👤"),
    DECISIONS("Decisions", "✅"),
    AGREEMENTS("Agreements", "📝");

    companion object {
        val DEFAULT_CATEGORIES = entries.toSet()
    }
}
```

#### Checkpoint.kt
```kotlin
data class Checkpoint(
    val id: String,
    val sessionId: String,
    val name: String,
    val parentCheckpointId: String?, // null для root
    val messageId: String, // Сообщение, после которого создан checkpoint
    val createdAt: Long,
    val branchName: String = "main"
)
```

#### Branch.kt
```kotlin
data class Branch(
    val id: String,
    val sessionId: String,
    val checkpointId: String,
    val name: String,
    val createdAt: Long,
    val messageCount: Int = 0
)
```

#### Message (модификация)
```kotlin
data class Message(
    val id: String,
    val content: String,
    val senderType: SenderType,
    val timestamp: Long,
    // NEW FIELDS:
    val checkpointId: String? = null, // null = main branch
    val parentMessageId: String? = null // Для tree структуры
)
```

### 5.3 Файлы для создания

| Файл | Описание |
|------|----------|
| `features/chat/domain/model/ContextStrategy.kt` | Enum стратегий контекста |
| `features/chat/domain/model/StrategyConfig.kt` | Sealed class конфигураций |
| `features/chat/domain/model/Fact.kt` | Модель fact |
| `features/chat/domain/model/FactCategory.kt` | Категории facts |
| `features/chat/domain/model/Checkpoint.kt` | Модель checkpoint |
| `features/chat/domain/model/Branch.kt` | Модель ветки |
| `features/chat/domain/model/ComparisonResult.kt` | Результат сравнения |
| `features/chat/domain/strategy/ContextStrategyProcessor.kt` | Interface стратегии |
| `features/chat/domain/strategy/SlidingWindowStrategy.kt` | Реализация Sliding Window |
| `features/chat/domain/strategy/StickyFactsStrategy.kt` | Реализация Sticky Facts |
| `features/chat/domain/strategy/BranchingStrategy.kt` | Реализация Branching |
| `features/chat/domain/usecase/GetContextForStrategyUseCase.kt` | UseCase получения контекста |
| `features/chat/domain/usecase/ExtractFactsUseCase.kt` | UseCase извлечения facts |
| `features/chat/domain/usecase/CreateCheckpointUseCase.kt` | UseCase создания checkpoint |
| `features/chat/domain/usecase/CreateBranchUseCase.kt` | UseCase создания ветки |
| `features/chat/domain/usecase/SwitchBranchUseCase.kt` | UseCase переключения ветки |
| `features/chat/domain/usecase/GetBranchesUseCase.kt` | UseCase получения веток |
| `features/chat/domain/usecase/RunComparisonUseCase.kt` | UseCase запуска сравнения |
| `features/chat/domain/usecase/ExportComparisonUseCase.kt` | UseCase экспорта |
| `features/chat/domain/repository/FactsRepository.kt` | Repository interface |
| `features/chat/domain/repository/BranchRepository.kt` | Repository interface |
| `features/chat/domain/optimization/FactsExtractor.kt` | Извлечение facts через LLM |
| `features/chat/data/entity/FactEntity.kt` | Room entity |
| `features/chat/data/entity/CheckpointEntity.kt` | Room entity |
| `features/chat/data/entity/BranchEntity.kt` | Room entity |
| `features/chat/data/dao/FactDao.kt` | Room DAO |
| `features/chat/data/dao/CheckpointDao.kt` | Room DAO |
| `features/chat/data/dao/BranchDao.kt` | Room DAO |
| `features/chat/data/repository/FactsRepositoryImpl.kt` | Repository implementation |
| `features/chat/data/repository/BranchRepositoryImpl.kt` | Repository implementation |
| `features/chat/data/remote/dto/FactsExtractionRequest.kt` | DTO для LLM |
| `features/chat/presentation/components/StrategySelector.kt` | UI компонент |
| `features/chat/presentation/components/FactsPanel.kt` | UI компонент |
| `features/chat/presentation/components/BranchTreeView.kt` | UI компонент |
| `features/chat/presentation/components/ComparisonView.kt` | UI компонент |
| `features/chat/presentation/components/TokenStats.kt` | UI компонент |
| `features/chat/presentation/comparison/ComparisonViewModel.kt` | ViewModel |
| `features/chat/presentation/comparison/ComparisonState.kt` | State |
| `features/chat/presentation/comparison/ComparisonEvent.kt` | Event |
| `features/chat/presentation/models/StrategySettingsState.kt` | State для настроек |

### 5.4 Файлы для изменения

| Файл | Изменение |
|------|-----------|
| `features/chat/domain/model/Message.kt` | Добавить checkpointId, parentMessageId |
| `features/chat/data/local/entity/MessageEntity.kt` | Добавить колонки checkpointId |
| `features/chat/data/local/dao/MessageDao.kt` | Добавить запросы для branching |
| `features/chat/data/local/mapper/MessageMapper.kt` | Обновить маппинг |
| `features/chat/domain/repository/ChatRepository.kt` | Добавить методы стратегий |
| `features/chat/data/repository/ChatRepositoryImpl.kt` | Реализовать новые методы |
| `features/chat/domain/optimization/ContextOptimizer.kt` | Рефакторинг под стратегии |
| `features/chat/domain/usecase/SendMessageUseCase.kt` | Добавить параметр strategy |
| `features/chat/presentation/ChatViewModel.kt` | Добавить state стратегий |
| `features/chat/presentation/ChatScreen.kt` | Добавить UI стратегий |
| `features/chat/presentation/models/ChatViewState.kt` | Добавить поля стратегий |
| `features/chat/presentation/models/ChatEvent.kt` | Добавить events стратегий |
| `features/chat/presentation/models/ChatAction.kt` | Добавить actions стратегий |
| `features/chat/di/FeatureChatModule.kt` | Добавить новые зависимости |
| `core/database/AppDatabase.kt` | Добавить новые entities |

### 5.5 DI зависимости

```kotlin
// В FeatureChatModule.kt добавить:

// Strategy Processors
singleOf(::SlidingWindowStrategy)
singleOf(::StickyFactsStrategy)
singleOf(::BranchingStrategy)

// Context Strategy Processor Factory
single<ContextStrategyProcessor.Factory> {
    ContextStrategyProcessor.Factory(
        slidingWindow = get(),
        stickyFacts = get(),
        branching = get()
    )
}

// Repositories
singleOf(::FactsRepositoryImpl) bind FactsRepository::class
singleOf(::BranchRepositoryImpl) bind BranchRepository::class

// UseCases
singleOf(::GetContextForStrategyUseCase)
singleOf(::ExtractFactsUseCase)
singleOf(::CreateCheckpointUseCase)
singleOf(::CreateBranchUseCase)
singleOf(::SwitchBranchUseCase)
singleOf(::GetBranchesUseCase)
singleOf(::RunComparisonUseCase)
singleOf(::ExportComparisonUseCase)

// Comparison ViewModel
viewModelOf(::ComparisonViewModel)
```

### 5.6 Database Migration

```kotlin
// Migration from version 1 to 2

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add checkpointId to messages table
        database.execSQL(
            "ALTER TABLE messages ADD COLUMN checkpointId TEXT REFERENCES checkpoints(id)"
        )
        database.execSQL(
            "ALTER TABLE messages ADD COLUMN parentMessageId TEXT REFERENCES messages(id)"
        )

        // Create facts table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS facts (
                id TEXT NOT NULL PRIMARY KEY,
                sessionId TEXT NOT NULL,
                category TEXT NOT NULL,
                key TEXT NOT NULL,
                value TEXT NOT NULL,
                sourceMessageId TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                confidence REAL NOT NULL,
                FOREIGN KEY(sessionId) REFERENCES chat_sessions(id) ON DELETE CASCADE,
                FOREIGN KEY(sourceMessageId) REFERENCES messages(id) ON DELETE CASCADE
            )
        """)
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_facts_sessionId ON facts(sessionId)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_facts_category ON facts(category)"
        )

        // Create checkpoints table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS checkpoints (
                id TEXT NOT NULL PRIMARY KEY,
                sessionId TEXT NOT NULL,
                name TEXT NOT NULL,
                parentCheckpointId TEXT,
                messageId TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                branchName TEXT NOT NULL,
                FOREIGN KEY(sessionId) REFERENCES chat_sessions(id) ON DELETE CASCADE,
                FOREIGN KEY(messageId) REFERENCES messages(id) ON DELETE CASCADE
            )
        """)
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_checkpoints_sessionId ON checkpoints(sessionId)"
        )

        // Create branches table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS branches (
                id TEXT NOT NULL PRIMARY KEY,
                sessionId TEXT NOT NULL,
                checkpointId TEXT NOT NULL,
                name TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                messageCount INTEGER NOT NULL,
                FOREIGN KEY(sessionId) REFERENCES chat_sessions(id) ON DELETE CASCADE,
                FOREIGN KEY(checkpointId) REFERENCES checkpoints(id) ON DELETE CASCADE
            )
        """)
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_branches_sessionId ON branches(sessionId)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_branches_checkpointId ON branches(checkpointId)"
        )
    }
}
```

## 6. Критерии приёмки

### 6.1 Функциональные

- [ ] Sliding Window: отправляются только последние N сообщений
- [ ] Sliding Window: N настраивается через UI
- [ ] Sticky Facts: facts извлекаются после каждого сообщения пользователя
- [ ] Sticky Facts: facts отображаются в боковой панели
- [ ] Sticky Facts: facts можно редактировать вручную
- [ ] Sticky Facts: facts корректно добавляются в контекст LLM
- [ ] Branching: checkpoint создаётся по команде
- [ ] Branching: ветки отображаются в дереве
- [ ] Branching: переключение между ветками работает
- [ ] Branching: сообщения в разных ветках изолированы
- [ ] Comparison: 3 сессии запускаются параллельно
- [ ] Comparison: статистика токенов корректна
- [ ] Comparison: экспорт в JSON работает

### 6.2 Desktop-specific

- [ ] Работает на Windows/macOS/Linux
- [ ] Keyboard shortcuts не конфликтуют с системой
- [ ] Все файловые операции используют Dispatchers.IO
- [ ] Database migrations работают корректно
- [ ] UI адаптируется под размер окна

### 6.3 Качество кода

- [ ] Clean Architecture соблюдена
- [ ] Все Strategy Processor реализуют общий interface
- [ ] ViewModel тестируем (без Android зависимостей)
- [ ] Нет блокирующих вызовов в UI
- [ ] Error handling для всех API вызовов

### 6.4 UI/UX

- [ ] Переключатель стратегий в TopAppBar
- [ ] Keyboard shortcuts работают (минимум 6)
- [ ] Menu structure корректна
- [ ] Диалог предупреждения при переключении стратегии
- [ ] Loading states для длительных операций
- [ ] Error states с понятными сообщениями

## 7. Риски и зависимости

### Риски

| Риск | Вероятность | Влияние | Митигация |
|------|-------------|---------|-----------|
| Дополнительный API вызов для facts увеличивает стоимость | Высокая | Среднее | Добавить toggle для отключения авто-извлечения |
| Branching усложняет UI | Средняя | Высокое | Итеративный UX дизайн, user testing |
| Migration для существующих пользователей | Средняя | Высокое | Тщательное тестирование migration |
| Конфликты keyboard shortcuts на разных OS | Низкая | Низкое | Использовать стандартные комбинации |
| Превышение rate limit при comparison mode | Средняя | Среднее | Добавить задержку между запросами |

### Зависимости

- Зависимость от: DeepSeek API (для facts extraction)
- Зависимость от: Room database (migration)
- Зависимость от: Существующая Chat feature (модификация)

## 8. План реализации

### Фаза 1: Sliding Window (Приоритет: HIGH, Оценка: 8-10 часов)

1. Создать модели ContextStrategy, StrategyConfig
2. Реализовать ContextStrategyProcessor interface
3. Реализовать SlidingWindowStrategy
4. Модифицировать ChatRepository для поддержки стратегий
5. Добавить UI компонент StrategySelector
6. Добавить keyboard shortcuts
7. Модифицировать ChatViewModel для работы со стратегиями
8. Unit тесты для SlidingWindowStrategy

### Фаза 2: Sticky Facts (Приоритет: HIGH, Оценка: 12-15 часов)

1. Создать модели Fact, FactCategory
2. Создать FactEntity, FactDao
3. Реализовать FactsRepository
4. Реализовать FactsExtractor (LLM integration)
5. Реализовать StickyFactsStrategy
6. Создать FactsPanel UI компонент
7. Реализовать ExtractFactsUseCase
8. Добавить edit facts functionality
9. Database migration
10. Integration тесты

### Фаза 3: Branching (Приоритет: MEDIUM, Оценка: 15-18 часов)

1. Модифицировать Message entity (добавить checkpointId)
2. Создать CheckpointEntity, BranchEntity
3. Создать CheckpointDao, BranchDao
4. Реализовать BranchRepository
5. Реализовать BranchingStrategy
6. Создать BranchTreeView UI компонент
7. Реализовать CreateCheckpointUseCase
8. Реализовать CreateBranchUseCase
9. Реализовать SwitchBranchUseCase
10. Реализовать GetBranchesUseCase
11. Database migration
12. UI для создания checkpoint и веток
13. Integration тесты

### Фаза 4: Comparison Mode (Приоритет: LOW, Оценка: 8-10 часов)

1. Создать ComparisonResult model
2. Реализовать RunComparisonUseCase
3. Реализовать ExportComparisonUseCase
4. Создать ComparisonViewModel
5. Создать ComparisonView UI
6. Реализовать параллельный запуск 3 сессий
7. Добавить сбор статистики
8. Добавить export functionality
9. UI для режима сравнения

### Фаза 5: Documentation & Testing (Приоритет: LOW, Оценка: 4-6 часов)

1. Обновить README.md
2. Создать документацию по стратегиям
3. Создать документацию по keyboard shortcuts
4. Финальное тестирование на всех платформах
5. Code review

**Общая оценка: 47-59 часов**

## 9. Notes

### Дополнительные рекомендации

1. **Logging**: Добавить детальное логирование для каждой стратегии для отладки
2. **Analytics**: Рассмотреть добавление analytics для отслеживания использования стратегий
3. **Performance**: Кэшировать facts для избежания повторного извлечения
4. **Offline**: Реализовать offline-first подход для facts (сохранять локально, синхронизировать при подключении)
5. **Accessibility**: Добавить content descriptions для всех интерактивных элементов

### Open Questions (для future iterations)

1. Автоматическое суммирование старых сообщений (summary-based context)
2. Персистентность facts между сессиями
3. Импорт/экспорт веток диалога
4. Merge веток в Branching стратегии
5. Machine learning для автоматического выбора оптимальной стратегии

---

**Версия спецификации:** 1.0
**Дата создания:** 2026-03-02
**Автор:** Business Analyst Agent
**Статус:** Готово к реализации
