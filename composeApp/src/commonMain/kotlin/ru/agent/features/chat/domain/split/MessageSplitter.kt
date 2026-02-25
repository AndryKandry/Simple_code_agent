package ru.agent.features.chat.domain.split

/**
 * Класс для разбиения длинных сообщений на части.
 *
 * Стратегия разбиения:
 * - Разбиение по абзацам (двойной перенос строки)
 * - Не разрывать код-блоки (```...```)
 * - Не разрывать inline код (`...`)
 * - Сохранять целостность списков и цитат
 */
class MessageSplitter {

    companion object {
        const val DEFAULT_LIMIT = 3000
        const val MIN_PART_SIZE = 500 // Минимальный размер части для предотвращения слишком коротких частей

        // Regex для поиска код-блоков
        private val CODE_BLOCK_REGEX = Regex("""```[\s\S]*?```""", RegexOption.MULTILINE)
        // Regex для inline кода
        private val INLINE_CODE_REGEX = Regex("""`[^`]+`""")
        // Regex для абзацев (двойной перенос строки или более)
        private val PARAGRAPH_REGEX = Regex("""\n{2,}""")
    }

    /**
     * Разбить сообщение на части.
     *
     * @param content Текст сообщения
     * @param limit Лимит символов (по умолчанию DEFAULT_LIMIT)
     * @return Результат разбиения
     */
    fun split(content: String, limit: Int = DEFAULT_LIMIT): SplitResult {
        // Если сообщение короткое, возвращаем без разбиения
        if (content.length <= limit) {
            return SplitResult.single(content, generateBatchId(content))
        }

        val parts = mutableListOf<String>()
        val protectedRanges = findProtectedRanges(content)

        // Разбиваем по абзацам
        val paragraphs = splitByParagraphs(content, protectedRanges)

        var currentPart = StringBuilder()
        var currentLength = 0

        for (paragraph in paragraphs) {
            val paragraphLength = paragraph.length + 2 // +2 for separator

            // Проверяем, можно ли добавить абзац к текущей части
            if (currentLength + paragraphLength > limit && currentPart.isNotEmpty()) {
                // Если текущая часть не пуста и добавление превысит лимит, начинаем новую часть
                parts.add(currentPart.toString().trim())
                currentPart = StringBuilder()
                currentLength = 0
            }

            // Добавляем абзац к текущей части
            if (currentPart.isNotEmpty()) {
                currentPart.append("\n\n")
                currentLength += 2
            }
            currentPart.append(paragraph)
            currentLength += paragraph.length
        }

        // Добавляем последнюю часть, если она не пуста
        if (currentPart.isNotEmpty()) {
            parts.add(currentPart.toString().trim())
        }

        // Если какой-то абзац слишком длинный, разбиваем его принудительно
        val finalParts = mutableListOf<String>()
        for (part in parts) {
            if (part.length <= limit) {
                finalParts.add(part)
            } else {
                // Принудительное разбиение длинного абзаца
                finalParts.addAll(splitLongParagraph(part, limit, protectedRanges))
            }
        }

        val messageParts = finalParts.mapIndexed { index, text ->
            MessagePart(text, index + 1, finalParts.size)
        }

        return SplitResult(
            originalContent = content,
            parts = messageParts,
            batchId = generateBatchId(content)
        )
    }

    /**
     * Найти защищенные диапазоны (код-блоки и inline код), которые нельзя разрывать.
     */
    private fun findProtectedRanges(content: String): List<IntRange> {
        val ranges = mutableListOf<IntRange>()

        // Находим все код-блоки
        CODE_BLOCK_REGEX.findAll(content).forEach { matchResult ->
            ranges.add(matchResult.range)
        }

        // Находим все inline код
        INLINE_CODE_REGEX.findAll(content).forEach { matchResult ->
            ranges.add(matchResult.range)
        }

        return ranges.sortedBy { it.first }
    }

