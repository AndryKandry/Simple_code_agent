package ru.agent.features.task.domain.usecase

import ru.agent.features.task.domain.model.TaskState
import ru.agent.features.task.domain.repository.TaskStateRepository

/**
 * Use case for creating a task from a user message.
 * Automatically detects if the message is a task request and creates an appropriate task.
 */
class CreateTaskFromMessageUseCase(
    private val repository: TaskStateRepository
) {
    /**
     * Analyzes a message and creates a task if it looks like a request.
     *
     * @param sessionId The chat session ID
     * @param userMessage The user's message to analyze
     * @return The created task, or null if the message is not a task request
     */
    suspend operator fun invoke(
        sessionId: String,
        userMessage: String
    ): TaskState? {
        // Check if there's already an active task for this session
        val activeTask = repository.getActiveTaskForSession(sessionId)
        if (activeTask != null && !activeTask.isCompleted()) {
            // Clean up stale incomplete task from previous session
            repository.deleteTaskState(activeTask.taskId)
            // Continue to create a new task
        }

        // Analyze the message to determine if it's a task request
        val taskInfo = analyzeMessage(userMessage) ?: return null

        // Create new task
        val newTask = TaskState.create(
            sessionId = sessionId,
            taskName = taskInfo.first,
            taskDescription = taskInfo.second,
            totalSteps = estimateSteps(userMessage)
        )

        // Save to repository
        repository.saveTaskState(newTask)

        return newTask
    }

    /**
     * Analyzes a message to extract task information.
     * Returns null if the message is not a task request.
     */
    private fun analyzeMessage(message: String): Pair<String, String?>? {
        val trimmedMessage = message.trim()

        // Skip question-only messages - they should use RAG, not create tasks
        if (isQuestion(trimmedMessage)) {
            return null
        }

        // Skip short messages (likely casual conversation)
        if (trimmedMessage.length < MIN_MESSAGE_LENGTH) {
            return null
        }

        // Skip greeting messages
        if (isGreeting(trimmedMessage)) {
            return null
        }

        // Skip thank you messages
        if (isThankYou(trimmedMessage)) {
            return null
        }

        // Skip scheduler/reminder requests - they should be handled by tools directly
        if (isSchedulerRequest(trimmedMessage)) {
            return null
        }

        // Check if message contains task indicators
        if (isTaskRequest(trimmedMessage)) {
            // Extract task name from message (first sentence or up to 50 chars)
            val taskName = extractTaskName(trimmedMessage)
            val taskDescription = if (trimmedMessage.length > MAX_TASK_NAME_LENGTH) {
                trimmedMessage
            } else {
                null
            }
            return Pair(taskName, taskDescription)
        }

        // For longer messages without explicit task markers,
        // treat them as tasks if they're substantial
        if (trimmedMessage.length >= SUBSTANTIAL_MESSAGE_LENGTH) {
            val taskName = extractTaskName(trimmedMessage)
            return Pair(taskName, trimmedMessage)
        }

        return null
    }

    /**
     * Checks if the message is a greeting.
     */
    private fun isGreeting(message: String): Boolean {
        val lowerMessage = message.lowercase()
        return GREETING_PATTERNS.any { pattern -> lowerMessage.matches(pattern) }
    }

    /**
     * Checks if the message is a thank you.
     */
    private fun isThankYou(message: String): Boolean {
        val lowerMessage = message.lowercase()
        return THANK_YOU_PATTERNS.any { pattern -> lowerMessage.matches(pattern) }
    }

    /**
     * Checks if the message contains task request indicators.
     */
    private fun isTaskRequest(message: String): Boolean {
        val lowerMessage = message.lowercase()
        return TASK_INDICATORS.any { indicator -> lowerMessage.contains(indicator) }
    }

    /**
     * Checks if the message is a scheduler/reminder request.
     * These should be handled by tools directly, not as tasks.
     */
    private fun isSchedulerRequest(message: String): Boolean {
        val lowerMessage = message.lowercase()
        return SCHEDULER_INDICATORS.any { indicator -> lowerMessage.contains(indicator) }
    }

    /**
     * Extracts a concise task name from the message.
     */
    private fun extractTaskName(message: String): String {
        // Try to get first sentence
        val firstSentence = message.split(SENTENCE_DELIMITERS).firstOrNull()?.trim() ?: message

        // Truncate if too long
        return if (firstSentence.length <= MAX_TASK_NAME_LENGTH) {
            firstSentence
        } else {
            firstSentence.take(MAX_TASK_NAME_LENGTH - 3) + "..."
        }
    }

    /**
     * Estimates the number of steps based on message complexity.
     */
    private fun estimateSteps(message: String): Int {
        val wordCount = message.split(WHITESPACE).size

        return when {
            wordCount < 10 -> 1
            wordCount < 30 -> 2
            wordCount < 60 -> 3
            wordCount < 100 -> 4
            else -> 5
        }
    }

    /**
     * Checks if the message is a question.
     * Questions should be handled by RAG, not as tasks.
     */
    private fun isQuestion(message: String): Boolean {
        val trimmed = message.trim()

        // Check if starts with question word
        val questionStarters = listOf(
            "как", "какая", "какой", "какие", "кто", "что", "где", "когда", "почему",
            "what", "which", "who", "where", "when", "why", "how"
        )

        val firstWord = trimmed.split(Regex("\\s+")).firstOrNull()?.lowercase() ?: ""
        if (firstWord in questionStarters) {
            return true
        }

        // Check if ends with question mark
        if (trimmed.endsWith("?")) {
            return true
        }

        return false
    }

    companion object {
        private const val MIN_MESSAGE_LENGTH = 10
        private const val SUBSTANTIAL_MESSAGE_LENGTH = 50
        private const val MAX_TASK_NAME_LENGTH = 50

        private val SENTENCE_DELIMITERS = Regex("[.!?]")
        private val WHITESPACE = Regex("\\s+")

        // Patterns for greetings
        private val GREETING_PATTERNS = listOf(
            Regex("^(привет|здравствуй|hello|hi|hey|добрый\\s*(день|вечер|утро)|хай)[!.\\s]*$"),
            Regex("^(как\\s*(дела|ты|поживаешь)|what'?s\\s*up)[?!.\\s]*$", RegexOption.IGNORE_CASE)
        )

        // Patterns for thank you
        private val THANK_YOU_PATTERNS = listOf(
            Regex("^(спасибо|благодарю|thanks?|thank\\s*you)[!.\\s]*$", RegexOption.IGNORE_CASE)
        )

        // Indicators that the message should be handled by scheduler tools directly
        // (not as a task that generates code)
        private val SCHEDULER_INDICATORS = listOf(
            // Russian - reminder/scheduler keywords
            "напоминание", "напомни", "напомнить", "напоминай",
            "планировщик", "запланируй", "запланировать", "запланированный",
            "будильник", "таймер", "секундомер",
            "уведомление", "уведоми", "оповещение", "оповести",
            "каждую минуту", "каждый час", "каждый день", "каждую секунду",
            "ежедневно", "ежечасно", "ежеминутно",
            "по расписанию", "расписание",
            "регулярно", "периодически", "повторяй",
            "cron", "scheduler",
            // English - reminder/scheduler keywords
            "reminder", "remind me", "notify", "notification",
            "schedule", "scheduled", "scheduling",
            "alarm", "timer", "stopwatch",
            "every minute", "every hour", "every day", "every second",
            "daily", "hourly", "minutely",
            "recurring", "periodic", "repeat"
        )

        // Indicators that the message is a task request
        private val TASK_INDICATORS = listOf(
            // Russian
            "напиши", "создай", "сделай", "исправь", "найди", "помоги",
            "реализуй", "добавь", "удали", "измени", "обнови", "оптимизируй",
            "перепиши", "отрефактори", "отладь", "проверь", "проанализируй",
            "объясни", "расскажи", "покажи", "выведи", "запусти", "останови",
            "нужно", "надо", "хочу", "требуется", "проблема", "ошибка", "баг",
            "не работает", "сломалось", "как сделать", "как написать",
            // English
            "write", "create", "make", "fix", "find", "help",
            "implement", "add", "remove", "delete", "change", "update", "optimize",
            "refactor", "debug", "check", "analyze", "review",
            "explain", "show", "display", "run", "stop", "execute",
            "need to", "want to", "i want", "problem", "error", "bug",
            "not working", "broken", "how to", "how do i"
        )
    }
}
