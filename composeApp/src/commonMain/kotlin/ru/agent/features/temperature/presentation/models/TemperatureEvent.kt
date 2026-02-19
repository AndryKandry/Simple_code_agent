package ru.agent.features.temperature.presentation.models

import ru.agent.features.temperature.domain.model.TemperatureLevel

/**
 * Events triggered from UI to ViewModel
 */
sealed interface TemperatureEvent {
    data class InputTextChanged(val text: String) : TemperatureEvent
    data object Compare : TemperatureEvent
    data object Retry : TemperatureEvent
    data object ClearInput : TemperatureEvent
    data object ClearResults : TemperatureEvent

    data class ToggleTemperatureLevel(val level: TemperatureLevel) : TemperatureEvent
    data class SetMaxTokens(val tokens: Int) : TemperatureEvent

    data object SaveResults : TemperatureEvent
    data object ExportMarkdown : TemperatureEvent
    data object ExportJson : TemperatureEvent

    data object ToggleHistory : TemperatureEvent
    data class LoadHistoryItem(val resultId: String) : TemperatureEvent

    data object ClearError : TemperatureEvent
}
