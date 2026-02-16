package ru.agent.features.main.presentation.models

import androidx.compose.runtime.Immutable

@Immutable
data class MainViewState(
    val items: List<MainMenuItemInfo> = emptyList()
)
