package ru.agent.features.chat.presentation.models

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
    // Token-related events
    object DismissTokenWarning : ChatEvent()
    object RecalculateTokens : ChatEvent()
    object ToggleTokenHints : ChatEvent()
    // Batch message navigation events (for split long messages)
    data class ToggleBatchExpansion(val batchId: String) : ChatEvent()
    data class NavigateBatchPart(val batchId: String, val direction: Int, val totalParts: Int) : ChatEvent()
    data class ExpandBatch(val batchId: String) : ChatEvent()
    data class CollapseBatch(val batchId: String) : ChatEvent()
}
