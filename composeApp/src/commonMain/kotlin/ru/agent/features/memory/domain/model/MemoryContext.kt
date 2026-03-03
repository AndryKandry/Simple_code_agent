package ru.agent.features.memory.domain.model

import ru.agent.features.chat.domain.model.Message

/**
 * Memory Context - агрегированный контекст памяти.
 *
 * Объединяет все три уровня памяти для формирования полного контекста
 * для AI-ассистента.
 *
 * @property shortTermMemory Краткосрочная память (in-memory)
 * @property workingMemory Рабочая память (текущая задача)
 * @property userProfile Профиль пользователя
 * @property relevantKnowledge Релевантные записи из базы знаний
 * @property activeAnchors Активные контекстные якоря
 */
data class MemoryContext(
    val shortTermMemory: ShortTermMemory = ShortTermMemory(),
    val workingMemory: WorkingMemory? = null,
    val userProfile: UserProfile? = null,
    val relevantKnowledge: List<KnowledgeEntry> = emptyList(),
    val activeAnchors: List<ContextAnchor> = emptyList()
) {

    /**
     * Проверить, пустой ли контекст.
     */
    fun isEmpty(): Boolean {
        return shortTermMemory.isEmpty() &&
                workingMemory == null &&
                userProfile == null &&
                relevantKnowledge.isEmpty() &&
                activeAnchors.isEmpty()
    }

    /**
     * Сформировать system prompt для AI на основе контекста памяти.
     */
    fun toSystemPrompt(): String {
        val parts = mutableListOf<String>()

        // Профиль пользователя
        userProfile?.let { profile ->
            parts.add(buildUserContextSection(profile))
        }

        // Контекстные якоря
        if (activeAnchors.isNotEmpty()) {
            parts.add(buildAnchorsSection())
        }

        // База знаний
        if (relevantKnowledge.isNotEmpty()) {
            parts.add(buildKnowledgeSection())
        }

        // Рабочая память
        workingMemory?.let { wm ->
            parts.add(buildWorkingMemorySection(wm))
        }

        return if (parts.isNotEmpty()) {
            """
            |=== CONTEXT INFORMATION ===
            |
            |${parts.joinToString("\n\n")}
            |
            |=== END CONTEXT ===
            """.trimMargin()
        } else {
            ""
        }
    }

    /**
     * Сформировать строку последних сообщений для контекста.
     */
    fun toConversationContext(): String {
        return shortTermMemory.toContextString()
    }

    /**
     * Получить все сообщения из краткосрочной памяти.
     */
    fun getMessages(): List<Message> {
        return shortTermMemory.messages
    }

    /**
     * Получить последние N сообщений.
     */
    fun getLastNMessages(count: Int): List<Message> {
        return shortTermMemory.getLastNMessages(count)
    }

    // === Private helper methods ===

    private fun buildUserContextSection(profile: UserProfile): String {
        val prefs = profile.preferences
        return """
            |--- USER PROFILE ---
            |Name: ${profile.name}
            |Role: ${profile.role}
            |${if (profile.context.isNotBlank()) "Context: ${profile.context}" else ""}
            |Preferred language: ${prefs.preferredLanguage}
            |Code style: indent=${prefs.codeStyle.indentSize}, maxLineLength=${prefs.codeStyle.maxLineLength}
            |Response verbosity: ${prefs.responseVerbosity}
            |${if (prefs.customInstructions.isNotEmpty()) "Custom instructions: ${prefs.customInstructions.joinToString("; ")}" else ""}
        """.trimMargin()
    }

    private fun buildAnchorsSection(): String {
        val anchorsInfo = activeAnchors.map { anchor ->
            val location = when (anchor.type) {
                AnchorType.FILE, AnchorType.DIRECTORY -> anchor.path ?: ""
                AnchorType.TOPIC -> anchor.topic ?: ""
                AnchorType.TASK, AnchorType.SESSION -> anchor.name
            }
            "- ${anchor.type}: $location${anchor.context?.let { " ($it)" } ?: ""}"
        }
        return """
            |--- ACTIVE CONTEXT ---
            |${anchorsInfo.joinToString("\n")}
        """.trimMargin()
    }

    private fun buildKnowledgeSection(): String {
        val knowledgeInfo = relevantKnowledge.map { entry ->
            "[${entry.category}] ${entry.key}: ${entry.value.take(200)}${if (entry.value.length > 200) "..." else ""}"
        }
        return """
            |--- RELEVANT KNOWLEDGE ---
            |${knowledgeInfo.joinToString("\n")}
        """.trimMargin()
    }

    private fun buildWorkingMemorySection(wm: WorkingMemory): String {
        val taskInfo = wm.taskInfo?.let { task ->
            """
            |Current task: ${task.description}
            |Type: ${task.taskType}
            |Status: ${task.status}
            |Progress: ${(task.progress * 100).toInt()}%
            """.trimMargin()
        } ?: "No active task"

        return """
            |--- CURRENT WORK ---
            |State: ${wm.executionState}
            |$taskInfo
        """.trimMargin()
    }
}

/**
 * Результат оптимизации контекста памяти.
 *
 * @property context Оптимизированный контекст
 * @property estimatedTokens Оценка количества токенов
 * @property strategy Использованная стратегия оптимизации
 * @property wasOptimized Была ли произведена оптимизация
 */
data class OptimizedMemoryContext(
    val context: MemoryContext,
    val estimatedTokens: Int,
    val strategy: MemoryOptimizationStrategy,
    val wasOptimized: Boolean = false
)

/**
 * Стратегия оптимизации памяти.
 */
enum class MemoryOptimizationStrategy {
    NONE,               // Оптимизация не требуется
    TRUNCATE_STM,       // Усечение краткосрочной памяти
    FILTER_KNOWLEDGE,   // Фильтрация базы знаний
    PRIORITIZE_ANCHORS, // Приоритизация якорей
    FULL_OPTIMIZATION   // Полная оптимизация всех компонентов
}
