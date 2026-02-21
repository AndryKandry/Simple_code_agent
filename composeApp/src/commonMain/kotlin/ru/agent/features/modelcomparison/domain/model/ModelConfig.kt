package ru.agent.features.modelcomparison.domain.model

/**
 * Configuration for model comparison
 */
data class ModelConfig(
    val maxTokens: Int = 1000,
    val temperature: Double = 0.7,
    val models: List<ModelType> = ModelType.DEFAULT_MODELS
) {
    companion object {
        val DEFAULT = ModelConfig()
    }
}
