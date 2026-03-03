package ru.agent.features.memory.domain.model

/**
 * Working Memory (WM) - рабочая память.
 *
 * Хранит данные текущей задачи и временные структуры данных.
 * Персистентно хранится в Room Database.
 *
 * @property id Уникальный идентификатор записи
 * @property sessionId ID сессии чата, к которой относится
 * @property taskInfo Информация о текущей задаче
 * @property executionState Состояние выполнения
 * @property temporaryData Временные данные (JSON-строка)
 * @property createdAt Время создания
 * @property updatedAt Время последнего обновления
 */
data class WorkingMemory(
    val id: String,
    val sessionId: String,
    val taskInfo: TaskInfo? = null,
    val executionState: ExecutionState = ExecutionState.IDLE,
    val temporaryData: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Информация о текущей задаче.
 */
data class TaskInfo(
    val taskId: String,
    val taskType: TaskType,
    val description: String,
    val status: TaskStatus,
    val progress: Float = 0f, // 0.0 - 1.0
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val parentTaskId: String? = null
)

/**
 * Тип задачи.
 */
enum class TaskType {
    CODE_GENERATION,
    CODE_REVIEW,
    REFACTORING,
    DEBUGGING,
    DOCUMENTATION,
    ANALYSIS,
    FILE_OPERATION,
    GIT_OPERATION,
    SEARCH,
    OTHER
}

/**
 * Статус задачи.
 */
enum class TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Состояние выполнения.
 */
enum class ExecutionState {
    IDLE,           // Простой
    THINKING,       // Анализ запроса
    EXECUTING,      // Выполнение действия
    WAITING_INPUT,  // Ожидание ввода пользователя
    ERROR           // Ошибка
}
