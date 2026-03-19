package ru.agent.features.memory.domain.model

import ru.agent.features.chat.domain.model.Message
import ru.agent.features.invariant.domain.model.Invariant
import ru.agent.features.rag.domain.model.ChunkScore

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
 * @property activeInvariants Активные инварианты проекта
 * @property relevantChunks Релевантные чанки из RAG индекса
 */
data class MemoryContext(
    val shortTermMemory: ShortTermMemory = ShortTermMemory(),
    val workingMemory: WorkingMemory? = null,
    val userProfile: UserProfile? = null,
    val relevantKnowledge: List<KnowledgeEntry> = emptyList(),
    val activeAnchors: List<ContextAnchor> = emptyList(),
    val activeInvariants: List<Invariant> = emptyList(),
    val relevantChunks: List<ChunkScore> = emptyList()
) {

    /**
     * Проверить, пустой ли контекст.
     */
    fun isEmpty(): Boolean {
        return shortTermMemory.isEmpty() &&
                workingMemory == null &&
                userProfile == null &&
                relevantKnowledge.isEmpty() &&
                activeAnchors.isEmpty() &&
                activeInvariants.isEmpty() &&
                relevantChunks.isEmpty()
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

        // RAG контекст (code snippets from indexed files)
        if (relevantChunks.isNotEmpty()) {
            parts.add(buildRagSection())
        }

        // База знаний
        if (relevantKnowledge.isNotEmpty()) {
            parts.add(buildKnowledgeSection())
        }

        // Рабочая память
        workingMemory?.let { wm ->
            parts.add(buildWorkingMemorySection(wm))
        }

        // Инварианты проекта
        if (activeInvariants.isNotEmpty()) {
            parts.add(buildInvariantsSection())
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

    /**
     * Получить только включенные инварианты из списка активных.
     */
    fun getEnabledInvariants(): List<Invariant> {
        return activeInvariants.filter { it.isActive }
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

    private fun buildInvariantsSection(): String {
        val criticalInvariants = activeInvariants.filter { it.priority == ru.agent.features.invariant.domain.model.InvariantPriority.CRITICAL }
        val highInvariants = activeInvariants.filter { it.priority == ru.agent.features.invariant.domain.model.InvariantPriority.HIGH }
        val mediumInvariants = activeInvariants.filter { it.priority == ru.agent.features.invariant.domain.model.InvariantPriority.MEDIUM }

        val sections = mutableListOf<String>()

        if (criticalInvariants.isNotEmpty()) {
            sections.add("CRITICAL RULES (MUST FOLLOW):")
            sections.addAll(criticalInvariants.map { "- ${it.description}" })
        }

        if (highInvariants.isNotEmpty()) {
            sections.add("")
            sections.add("HIGH PRIORITY RULES (STRONGLY RECOMMENDED):")
            sections.addAll(highInvariants.map { "- ${it.description}" })
        }

        if (mediumInvariants.isNotEmpty()) {
            sections.add("")
            sections.add("MEDIUM PRIORITY RULES:")
            sections.addAll(mediumInvariants.map { "- ${it.description}" })
        }

        return """
            |--- PROJECT INVARIANTS ---
            |${sections.joinToString("\n")}
            |
            |VIOLATION HANDLING INSTRUCTIONS:
            |1. If a requested change violates a CRITICAL rule, EXPLAIN why and REFUSE to make the change.
            |2. If a requested change violates a HIGH priority rule, WARN the user and ask for confirmation.
            |3. If a requested change violates a MEDIUM priority rule, INFORM the user but proceed if they confirm.
            |4. ALWAYS explain which invariant is being violated and why it matters.
        """.trimMargin()
    }

    private fun buildRagSection(): String {
        val header = "--- RELEVANT CODE CONTEXT (RAG) ---\nFound ${relevantChunks.size} relevant code sections:\n"

        val chunksInfo = relevantChunks.mapIndexed { index, chunk ->
            val rank = index + 1
            val locationInfo = if (chunk.startLine > 0 && chunk.endLine > 0) {
                "Lines: ${chunk.startLine}-${chunk.endLine}"
            } else {
                ""
            }

            buildString {
                appendLine("[$rank] File: ${chunk.fileName} (similarity: ${"%.2f".format(chunk.similarity)})")
                if (locationInfo.isNotEmpty()) {
                    appendLine(locationInfo)
                }
                appendLine("```")
                appendLine(chunk.content)
                append("```")
            }
        }

        val footer = "--- END RAG CONTEXT ---"

        return buildString {
            appendLine(header)
            appendLine(chunksInfo.joinToString("\n"))
            append(footer)
        }
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
