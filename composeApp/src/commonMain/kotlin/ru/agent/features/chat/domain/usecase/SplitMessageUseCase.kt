package ru.agent.features.chat.domain.usecase

import ru.agent.features.chat.domain.split.MessageSplitter
import ru.agent.features.chat.domain.split.SplitResult

/**
 * UseCase для разбиения длинных сообщений на части.
 *
 * Инкапсулирует логику разбиения и предоставляет простой интерфейс для Presentation слоя.
 */
class SplitMessageUseCase(
    private val messageSplitter: MessageSplitter
) {

    /**
     * Разбить сообщение на части.
     *
     * @param content Текст сообщения
     * @param limit Лимит символов (опционально, используется значение по умолчанию)
     * @return Результат разбиения
     */
    operator fun invoke(content: String, limit: Int? = null): SplitResult {
        return if (limit != null) {
            messageSplitter.split(content, limit)
        } else {
            messageSplitter.split(content)
        }
    }

    /**
     * Проверить, требует ли сообщение разбиения.
     *
     * @param content Текст сообщения
     * @param limit Лимит символов (опционально)
     * @return true если сообщение длиннее лимита
     */
    fun needsSplit(content: String, limit: Int? = null): Boolean {
        val actualLimit = limit ?: MessageSplitter.DEFAULT_LIMIT
        return content.length > actualLimit
    }

    /**
     * Получить количество частей для сообщения.
     *
     * @param content Текст сообщения
     * @param limit Лимит символов (опционально)
     * @return Количество частей
     */
    fun getPartCount(content: String, limit: Int? = null): Int {
        return invoke(content, limit).totalParts
    }
}
