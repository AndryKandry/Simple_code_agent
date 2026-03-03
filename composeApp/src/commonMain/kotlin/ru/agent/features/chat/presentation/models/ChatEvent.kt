package ru.agent.features.chat.presentation.models

import ru.agent.features.memory.domain.model.KnowledgeEntry
import ru.agent.features.memory.presentation.models.MemoryEvent

sealed class ChatEvent {
    data class SendMessage(val text: String) : ChatEvent()
    data class InputTextChanged(val text: String) : ChatEvent()
    data class SelectSession(val sessionId: String) : ChatEvent()
    object CreateNewSession : ChatEvent()
    data class DeleteSession(val sessionId: String) : ChatEvent()
    object ClearError : ChatEvent()
    data class ClearHistory(val sessionId: String) : ChatEvent()
    object ToggleSidebar : ChatEvent()
    data class LoadSession(val sessionId: String) : ChatEvent()

    // Memory Panel Events
    object ToggleMemoryPanel : ChatEvent()
    data class MemoryEventWrapper(val memoryEvent: MemoryEvent) : ChatEvent()
    data class SearchKnowledge(val query: String) : ChatEvent()
    data class SaveToKnowledge(val entry: KnowledgeEntry) : ChatEvent()
}
