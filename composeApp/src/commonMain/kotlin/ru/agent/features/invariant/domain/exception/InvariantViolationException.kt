package ru.agent.features.invariant.domain.exception

import ru.agent.features.invariant.domain.model.Violation
import ru.agent.features.invariant.domain.model.ViolationSeverity

/**
 * Исключение при нарушении инварианта.
 *
 * Выбрасывается, когда обнаружено нарушение BLOCK-серьезности.
 *
 * @property message Сообщение об ошибке
 * @property violations Список обнаруженных нарушений
 * @property suggestions Список рекомендаций для исправления проблемы
 */
class InvariantViolationException(
    override val message: String,
    val violations: List<Violation> = emptyList(),
    val suggestions: List<String> = emptyList()
) : Exception(message) {

    /**
     * Creates exception with suggestions derived from violations.
     */
    constructor(
        message: String,
        violations: List<Violation>
    ) : this(
        message = message,
        violations = violations,
        suggestions = violations.map { it.message }.filter { it.isNotBlank() }
    )

    /**
     * Creates exception with a single suggestion.
     */
    constructor(
        message: String,
        suggestion: String
    ) : this(
        message = message,
        violations = emptyList(),
        suggestions = if (suggestion.isNotBlank()) listOf(suggestion) else emptyList()
    )

    /**
     * Returns a user-friendly message with suggestions if available.
     */
    fun toUserFriendlyMessage(): String {
        return buildString {
            append(message)

            if (suggestions.isNotEmpty()) {
                appendLine()
                appendLine()
                append("Suggested actions:")
                suggestions.forEach { suggestion ->
                    appendLine()
                    append("  - $suggestion")
                }
            }
        }
    }
}
