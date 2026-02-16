package ru.agent.features.main.presentation.models

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

@Immutable
data class MainMenuItemInfo(
    val titleRes: StringResource,
    val imageRes: DrawableResource,
    val event: MainEvent,
)
