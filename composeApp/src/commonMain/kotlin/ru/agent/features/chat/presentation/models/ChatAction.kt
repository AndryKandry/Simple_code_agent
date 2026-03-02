package ru.agent.features.chat.presentation.models

sealed class ChatAction {
    object ScrollToBottom : ChatAction()
    data class ShowError(val message: String) : ChatAction()
    data class NavigateToSession(val sessionId: String) : ChatAction()
    object ShowDeleteConfirmation : ChatAction()
    object ShowStrategyChangeWarning : ChatAction()
    data class ShowStrategyInfo(val message: String) : ChatAction()

    // Sticky Facts Actions
    data class ShowFactExtracted(val factCount: Int) : ChatAction()
    data class ShowFactsCleared(val count: Int) : ChatAction()
    object ShowFactsError : ChatAction()

    // Branching Actions
    data class ShowCheckpointCreated(val checkpointName: String) : ChatAction()
    data class ShowBranchCreated(val branchName: String) : ChatAction()
    data class ShowBranchSwitched(val branchName: String) : ChatAction()
    data class ShowBranchDeleted(val branchName: String) : ChatAction()
    data class ShowCheckpointDeleted(val checkpointName: String) : ChatAction()
    object ShowBranchingError : ChatAction()
    object ShowEmptyCheckpointWarning : ChatAction()

    // Comparison Mode Actions
    object NavigateToComparisonMode : ChatAction()
}