    /**
     * Проверить, находится ли позиция внутри защищенного диапазона.
     */
    private fun isPositionProtected(position: Int, protectedRanges: List<IntRange>): Boolean {
        return protectedRanges.any { position in it }
    }

    /**
     * Разбить текст на абзацы, учитывая защищенные диапазоны.
     */
    private fun splitByParagraphs(content: String, protectedRanges: List<IntRange>): List<String> {
        val paragraphs = mutableListOf<String>()
        var lastIndex = 0

        // Находим все разделители абзацев
        val matches = PARAGRAPH_REGEX.findAll(content).toList()

        for (match in matches) {
            val paragraphEnd = match.range.first
            val paragraph = content.substring(lastIndex, paragraphEnd).trim()

            if (paragraph.isNotEmpty()) {
                paragraphs.add(paragraph)
            }

            lastIndex = match.range.last + 1
        }

        // Добавляем последний абзац
        val lastParagraph = content.substring(lastIndex).trim()
        if (lastParagraph.isNotEmpty()) {
            paragraphs.add(lastParagraph)
        }

        return paragraphs
    }

    /**
     * Принудительное разбиение слишком длинного абзаца.
     * Пытается разбить по предложениям или словам.
     */
    private fun splitLongParagraph(
        paragraph: String,
        limit: Int,
        protectedRanges: List<IntRange>
    ): List<String> {
        val parts = mutableListOf<String>()
        var remaining = paragraph

        while (remaining.length > limit) {
            // Ищем подходящую точку разрыва
            val breakPoint = findBreakPoint(remaining, limit, protectedRanges)

            if (breakPoint > MIN_PART_SIZE) {
                parts.add(remaining.substring(0, breakPoint).trim())
                remaining = remaining.substring(breakPoint).trim()
            } else {
                // Если не можем найти хорошую точку разрыва, разбиваем по лимиту
                parts.add(remaining.substring(0, limit).trim())
                remaining = remaining.substring(limit).trim()
            }
        }

        if (remaining.isNotEmpty()) {
            parts.add(remaining)
        }

        return parts
    }

    /**
     * Найти подходящую точку разрыва в тексте.
     * Приоритет: конец предложения > конец слова > принудительный разрыв
     */
    private fun findBreakPoint(
        text: String,
        limit: Int,
        protectedRanges: List<IntRange>
    ): Int {
        // Ищем ближайшую точку разрыва до лимита
        var bestBreakPoint = -1

        // Приоритеты разрыва (от лучшего к худшему)
        val breakChars = listOf(
            ". " to 2,      // Конец предложения
            "! " to 2,      // Восклицание
            "? " to 2,      // Вопрос
            "\n" to 1,      // Перенос строки
            " " to 1        // Пробел
        )

        for ((char, length) in breakChars) {
            var searchFrom = limit
            while (searchFrom > MIN_PART_SIZE) {
                val index = text.lastIndexOf(char, startIndex = searchFrom)
                if (index > MIN_PART_SIZE && index < limit) {
                    // Проверяем, не находится ли точка разрыва в защищенном диапазоне
                    if (!isPositionProtected(index, protectedRanges)) {
                        return index + length
                    }
                }
                searchFrom = index - 1
                if (index == -1) break
            }
        }

        // Если не нашли подходящую точку, ищем любой пробел до лимита
        for (i in limit downTo MIN_PART_SIZE) {
            if (text.getOrNull(i) == ' ' && !isPositionProtected(i, protectedRanges)) {
                return i + 1
            }
        }

        // Принудительный разрыв по лимиту
        return limit
    }

    /**
     * Сгенерировать уникальный ID для группы частей на основе контента.
     */
    private fun generateBatchId(content: String): String {
        // Используем хеш от контента для создания стабильного ID
        val hash = content.hashCode()
        val positiveHash = if (hash < 0) hash.toLong() + Int.MAX_VALUE.toLong() * 2 else hash.toLong()
        val length = content.length
        return "batch_${positiveHash.toString(16)}_$length"
    }
}
