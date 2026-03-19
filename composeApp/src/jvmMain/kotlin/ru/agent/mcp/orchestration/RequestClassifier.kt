package ru.agent.mcp.orchestration

import co.touchlab.kermit.Logger

/**
 * Rule-based classifier for determining request type.
 *
 * Uses regex patterns to analyze user messages and classify them
 * into categories that map to specific MCP servers.
 *
 * Supports both English and Russian keywords.
 */
class RequestClassifier {

    private val logger = Logger.withTag("RequestClassifier")

    // === File Operation Patterns ===
    private val filePatterns = listOf(
        // English
        Regex("""(?i)\b(read|write|create|delete|copy|move|list|search)\s+(file|files|directory|directories|folder|folders)"""),
        Regex("""(?i)\b(show|display|view|open|edit)\s+(file|files)"""),
        Regex("""(?i)\b(file|folder|directory)\s+(contents|content|exists|path)"""),
        Regex("""(?i)\b(find|search)\s+(files?|in\s+files?)"""),
        Regex("""(?i)\.(kt|java|py|js|ts|json|xml|gradle|md|txt|yaml|yml)\b"""),
        // Russian
        Regex("""(?i)\b(читай|прочитай|запиши|создай|удали|скопируй|перемести|покажи|открой)\s+(файл|файлы|папку|директорию)"""),
        Regex("""(?i)\b(список|найти)\s+(файлов?|папок|директорий)"""),
        Regex("""(?i)\b(содержимое|содержание)\s+(файла|папки|директории)""")
    )

    // === Terminal Command Patterns ===
    private val terminalPatterns = listOf(
        // English
        Regex("""(?i)\b(run|execute|start|launch)\s+(command|cmd|script|shell|bash)"""),
        Regex("""(?i)\b(terminal|console|cli|shell)\s+(command|script)"""),
        Regex("""(?i)\b( npm| yarn| gradle| mvn| pip| cargo| go\s+run| python| node)\s"""),
        Regex("""(?i)\b(build|compile|clean|test)\s+(project|app|application)"""),
        // Russian
        Regex("""(?i)\b(запусти|выполни|стартуй)\s+(команду|скрипт|bash|shell)"""),
        Regex("""(?i)\b(собери|скомпилируй|протестируй)\s+(проект|приложение)""")
    )

    // === Git Operation Patterns ===
    private val gitPatterns = listOf(
        // English
        Regex("""(?i)\bgit\s+(status|log|commit|push|pull|clone|branch|merge|rebase|checkout|add|diff)"""),
        Regex("""(?i)\b(commit|push|pull|merge|branch)\s+(changes|code|repository|repo)"""),
        Regex("""(?i)\b(repository|repo)\s+(status|history|log)"""),
        Regex("""(?i)\b(show|view|display)\s+(git|commits?|branches?)"""),
        // Russian
        Regex("""(?i)\b(закоммить|запуш|запулли|смерджи|переключи)\s+(изменения|код|ветку)"""),
        Regex("""(?i)\b(покажи|историю)\s+(коммитов|веток|репозитория)""")
    )

    // === Scheduling Patterns ===
    private val schedulingPatterns = listOf(
        // English
        Regex("""(?i)\b(schedule|remind|reminder|alarm|timer|notify)\s+(me|at|in|on|every)"""),
        Regex("""(?i)\b(set|create|add)\s+(reminder|schedule|task|alarm|notification)"""),
        Regex("""(?i)\b(cron|scheduled|periodic|recurring)\s+(task|job|execution)"""),
        Regex("""(?i)\b(at\s+\d{1,2}:\d{2}|in\s+\d+\s+(minutes?|hours?|days?))"""),
        // Russian
        Regex("""(?i)\b(напомни|напоминание|будильник|таймер|уведомление)"""),
        Regex("""(?i)\b(запланируй|расписание|по расписанию|периодическ)"""),
        Regex("""(?i)\b(в\s+\d{1,2}:\d{2}|через\s+\d+\s+(минут|часов|дней))""")
    )

