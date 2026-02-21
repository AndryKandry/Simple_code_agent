package ru.agent.features.main.presentation.models

sealed interface MainAction {
    data object ExampleAction : MainAction
    data object OpenChatScreen : MainAction
    data object OpenComparisonScreen : MainAction
    data object OpenReasoningScreen : MainAction
    data object OpenTemperatureScreen : MainAction
    data object OpenModelComparisonScreen : MainAction
//    data object OpenPersonagesScreen : MainAction
//    data object OpenGamerBookScreen : MainAction
}
