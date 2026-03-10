package ru.agent.features.invariant.domain.model

/**
 * Результат валидации текста на нарушения инвариантов.
 *
 * @property hasViolations Есть ли нарушения
 * @property violations Список обнаруженных нарушений
 * @property shouldBlock Нужно ли блокировать ответ
 * @property blockMessage Сообщение о блокировке (если shouldBlock = true)
 */
data class ValidationResult(
    val hasViolations: Boolean,
    val violations: List<Violation>,
    val shouldBlock: Boolean,
    val blockMessage: String? = null
) {
    companion object {
        /**
         * Результат без нарушений.
         */
        val OK = ValidationResult(
            hasViolations = false,
            violations = emptyList(),
            shouldBlock = false,
            blockMessage = null
        )
    }
}

/**
 * Обнаруженное нарушение инварианта.
 *
 * @property invariantId ID инварианта, который был нарушен
 * @property matchedPattern Паттерн, который сработал
 * @property severity Критичность нарушения
 * @property message Сообщение о нарушении
 */
data class Violation(
    val invariantId: String,
    val matchedPattern: String,
    val severity: ViolationSeverity,
    val message: String
)

/**
 * Тип проверки.
 */
enum class CheckType {
    /**
     * Проверка запроса пользователя.
     */
    USER_REQUEST,

    /**
     * Проверка ответа AI.
     */
    AI_RESPONSE
}
