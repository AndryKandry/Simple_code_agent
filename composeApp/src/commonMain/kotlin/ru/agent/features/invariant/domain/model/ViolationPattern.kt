package ru.agent.features.invariant.domain.model

/**
 * Паттерн нарушения инварианта.
 *
 * Используется для обнаружения нарушений инвариантов в тексте
 * (запросах пользователя или ответах AI).
 *
 * @property invariantId ID инварианта, к которому относится паттерн
 * @property patterns Список regex паттернов для обнаружения нарушения
 * @property severity Критичность нарушения
 * @property blockMessage Сообщение, которое будет показано при блокировке
 * @property checkTypes Типы проверок, к которым применяется паттерн (по умолчанию - все)
 */
data class ViolationPattern(
    val invariantId: String,
    val patterns: List<String>,
    val severity: ViolationSeverity,
    val blockMessage: String,
    val checkTypes: Set<CheckType> = CheckType.entries.toSet()
)

/**
 * Критичность нарушения.
 */
enum class ViolationSeverity {
    /**
     * Критическое нарушение - блокировать ответ полностью.
     */
    BLOCK,

    /**
     * Предупредить пользователя, но не блокировать.
     */
    WARN,

    /**
     * Только логировать нарушение.
     */
    LOG
}
