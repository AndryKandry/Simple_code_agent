package ru.agent.features.temperature.domain.model

/**
 * Predefined temperature levels for comparison
 */
enum class TemperatureLevel(
    val value: Double,
    val displayName: String,
    val description: String,
    val colorHex: String
) {
    LOW(
        value = 0.0,
        displayName = "Deterministic",
        description = "Consistent, factual responses",
        colorHex = "#4CAF50"  // Green
    ),
    MEDIUM(
        value = 0.7,
        displayName = "Balanced",
        description = "Good for general tasks",
        colorHex = "#2196F3"  // Blue
    ),
    HIGH(
        value = 1.2,
        displayName = "Creative",
        description = "Diverse, creative outputs",
        colorHex = "#FF9800"  // Orange
    );

    companion object {
        val DEFAULT_LEVELS = listOf(LOW, MEDIUM, HIGH)

        fun fromValue(value: Double): TemperatureLevel? {
            return entries.find { it.value == value }
        }
    }
}
