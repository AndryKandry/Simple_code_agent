package ru.agent.features.memory.presentation.models

import ru.agent.features.chat.domain.model.Message
import ru.agent.features.memory.domain.model.ContextAnchor
import ru.agent.features.memory.domain.model.ExecutionState
import ru.agent.features.memory.domain.model.KnowledgeEntry
import ru.agent.features.memory.domain.model.TaskInfo

/**
 * UI State для Memory Panel.
 *
 * @property isMemoryPanelOpen Флаг открытости панели памяти
 * @property sessionId ID текущей сессии
 * @property lastMessages Последние сообщения из STM
 * @property activeTask Активная задача из WM
 * @property executionState Состояние выполнения
 * @property searchResults Результаты поиска по базе знаний
 * @property activeAnchors Активные контекстные якоря
 * @property searchQuery Текущий поисковый запрос
 */
data class MemoryState(
    val isMemoryPanelOpen: Boolean = false,
    val sessionId: String? = null,
    val lastMessages: List<Message> = emptyList(),
    val activeTask: TaskInfo? = null,
    val executionState: ExecutionState = ExecutionState.IDLE,
    val searchResults: List<KnowledgeEntry> = emptyList(),
    val activeAnchors: List<ContextAnchor> = emptyList(),
    val searchQuery: String = ""
)

/**
 * Events для Memory Panel.
 */
sealed class MemoryEvent {
    /**
     * Переключить видимость панели памяти.
     */
    data object ToggleMemoryPanelVisibility : MemoryEvent()

    /**
     * Очистить панель памяти для конкретной сессии.
     *
     * @property sessionId ID сессии
     */
    data class ClearMemoryPanel(val sessionId: String) : MemoryEvent()

    /**
     * Поиск по базе знаний.
     *
     * @property query Поисковый запрос
     */
    data class SearchKnowledge(val query: String) : MemoryEvent()

    /**
     * Сохранить запись в базу знаний.
     *
     * @property entry Запись для сохранения
     */
    data class SaveToKnowledge(val entry: KnowledgeEntry) : MemoryEvent()
}
