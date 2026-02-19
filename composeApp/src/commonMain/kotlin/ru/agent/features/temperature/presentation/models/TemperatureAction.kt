package ru.agent.features.temperature.presentation.models

/**
 * Actions triggered from ViewModel to UI
 */
sealed interface TemperatureAction {
    data class ShowError(val message: String) : TemperatureAction
    data class ShowSaveSuccess(val filePath: String) : TemperatureAction
    data class CopyToClipboard(val content: String) : TemperatureAction
    data object ScrollToResults : TemperatureAction
    data object ShowHistory : TemperatureAction
    data object HideHistory : TemperatureAction
}
