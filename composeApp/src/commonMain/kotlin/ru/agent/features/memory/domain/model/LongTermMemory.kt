package ru.agent.features.memory.domain.model

/**
 * Long-term Memory (LTM) - долгосрочная память.
 *
 * Хранит:
 * - Профиль пользователя (UserProfile)
 * - Базу знаний (KnowledgeEntry)
 * - Контекстные якоря (ContextAnchor)
 *
 * Персистентно хранится в Room Database.
 */

/**
 * Профиль пользователя.
 *
 * @property id Уникальный идентификатор
 * @property name Имя пользователя
 * @property role Роль пользователя (developer, manager, designer, etc.)
 * @property context Дополнительный контекст о пользователе
 * @property preferences Настройки пользователя (JSON)
 * @property interactionStats Статистика взаимодействий
 * @property createdAt Время создания
 * @property updatedAt Время последнего обновления
 */
data class UserProfile(
    val id: String,
    val name: String = "User",
    val role: String = "developer",
    val context: String = "",
    val preferences: UserPreferences = UserPreferences(),
    val interactionStats: InteractionStats = InteractionStats(),
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Настройки пользователя.
 */
data class UserPreferences(
    val preferredLanguage: String = "kotlin",
    val codeStyle: CodeStyle = CodeStyle(),
    val theme: String = "dark",
    val responseVerbosity: ResponseVerbosity = ResponseVerbosity.NORMAL,
    val customInstructions: List<String> = emptyList()
)

/**
 * Стиль кода.
 */
data class CodeStyle(
    val indentSize: Int = 4,
    val useTabs: Boolean = false,
    val maxLineLength: Int = 120,
    val trailingComma: Boolean = true
)

/**
 * Уровень детализации ответов.
 */
enum class ResponseVerbosity {
    CONCISE,    // Краткий
    NORMAL,     // Обычный
    DETAILED    // Подробный
}

/**
 * Статистика взаимодействий.
 */
data class InteractionStats(
    val totalMessages: Long = 0,
    val totalSessions: Long = 0,
    val totalTasksCompleted: Long = 0,
    val averageSessionLength: Float = 0f,
    val mostUsedTaskTypes: Map<String, Int> = emptyMap(),
    val lastActiveAt: Long? = null
)

/**
 * Запись в базе знаний.
 *
 * @property id Уникальный идентификатор
 * @property key Ключ записи
 * @property value Значение (контент)
 * @property category Категория знаний
 * @property tags Теги для поиска
 * @property relevanceScore Оценка релевантности (0.0 - 1.0)
 * @property accessCount Количество обращений
 * @property lastAccessedAt Время последнего обращения
 * @property createdAt Время создания
 * @property updatedAt Время последнего обновления
 * @property expiresAt Время истечения (null = бессрочно)
 */
data class KnowledgeEntry(
    val id: String,
    val key: String,
    val value: String,
    val category: KnowledgeCategory = KnowledgeCategory.GENERAL,
    val tags: List<String> = emptyList(),
    val relevanceScore: Float = 1.0f,
    val accessCount: Int = 0,
    val lastAccessedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val expiresAt: Long? = null
)

/**
 * Категория знаний.
 */
enum class KnowledgeCategory {
    GENERAL,        // Общие знания
    CODE_PATTERN,   // Паттерны кода
    API_REFERENCE,  // Справка по API
    PROJECT_INFO,   // Информация о проекте
    USER_PREFERENCE,// Предпочтения пользователя
    ERROR_SOLUTION, // Решения ошибок
    WORKFLOW,       // Рабочие процессы
    SNIPPET         // Сниппеты кода
}

/**
 * Контекстный якорь.
 *
 * Связывает контекст с определенным файлом, директорией или темой.
 *
 * @property id Уникальный идентификатор
 * @property name Название якоря
 * @property type Тип якоря
 * @property path Путь к файлу/директории (для FILE/DIRECTORY типов)
 * @property topic Тема (для TOPIC типа)
 * @property context Контекстная информация (JSON)
 * @property priority Приоритет (влияет на порядок использования)
 * @property isActive Активен ли якорь
 * @property createdAt Время создания
 * @property updatedAt Время последнего обновления
 * @property lastUsedAt Время последнего использования
 */
data class ContextAnchor(
    val id: String,
    val name: String,
    val type: AnchorType,
    val path: String? = null,
    val topic: String? = null,
    val context: String? = null,
    val priority: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
    val lastUsedAt: Long? = null
)

/**
 * Тип контекстного якоря.
 */
enum class AnchorType {
    FILE,       // Файл
    DIRECTORY,  // Директория
    TOPIC,      // Тема обсуждения
    TASK,       // Задача
    SESSION     // Сессия
}
