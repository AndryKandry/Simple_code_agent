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
    object OpenProfileSettings : ChatEvent()
    object CloseProfileSettings : ChatEvent()
    object ProfileUpdated : ChatEvent()
    object ResetProfileToDefaults : ChatEvent()

    // Task events
    object PauseTask : ChatEvent()
    object ResumeTask : ChatEvent()
    object CancelTask : ChatEvent()
    object ToggleTaskPanel : ChatEvent()
    object AdvanceTaskStage : ChatEvent()

    // Dialog-based task approval events
    object ApprovePlan : ChatEvent()      // Approve plan in PLANNING -> EXECUTION
    data class RejectPlan(val feedback: String) : ChatEvent()  // Reject plan with feedback
    object ApproveResult : ChatEvent()    // Approve result in VALIDATION -> DONE
    data class RejectResult(val feedback: String) : ChatEvent() // Reject result with feedback -> EXECUTION (retry)
}
