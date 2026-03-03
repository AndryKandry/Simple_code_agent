package ru.agent.features.memory.domain.usecase

import ru.agent.core.time.currentTimeMillis
import ru.agent.features.memory.domain.model.AnchorType
import ru.agent.features.memory.domain.model.CodeStyle
import ru.agent.features.memory.domain.model.ContextAnchor
import ru.agent.features.memory.domain.model.InteractionStats
import ru.agent.features.memory.domain.model.KnowledgeCategory
import ru.agent.features.memory.domain.model.KnowledgeEntry
import ru.agent.features.memory.domain.model.ResponseVerbosity
import ru.agent.features.memory.domain.model.UserPreferences
import ru.agent.features.memory.domain.model.UserProfile
import ru.agent.features.memory.domain.repository.LongTermMemoryRepository

/**
 * UseCase для инициализации демо-данных памяти.
 *
 * Создает примеры данных для всех трех уровней памяти:
 * - Профиль пользователя
 * - Записи в базе знаний (9 записей)
 * - Контекстные якоря (3 якоря)
 */
class InitializeDemoMemoryUseCase(
    private val longTermMemoryRepository: LongTermMemoryRepository
) {
    /**
     * Инициализирует демо-данные, если они еще не созданы.
     */
    suspend operator fun invoke() {
        // Проверить, есть ли уже данные
        val existingProfile = longTermMemoryRepository.getUserProfile(DEFAULT_USER_ID)
        if (existingProfile != null) return  // Данные уже есть

        // 1. Создать профиль пользователя
        createDemoUserProfile()

        // 2. Добавить записи в базу знаний
        createDemoKnowledgeEntries()

        // 3. Создать контекстные якоря
        createDemoContextAnchors()
    }

    private suspend fun createDemoUserProfile() {
        val now = currentTimeMillis()
        val profile = UserProfile(
            id = DEFAULT_USER_ID,
            name = "Developer",
            preferences = UserPreferences(
                preferredLanguage = "kotlin",
                codeStyle = CodeStyle(
                    indentSize = 4,
                    maxLineLength = 120
                ),
                theme = "dark",
                responseVerbosity = ResponseVerbosity.NORMAL,
                customInstructions = listOf(
                    "Always use meaningful variable names",
                    "Prefer functional style over loops"
                )
            ),
            interactionStats = InteractionStats(
                totalSessions = 42,
                totalMessages = 256,
                totalTasksCompleted = 128,
                averageSessionLength = 15.5f,
                mostUsedTaskTypes = mapOf(
                    "code_generation" to 45,
                    "refactoring" to 30,
                    "debugging" to 25
                ),
                lastActiveAt = now
            ),
            createdAt = now,
            updatedAt = now
        )
        longTermMemoryRepository.saveUserProfile(profile)
    }

    private suspend fun createDemoKnowledgeEntries() {
        val now = currentTimeMillis()

        val entries = listOf(
            // Code Patterns
            KnowledgeEntry(
                id = "demo_1",
                key = "mvvm_architecture",
                value = "MVVM pattern separates UI (View) from business logic (ViewModel) and data (Model). ViewModel exposes StateFlow/Flow for UI observation. Use cases encapsulate single business operations.",
                category = KnowledgeCategory.CODE_PATTERN,
                tags = listOf("architecture", "mvvm", "kotlin"),
                relevanceScore = 0.95f,
                createdAt = now,
                updatedAt = now
            ),
            KnowledgeEntry(
                id = "demo_2",
                key = "clean_architecture_layers",
                value = "Three layers: Presentation (UI, ViewModels), Domain (UseCases, Entities), Data (Repositories, DataSource). Dependencies point inward - Presentation depends on Domain, Domain has no dependencies.",
                category = KnowledgeCategory.CODE_PATTERN,
                tags = listOf("architecture", "clean-architecture", "layers"),
                relevanceScore = 0.9f,
                createdAt = now,
                updatedAt = now
            ),
            KnowledgeEntry(
                id = "demo_3",
                key = "stateflow_usage",
                value = "StateFlow is a hot flow that always has a value. Use for UI state exposure. Initialize with initial value. Thread-safe by design. Use 'by' delegation for convenient access.",
                category = KnowledgeCategory.API_REFERENCE,
                tags = listOf("kotlin", "coroutines", "stateflow"),
                relevanceScore = 0.85f,
                createdAt = now,
                updatedAt = now
            ),
            KnowledgeEntry(
                id = "demo_4",
                key = "room_database_entity",
                value = "Entity classes represent database tables. Use @Entity annotation. Primary key with @PrimaryKey. Column info with @ColumnInfo. DAOs define data access methods with @Dao annotation.",
                category = KnowledgeCategory.API_REFERENCE,
                tags = listOf("android", "room", "database"),
                relevanceScore = 0.8f,
                createdAt = now,
                updatedAt = now
            ),
            KnowledgeEntry(
                id = "demo_5",
                key = "koin_di_setup",
                value = "Koin modules define dependencies with 'module' block. Use 'single' for singletons, 'factory' for new instances each time. ViewModels with 'viewModel' keyword. Include modules in Application setup.",
                category = KnowledgeCategory.SNIPPET,
                tags = listOf("koin", "di", "dependency-injection"),
                relevanceScore = 0.75f,
                createdAt = now,
                updatedAt = now
            ),

            // Error Solutions
            KnowledgeEntry(
                id = "demo_6",
                key = "compilation_error_type_mismatch",
                value = "Type mismatch often occurs when expected type differs from actual. Check generics, nullable types, and implicit conversions. Use 'is' keyword for smart casting.",
                category = KnowledgeCategory.ERROR_SOLUTION,
                tags = listOf("error", "compilation", "types"),
                relevanceScore = 0.7f,
                createdAt = now,
                updatedAt = now
            ),
            KnowledgeEntry(
                id = "demo_7",
                key = "null_safety_tips",
                value = "Use safe call ?. for nullable access, Elvis operator ?: for defaults, not-null assertion !! only when certain. Prefer handling null explicitly over !! operator.",
                category = KnowledgeCategory.ERROR_SOLUTION,
                tags = listOf("kotlin", "null-safety", "best-practices"),
                relevanceScore = 0.65f,
                createdAt = now,
                updatedAt = now
            ),

            // Project Info
            KnowledgeEntry(
                id = "demo_8",
                key = "project_structure",
                value = "Compose Multiplatform project with shared commonMain code. Features organized by modules: chat, memory, main. Core module provides base classes and utilities. Platform-specific code in jvmMain, androidMain, iosMain.",
                category = KnowledgeCategory.PROJECT_INFO,
                tags = listOf("project", "structure", "kmp"),
                relevanceScore = 0.6f,
                createdAt = now,
                updatedAt = now
            ),

            // User Preferences
            KnowledgeEntry(
                id = "demo_9",
                key = "user_coding_style",
                value = "User prefers 4-space indentation, max line length 120 characters. Likes descriptive variable names. Prefers functional programming style with map/filter/flatMap over for loops.",
                category = KnowledgeCategory.USER_PREFERENCE,
                tags = listOf("style", "preferences", "coding"),
                relevanceScore = 0.95f,
                createdAt = now,
                updatedAt = now
            )
        )

        entries.forEach { entry ->
            longTermMemoryRepository.saveKnowledgeEntry(entry)
        }
    }

    private suspend fun createDemoContextAnchors() {
        val now = currentTimeMillis()

        val anchors = listOf(
            ContextAnchor(
                id = "anchor_1",
                name = "ChatViewModel",
                type = AnchorType.FILE,
                path = "composeApp/src/commonMain/kotlin/ru/agent/features/chat/presentation/ChatViewModel.kt",
                context = "Main ViewModel for chat feature. Handles message sending, session management, and memory integration.",
                priority = 10,
                isActive = true,
                createdAt = now,
                updatedAt = now
            ),
            ContextAnchor(
                id = "anchor_2",
                name = "Memory System",
                type = AnchorType.TOPIC,
                topic = "Three-layer memory architecture: STM (in-memory), WM (task state), LTM (knowledge base)",
                context = "Memory context provides aggregated data for AI system prompt",
                priority = 8,
                isActive = true,
                createdAt = now,
                updatedAt = now
            ),
            ContextAnchor(
                id = "anchor_3",
                name = "composeApp",
                type = AnchorType.DIRECTORY,
                path = "composeApp/src/commonMain/kotlin",
                context = "Main source directory for shared Kotlin code",
                priority = 5,
                isActive = true,
                createdAt = now,
                updatedAt = now
            )
        )

        anchors.forEach { anchor ->
            longTermMemoryRepository.saveAnchor(anchor)
        }
    }

    companion object {
        const val DEFAULT_USER_ID = "default"
    }
}
