package ru.agent.features.chat.domain.split

/**
 * Результат разбиения сообщения на части.
 *
 * @property originalContent Оригинальный полный текст сообщения (без изменений)
 * @property parts Список частей сообщения
 * @property batchId Уникальный идентификатор группы частей (генерируется на основе контента)
 */
data class SplitResult(
    val originalContent: String,
    val parts: List<MessagePart>,
    val batchId: String
) {
    /**
     * Требуется ли разбиение (более одной части)
     */
    val needsSplit: Boolean get() = parts.size > 1

    /**
     * Общее количество частей
     */
    val totalParts: Int get() = parts.size

    companion object {
        /**
         * Создать результат для короткого сообщения (без разбиения)
         */
        fun single(content: String, batchId: String): SplitResult {
            return SplitResult(
                originalContent = content,
                parts = listOf(MessagePart(content, 1, 1)),
                batchId = batchId
            )
        }
    }
}

/**
 * Часть разбитого сообщения.
 *
 * @property content Текст части
 * @property partNumber Номер части (1-based)
 * @property totalParts Общее количество частей
 */
data class MessagePart(
    val content: String,
    val partNumber: Int,
    val totalParts: Int
) {
    /**
     * Первая ли это часть
     */
    val isFirst: Boolean get() = partNumber == 1

    /**
     * Последняя ли это часть
     */
    val isLast: Boolean get() = partNumber == totalParts

    /**
     * Является ли это единственной частью
     */
    val isSingle: Boolean get() = totalParts == 1

    /**
     * Индикатор прогресса в формате "1/3"
     */
    val progressIndicator: String get() = "$partNumber/$totalParts"
}
