package ru.agent.features.main.presentation.models

sealed interface MainEvent {
    data object ExampleEvent : MainEvent
    data object ChatClicked : MainEvent
//    data object PersonagesClicked : MainEvent
//    data object GamerBookClicked : MainEvent
}
