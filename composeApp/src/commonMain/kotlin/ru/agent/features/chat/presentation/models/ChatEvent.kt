package ru.agent.features.chat.presentation.models

import ru.agent.features.chat.domain.model.ContextStrategy
import ru.agent.features.chat.domain.model.Fact
import ru.agent.features.chat.domain.model.FactCategory

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

    // Context Strategy Events
    data class SelectStrategy(val strategy: ContextStrategy) : ChatEvent()
    data class UpdateSlidingWindowSize(val size: Int) : ChatEvent()
    object ConfirmStrategyChange : ChatEvent()
    object CancelStrategyChange : ChatEvent()
    object DismissStrategyWarning : ChatEvent()

    // Sticky Facts Events
    object ToggleFactsPanel : ChatEvent()
    data class DeleteFact(val factId: String) : ChatEvent()
    data class UpdateFact(val fact: Fact) : ChatEvent()
    object ClearAllFacts : ChatEvent()
    data class ClearFactsByCategory(val category: FactCategory) : ChatEvent()
    object RefreshFacts : ChatEvent()

    // Branching Events
    object ToggleBranchTree : ChatEvent()
    data class CreateCheckpoint(val name: String) : ChatEvent()
    data class CreateBranch(val checkpointId: String, val name: String) : ChatEvent()
    data class SwitchBranch(val checkpointId: String?) : ChatEvent()
    data class DeleteBranch(val branchId: String) : ChatEvent()
    data class DeleteCheckpoint(val checkpointId: String) : ChatEvent()
    object ShowCreateCheckpointDialog : ChatEvent()
    object DismissCreateCheckpointDialog : ChatEvent()
    data class ShowCreateBranchDialog(val checkpointId: String) : ChatEvent()
    object DismissCreateBranchDialog : ChatEvent()
    object SwitchToMainBranch : ChatEvent()

    // Comparison Mode Events
    object OpenComparisonMode : ChatEvent()
}
