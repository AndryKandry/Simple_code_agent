package ru.agent.features.invariant.domain.usecase

import co.touchlab.kermit.Logger
import ru.agent.features.invariant.domain.model.CheckType
import ru.agent.features.invariant.domain.model.Violation
import ru.agent.features.invariant.domain.model.ViolationPattern
import ru.agent.features.invariant.domain.model.ViolationSeverity
import ru.agent.features.invariant.domain.model.ValidationResult
import ru.agent.features.invariant.domain.repository.InvariantRepository

/**
 * UseCase для валидации текста на нарушения инвариантов.
 *
 * Проверяет текст (запрос пользователя или ответ AI) на наличие
 * паттернов, нарушающих инварианты проекта.
 *
 * @param invariantRepository Репозиторий инвариантов
 */
class ValidateInvariantViolationUseCase(
    private val invariantRepository: InvariantRepository
) {
    private val logger = Logger.withTag("ValidateInvariantViolation")

    /**
     * Предскомпилированный паттерн нарушения с готовыми Regex объектами.
     */
    private data class CompiledViolationPattern(
        val invariantId: String,
        val compiledRegexes: List<Regex>,
        val severity: ViolationSeverity,
        val blockMessage: String,
        val applicableCheckTypes: Set<CheckType>
    )

    /**
     * Предскомпилированные паттерны нарушений.
     * Инициализируются один раз при первом обращении (lazy).
     */
    private val compiledPatterns: List<CompiledViolationPattern> by lazy {
        getViolationPatterns().map { pattern ->
            CompiledViolationPattern(
                invariantId = pattern.invariantId,
                compiledRegexes = pattern.patterns.map { regexPattern ->
                    try {
                        Regex(regexPattern, RegexOption.IGNORE_CASE)
                    } catch (e: Exception) {
                        logger.e(throwable = e) { "Invalid regex pattern: $regexPattern" }
                        // Return a regex that never matches as fallback
                        Regex("(?!)")
                    }
                },
                severity = pattern.severity,
                blockMessage = pattern.blockMessage,
                applicableCheckTypes = pattern.checkTypes
            )
        }.also {
            logger.i { "Compiled ${it.size} violation patterns" }
        }
    }

    /**
     * Проверить текст на нарушения инвариантов.
     *
     * @param text Текст для проверки (запрос пользователя или ответ AI)
     * @param checkType Тип проверки (USER_REQUEST или AI_RESPONSE)
     * @return ValidationResult с найденными нарушениями
     */
    suspend operator fun invoke(
        text: String,
        checkType: CheckType
    ): ValidationResult {
        logger.d { "Validating ${checkType.name}: ${text.take(100)}..." }

        // Find all violations using precompiled patterns (filtered by checkType)
        val violations = mutableListOf<Violation>()

        for (compiled in compiledPatterns) {
            // Filter patterns by checkType
            if (checkType !in compiled.applicableCheckTypes) continue

            for (regex in compiled.compiledRegexes) {
                if (regex.containsMatchIn(text)) {
                    val violation = Violation(
                        invariantId = compiled.invariantId,
                        matchedPattern = regex.pattern,
                        severity = compiled.severity,
                        message = compiled.blockMessage
                    )
                    violations.add(violation)
                    logger.w { "Violation detected: ${compiled.invariantId} - ${compiled.severity}" }
                    // Break inner loop - one match per pattern is enough
                    break
                }
            }
        }

        // Determine if we should block
        val hasBlockViolations = violations.any { it.severity == ViolationSeverity.BLOCK }
        val blockMessage = if (hasBlockViolations) {
            violations
                .filter { it.severity == ViolationSeverity.BLOCK }
                .joinToString("\n\n") { it.message }
        } else {
            null
        }

        val result = ValidationResult(
            hasViolations = violations.isNotEmpty(),
            violations = violations,
            shouldBlock = hasBlockViolations,
            blockMessage = blockMessage
        )

        if (result.hasViolations) {
            logger.i {
                "Validation completed: ${violations.size} violations found, " +
                "should block: ${result.shouldBlock}"
            }
        }

        return result
    }

    /**
     * Получить предустановленные паттерны нарушений.
     *
     * ВАЖНО: Избегаем .*, .?, и других паттернов, которые могут вызвать catastrophic backtracking.
     * Используем конкретные символьные классы вместо точек.
     *
     * @return Список паттернов для проверки
     */
    private fun getViolationPatterns(): List<ViolationPattern> = listOf(
        // === ARCHITECTURE: Только MVVM/MVI - USER REQUEST ===
        // Примечание: используем [\\s\\p{L}\\d_-] вместо [\\s\\w-] для поддержки кириллицы
        ViolationPattern(
            invariantId = "sys_arch_mvvm_request",
            patterns = listOf(
                // MVP/MVC запросы на русском
                "напиши[\\s\\p{L}\\d_-]*MVP",
                "создай[\\s\\p{L}\\d_-]*MVP",
                "реализуй[\\s\\p{L}\\d_-]*MVP",
                "MVP[\\s\\p{L}\\d_-]*приложение",
                "MVP[\\s\\p{L}\\d_-]*архитектур",
                "напиши[\\s\\p{L}\\d_-]*MVC",
                "создай[\\s\\p{L}\\d_-]*MVC",
                "реализуй[\\s\\p{L}\\d_-]*MVC",
                "MVC[\\s\\p{L}\\d_-]*приложение",
                "MVC[\\s\\p{L}\\d_-]*архитектур",
                // Presenter requests
                "создай[\\s\\p{L}\\d_-]*presenter",
                "напиши[\\s\\p{L}\\d_-]*presenter",
                "реализуй[\\s\\p{L}\\d_-]*presenter",
                // Controller requests (в контексте MVC)
                "напиши[\\s\\p{L}\\d_-]*controller[\\s\\p{L}\\d_-]*(?:для|в)",
                "создай[\\s\\p{L}\\d_-]*controller[\\s\\p{L}\\d_-]*(?:для|в)"
            ),
            severity = ViolationSeverity.BLOCK,
            blockMessage = "Нарушение архитектуры: проект использует только MVVM/MVI паттерны. " +
                    "Запросы на создание MVP, MVC и других архитектур не поддерживаются.",
            checkTypes = setOf(CheckType.USER_REQUEST)
        ),

        // === ARCHITECTURE: Только MVVM/MVI - AI RESPONSE ===
        ViolationPattern(
            invariantId = "sys_arch_mvvm",
            patterns = listOf(
                // MVP patterns - безопасные версии без .*
                "\\bMVP\\b[\\s\\w-]*architecture",
                "\\bMVC\\b[\\s\\w-]*architecture",
                "Model[\\s-]?View[\\s-]?Presenter",
                "presenter\\s+(class|interface|layer)",
                "create[\\s\\w-]*MVP[\\s\\w-]*application",
                "implement[\\s\\w-]*MVP",
                "use[\\s\\w-]*MVP[\\s\\w-]*pattern",
                "MVP[\\s\\w-]*approach",
                // MVC patterns
                "Model[\\s-]?View[\\s-]?Controller",
                "controller\\s+(class|layer)",
                "MVC[\\s\\w-]*pattern",
                // Other anti-patterns
                "God[\\s-]?Activity",
                "Massive[\\s-]?View[\\s-]?Controller"
            ),
            severity = ViolationSeverity.BLOCK,
            blockMessage = "Нарушение архитектуры: проект использует только MVVM/MVI паттерны. " +
                    "MVP, MVC и другие архитектуры не поддерживаются.",
            checkTypes = setOf(CheckType.AI_RESPONSE)
        ),

        // === TECHNOLOGY: CLI режим - USER REQUEST ===
        // Примечание: используем [\\s\\p{L}\\d_-] вместо [\\s\\w-] для поддержки кириллицы
        ViolationPattern(
            invariantId = "sys_tech_cli_request",
            patterns = listOf(
                // Compose запросы на русском - прямой порядок (Compose ... компонент)
                "напиши[\\s\\p{L}\\d_-]*Compose[\\s\\p{L}\\d_-]*(?:функци|компонент|экран|ui|интерфейс)",
                "создай[\\s\\p{L}\\d_-]*Compose[\\s\\p{L}\\d_-]*(?:функци|компонент|экран|ui|интерфейс)",
                "реализуй[\\s\\p{L}\\d_-]*Compose[\\s\\p{L}\\d_-]*(?:функци|компонент|экран|ui|интерфейс)",
                "Compose[\\s\\p{L}\\d_-]*(?:функци|компонент|экран|ui)",
                // Compose запросы - обратный порядок (UI ... Compose)
                "напиши[\\s\\p{L}\\d_-]*(?:функци|компонент|экран|ui|интерфейс)[\\s\\p{L}\\d_-]*(?:на[\\s]+)?Compose",
                "создай[\\s\\p{L}\\d_-]*(?:функци|компонент|экран|ui|интерфейс)[\\s\\p{L}\\d_-]*(?:на[\\s]+)?Compose",
                "реализуй[\\s\\p{L}\\d_-]*(?:функци|компонент|экран|ui|интерфейс)[\\s\\p{L}\\d_-]*(?:на[\\s]+)?Compose",
                "(?:функци|компонент|экран|ui)[\\s\\p{L}\\d_-]*(?:на[\\s]+)?Compose",
                // GUI запросы
                "напиши[\\s\\p{L}\\d_-]*GUI[\\s\\p{L}\\d_-]*(?:приложение|интерфейс)",
                "создай[\\s\\p{L}\\d_-]*GUI[\\s\\p{L}\\d_-]*(?:приложение|интерфейс)",
                "графический[\\s\\p{L}\\d_-]*интерфейс",
                "напиши[\\s\\p{L}\\d_-]*графический",
                "создай[\\s\\p{L}\\d_-]*графический",
                // Desktop UI запросы
                "desktop[\\s\\p{L}\\d_-]*(?:приложение|ui|интерфейс)",
                "напиши[\\s\\p{L}\\d_-]*desktop",
                "создай[\\s\\p{L}\\d_-]*desktop",
                // Android UI запросы
                "напиши[\\s\\p{L}\\d_-]*(?:android|андроид)[\\s\\p{L}\\d_-]*(?:ui|интерфейс|экран)",
                "создай[\\s\\p{L}\\d_-]*(?:android|андроид)[\\s\\p{L}\\d_-]*(?:ui|интерфейс|экран)",
                // UI компоненты (включая функции)
                "напиши[\\s\\p{L}\\d_-]*UI[\\s\\p{L}\\d_-]*(?:компонент|элемент|кнопк|поле|форм|функци)",
                "создай[\\s\\p{L}\\d_-]*UI[\\s\\p{L}\\d_-]*(?:компонент|элемент|кнопк|поле|форм|функци)",
                // Screen/Activity
                "напиши[\\s\\p{L}\\d_-]*(?:экран|screen|activity)",
                "создай[\\s\\p{L}\\d_-]*(?:экран|screen|activity)"
            ),
            severity = ViolationSeverity.BLOCK,
            blockMessage = "Проект работает в CLI режиме, без Compose Desktop UI. " +
                    "Графический интерфейс не поддерживается. Пожалуйста, уточните запрос для CLI контекста.",
            checkTypes = setOf(CheckType.USER_REQUEST)
        ),

        // === TECHNOLOGY: CLI режим - AI RESPONSE ===
        ViolationPattern(
            invariantId = "sys_tech_cli",
            patterns = listOf(
                // Compose Desktop UI
                "@Composable",
                "ComposeDesktop",
                "DesktopWindow",
                "import[\\s\\w.]*androidx\\.compose",
                "import[\\s\\w.]*jetbrains\\.compose",
                "create[\\s\\w-]*UI[\\s\\w-]*Compose",
                "desktop[\\s\\w-]*application[\\s\\w-]*GUI",
                "Compose[\\s\\w-]*UI[\\s\\w-]*component",
                "Window\\s*\\{",
                "Column\\s*\\{",
                "Row\\s*\\{",
                "Box\\s*\\{",
                "Text\\s*\\(",
                "Button\\s*\\(",
                // Android UI
                "import[\\s\\w.]*android\\.(app|view|widget)",
                "android\\.widget\\.",
                "findViewById",
                "LayoutInflater"
            ),
            severity = ViolationSeverity.BLOCK,
            blockMessage = "Нарушение: проект работает в CLI режиме, без Compose Desktop UI. " +
                    "Графический интерфейс не поддерживается.",
            checkTypes = setOf(CheckType.AI_RESPONSE)
        ),

        // === STACK: Modern Kotlin ===
        ViolationPattern(
            invariantId = "sys_stack_modern",
            patterns = listOf(
                // Legacy Java concurrency
                "Thread\\.sleep\\s*\\(",
                "AsyncTask",
                "java\\.util\\.Timer",
                "TimerTask",
                "Handler\\s*\\(",
                "Looper",
                "synchronized\\s*\\(",
                // Deprecated patterns
                "@Deprecated",
                "System\\.exit",
                "Runtime\\.getRuntime\\(\\)\\.exec"
            ),
            severity = ViolationSeverity.WARN,
            blockMessage = "Предупреждение: обнаружен устаревший код. " +
                    "Используйте Kotlin Coroutines вместо Thread.sleep, Timer и других Java concurrency API.",
            checkTypes = setOf(CheckType.AI_RESPONSE)
        ),

        // === CLI Interface - USER REQUEST ===
        // Примечание: используем [\\s\\p{L}\\d_-] вместо [\\s\\w-] для поддержки кириллицы
        ViolationPattern(
            invariantId = "sys_cli_interface_request",
            patterns = listOf(
                // GUI framework requests
                "напиши[\\s\\p{L}\\d_-]*Swing[\\s\\p{L}\\d_-]*(?:приложение|окно|форм)",
                "создай[\\s\\p{L}\\d_-]*Swing[\\s\\p{L}\\d_-]*(?:приложение|окно|форм)",
                "напиши[\\s\\p{L}\\d_-]*JavaFX[\\s\\p{L}\\d_-]*(?:приложение|окно|форм)",
                "создай[\\s\\p{L}\\d_-]*JavaFX[\\s\\p{L}\\d_-]*(?:приложение|окно|форм)",
                // Window/Form requests
                "напиши[\\s\\p{L}\\d_-]*(?:окно|форму|диалог)[\\s\\p{L}\\d_-]*(?:с|для|в)",
                "создай[\\s\\p{L}\\d_-]*(?:окно|форму|диалог)[\\s\\p{L}\\d_-]*(?:с|для|в)"
            ),
            severity = ViolationSeverity.WARN,
            blockMessage = "Предупреждение: проект является CLI приложением. " +
                    "GUI компоненты (Swing, JavaFX) не требуются.",
            checkTypes = setOf(CheckType.USER_REQUEST)
        ),

        // === CLI: Command Line Interface - AI RESPONSE ===
        ViolationPattern(
            invariantId = "sys_cli_interface",
            patterns = listOf(
                // GUI components
                "JFrame",
                "JPanel",
                "JButton",
                "JLabel",
                "JTextField",
                "JTextArea",
                "SwingUtilities",
                "JavaFX",
                "Stage\\s*\\(",
                "Scene\\s*\\(",
                // Web frameworks
                "SpringBoot",
                "Ktor[\\s\\w-]*ApplicationEngine",
                "HttpServer",
                "EmbeddedServer"
            ),
            severity = ViolationSeverity.WARN,
            blockMessage = "Предупреждение: проект является CLI приложением. " +
                    "GUI компоненты (Swing, JavaFX) и веб-серверы не требуются.",
            checkTypes = setOf(CheckType.AI_RESPONSE)
        ),

        // === COROUTINES: Proper usage ===
        ViolationPattern(
            invariantId = "sys_coroutines",
            patterns = listOf(
                // Blocking calls in coroutines
                "runBlocking\\s*\\{",
                // Bad practice
                "GlobalScope\\.launch",
                "GlobalScope\\.async"
            ),
            severity = ViolationSeverity.WARN,
            blockMessage = "Предупреждение: обнаружены потенциально проблемные паттерны работы с корутинами. " +
                    "Избегайте runBlocking и GlobalScope в production коде.",
            checkTypes = setOf(CheckType.AI_RESPONSE)
        )
    )
}
