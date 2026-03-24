package ru.agent.features.taskcontext.domain.model

import kotlinx.serialization.Serializable
import ru.agent.core.time.currentTimeMillis

/**
 * ContextSummary - суммаризированный контекст для оптимизации промптов.
 *
 * Содержит сжатую версию TaskContext для эффективного использования в промптах,
 * экономя токены без потери важной информации.
 *
 * @property taskContextId ID контекста задачи
 * @property goalSummary Краткое описание цели (1-2 предложения)
 * @property keyClarifications Ключевые уточнения (bullet points)
 * @property keyConstraints Ключевые ограничения (bullet points)
 * @property stylePreferences Предпочтения стиля
 * @property estimatedTokens Оценка количества токенов
 * @property lastUpdated Время последнего обновления
 */
@Serializable
data class ContextSummary(
    val taskContextId: String,
    val goalSummary: String = "",
    val keyClarifications: List<String> = emptyList(),
    val keyConstraints: List<String> = emptyList(),
    val stylePreferences: StylePreferences = StylePreferences(),
    val estimatedTokens: Int = 0,
    val lastUpdated: Long = currentTimeMillis()
) {
    /**
     * Проверить, пустой ли саммари.
     */
    fun isEmpty(): Boolean {
        return goalSummary.isBlank() &&
                keyClarifications.isEmpty() &&
                keyConstraints.isEmpty()
    }

    /**
     * Сформировать строку для вставки в промпт.
     */
    fun toPromptString(): String {
        if (isEmpty()) return ""

        return buildString {
            appendLine("=== TASK CONTEXT ===")

            if (goalSummary.isNotBlank()) {
                appendLine("Goal: $goalSummary")
            }

            if (keyClarifications.isNotEmpty()) {
                appendLine("Key clarifications:")
                keyClarifications.forEach { appendLine("  - $it") }
            }

            if (keyConstraints.isNotEmpty()) {
                appendLine("Constraints:")
                keyConstraints.forEach { appendLine("  - $it") }
            }

            if (stylePreferences.language.isNotBlank()) {
                appendLine("Language: ${stylePreferences.language}")
            }

            appendLine("=== END CONTEXT ===")
        }
    }

    /**
     * Оценить токены для саммари.
     */
    fun estimateTokenCount(): Int {
        val text = toPromptString()
        // Примерная оценка: ~4 символа на токен
        return text.length / 4
    }

    companion object {
        /**
         * Создать саммари из TaskContext.
         */
        fun fromTaskContext(taskContext: TaskContext): ContextSummary {
            val goalSummary = taskContext.goal?.take(200) ?: ""
            val keyClarifications = taskContext.clarifications
                .take(5)
                .map { "${it.topic}: ${it.clarification.take(100)}" }
            val keyConstraints = taskContext.constraints
                .take(5)
                .map { "[${it.type.name}] ${it.description.take(100)}" }

            val summary = ContextSummary(
                taskContextId = taskContext.id,
                goalSummary = goalSummary,
                keyClarifications = keyClarifications,
                keyConstraints = keyConstraints,
                lastUpdated = currentTimeMillis()
            )

            return summary.copy(estimatedTokens = summary.estimateTokenCount())
        }
    }
}

/**
 * Предпочтения стиля кода и ответов.
 *
 * @property language Язык программирования
 * @property codeStyle Настройки кодстайла
 * @property verbosity Детальность ответов
 */
@Serializable
data class StylePreferences(
    val language: String = "Kotlin",
    val codeStyle: CodeStyle = CodeStyle(),
    val verbosity: ResponseVerbosity = ResponseVerbosity.MEDIUM
)

/**
 * Настройки кодстайла.
 *
 * @property indentSize Размер отступа
 * @property maxLineLength Максимальная длина строки
 * @property namingConvention Конвенция именования
 */
@Serializable
data class CodeStyle(
    val indentSize: Int = 4,
    val maxLineLength: Int = 120,
    val namingConvention: NamingConvention = NamingConvention.CAMEL_CASE
)

/**
 * Конвенция именования.
 */
@Serializable
enum class NamingConvention {
    CAMEL_CASE,
    SNAKE_CASE,
    PASCAL_CASE,
    KEBAB_CASE
}

/**
 * Детальность ответов.
 */
@Serializable
enum class ResponseVerbosity {
    CONCISE,
    MEDIUM,
    VERBOSE
}