    // === Code Analysis Patterns ===
    private val codeAnalysisPatterns = listOf(
        // English
        Regex("""(?i)\b(analyze|analyse|review|inspect|examine)\s+(code|project|files?)"""),
        Regex("""(?i)\b(find|search|detect)\s+(bugs?|issues?|errors?|problems?)"""),
        Regex("""(?i)\b(refactor|improve|optimize)\s+(code|performance)"""),
        Regex("""(?i)\b(code|static)\s+(analysis|quality|review)"""),
        Regex("""(?i)\b(run|execute)\s+(tests?|lint|checkstyle|detekt|ktlint)"""),
        // Russian
        Regex("""(?i)\b(проанализируй|проверь|осмотри|изучи)\s+(код|проект|файлы)"""),
        Regex("""(?i)\b(найди|обнаружь)\s+(баги|ошибки|проблемы)"""),
        Regex("""(?i)\b(рефакторинг|оптимизируй)\s+(код|производительность)""")
    )

    // === RAG Query Patterns ===
    private val ragPatterns = listOf(
        // Code location queries
        Regex("""(?i)\b(where|where is|in which|which file|where are)\s+(is|are|the|implemented|defined|located)"""),
        Regex("""(?i)\b(where|где)\s+(implemented|realized|defined|находится|реализован|определен)"""),
        Regex("""(?i)\b(find|найди|search|поиск)\s+(the|all|implementation|class|function|method|usage)"""),

        // Architecture queries
        Regex("""(?i)\b(architecture|архитектура|structure|структура|design|дизайн)\s+(of|проекта|project|system|системы)"""),
        Regex("""(?i)\b(how is|как\s*реализован|how does|как\s*работает)\s+(the|this|implemented|structured)"""),
        Regex("""(?i)\b(explain|объясни|describe|опиши)\s+(the|architecture|structure|design)"""),

        // Pattern search queries
        Regex("""(?i)\b(find|найди|search|поиск)\s+(all|все|pattern|паттерн|usage|использование|occurrences)"""),
        Regex("""(?i)\b(show|покажи|list|список)\s+(all|все|classes|классов|files|файлов|where|где)"""),

        // Documentation queries
        Regex("""(?i)\b(documentation|документация|readme|guide|руководство|docs)"""),
        Regex("""(?i)\b(is there|есть\s*ли|does.*have|имеет\s*ли)\s+(documentation|docs|readme)"""),

        // Code understanding queries
        Regex("""(?i)\b(what does|что\s*делает|how works|как\s*работает|purpose of|назначение)"""),
        Regex("""(?i)\b(understand|понять|explain|объясни|clarify|уточни)\s+(the|this|code|код|logic|логику)""")
    )

    // === Multi-Type Patterns ===
    private val multiTypePatterns = listOf(
        // English
        Regex("""(?i)\b(complex|multi|several|multiple)\s+(step|operation|task)"""),
        Regex("""(?i)\b(workflow|pipeline|chain)\s*(of)?\s*(operations|tasks|steps)"""),
        Regex("""(?i)\b(first|then|after\s+that|next|finally)\s*,?\s*(read|write|execute|run)"""),
        // Russian
        Regex("""(?i)\b(сложный|много|несколько)\s+(шаг|операций|задач)"""),
        Regex("""(?i)\b(сначала|потом|затем|после\s+этого|в\s+конце)""")
    )

    /**
     * Classify user message into request type.
     *
     * @param message User message to classify
     * @return Classified request type
     */
    fun classify(message: String): RequestType {
        val normalizedMessage = message.trim().lowercase()

        logger.d { "Classifying message: ${normalizedMessage.take(100)}..." }

        // Check RAG patterns FIRST (before file operations)
        // RAG queries can overlap with file operations, so we check RAG first
        if (matchesAnyPattern(normalizedMessage, ragPatterns)) {
            // But if this is clearly a file operation, terminal command, git or scheduling - don't count as RAG
            if (!matchesAnyPattern(normalizedMessage, filePatterns) &&
                !matchesAnyPattern(normalizedMessage, terminalPatterns) &&
                !matchesAnyPattern(normalizedMessage, gitPatterns) &&
                !matchesAnyPattern(normalizedMessage, schedulingPatterns)) {
                logger.d { "Classified as RAG_REQUEST" }
                return RequestType.RAG_REQUEST
            }
        }

        // Check each category in priority order
        // Git operations are checked before terminal because they are more specific
        if (matchesAnyPattern(normalizedMessage, gitPatterns)) {
            logger.d { "Classified as GIT_OPERATION" }
            return RequestType.GIT_OPERATION
        }

        if (matchesAnyPattern(normalizedMessage, schedulingPatterns)) {
            logger.d { "Classified as SCHEDULING" }
            return RequestType.SCHEDULING
        }

        if (matchesAnyPattern(normalizedMessage, codeAnalysisPatterns)) {
            logger.d { "Classified as CODE_ANALYSIS" }
            return RequestType.CODE_ANALYSIS
        }

        if (matchesAnyPattern(normalizedMessage, filePatterns)) {
            // Check if also has terminal patterns -> CODE_ANALYSIS
            if (matchesAnyPattern(normalizedMessage, terminalPatterns)) {
                logger.d { "Classified as CODE_ANALYSIS (file + terminal)" }
                return RequestType.CODE_ANALYSIS
            }
            logger.d { "Classified as FILE_OPERATION" }
            return RequestType.FILE_OPERATION
        }

        if (matchesAnyPattern(normalizedMessage, terminalPatterns)) {
            logger.d { "Classified as TERMINAL_COMMAND" }
            return RequestType.TERMINAL_COMMAND
        }

        if (matchesAnyPattern(normalizedMessage, multiTypePatterns)) {
            logger.d { "Classified as MULTI_TYPE" }
            return RequestType.MULTI_TYPE
        }

        logger.d { "Classified as UNKNOWN" }
        return RequestType.UNKNOWN
    }

    /**
     * Get confidence score for classification.
     *
     * @param message User message
     * @param type Request type to check
     * @return Confidence score from 0.0 to 1.0
     */
    fun getConfidence(message: String, type: RequestType): Double {
        val normalizedMessage = message.trim().lowercase()
        val patterns = when (type) {
            RequestType.FILE_OPERATION -> filePatterns
            RequestType.TERMINAL_COMMAND -> terminalPatterns
            RequestType.GIT_OPERATION -> gitPatterns
            RequestType.SCHEDULING -> schedulingPatterns
            RequestType.CODE_ANALYSIS -> codeAnalysisPatterns
            RequestType.MULTI_TYPE -> multiTypePatterns
            RequestType.RAG_REQUEST -> ragPatterns
            RequestType.UNKNOWN -> return 0.0
        }

        val matchCount = patterns.count { it.containsMatchIn(normalizedMessage) }
        return (matchCount.toDouble() / patterns.size).coerceIn(0.0, 1.0)
    }

    /**
     * Get all matching request types with confidence scores.
     *
     * @param message User message
     * @return Map of request types to confidence scores
     */
    fun getAllMatches(message: String): Map<RequestType, Double> {
        return RequestType.entries
            .filter { it != RequestType.UNKNOWN }
            .associateWith { getConfidence(message, it) }
            .filter { it.value > 0.0 }
            .toList()
            .sortedByDescending { it.second }
            .toMap()
    }

    /**
     * Determine which servers are needed for a request type.
     *
     * @param requestType Classified request type
     * @return Set of server names needed
     */
    fun getRequiredServers(requestType: RequestType): Set<String> {
        return when (requestType) {
            RequestType.FILE_OPERATION -> setOf("filesystem")
            RequestType.TERMINAL_COMMAND -> setOf("terminal")
            RequestType.GIT_OPERATION -> setOf("terminal")
            RequestType.SCHEDULING -> setOf("scheduler")
            RequestType.CODE_ANALYSIS -> setOf("filesystem", "terminal")
            RequestType.MULTI_TYPE -> setOf("filesystem", "terminal", "scheduler")
            RequestType.RAG_REQUEST -> emptySet() // RAG doesn't need MCP servers
            RequestType.UNKNOWN -> setOf("filesystem", "terminal", "scheduler")
        }
    }

    // === Private Helpers ===

    private fun matchesAnyPattern(text: String, patterns: List<Regex>): Boolean {
        return patterns.any { pattern -> pattern.containsMatchIn(text) }
    }
}
