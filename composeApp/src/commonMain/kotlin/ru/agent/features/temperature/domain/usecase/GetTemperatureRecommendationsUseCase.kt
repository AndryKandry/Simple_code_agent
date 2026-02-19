package ru.agent.features.temperature.domain.usecase

import ru.agent.features.temperature.domain.model.TemperatureComparisonResult
import ru.agent.features.temperature.domain.model.TemperatureRecommendation
import ru.agent.features.temperature.domain.model.TemperatureResponse

/**
 * UseCase for generating temperature recommendations based on comparison results
 */
class GetTemperatureRecommendationsUseCase {

    /**
     * Get recommendations based on comparison result
     *
     * @param result The comparison result to analyze
     * @return List of recommendations
     */
    operator fun invoke(
        result: TemperatureComparisonResult?
    ): List<TemperatureRecommendation> {
        if (result == null) {
            return TemperatureRecommendation.DEFAULT_RECOMMENDATIONS
        }

        // Return default recommendations enhanced with analysis
        return TemperatureRecommendation.DEFAULT_RECOMMENDATIONS.map { rec ->
            val response = result.responses[rec.level]
            if (response != null && response.isSuccess) {
                rec.copy(
                    pros = rec.pros + analyzeStrengths(response),
                    cons = rec.cons + analyzeWeaknesses(response)
                )
            } else {
                rec
            }
        }
    }

    private fun analyzeStrengths(response: TemperatureResponse): List<String> {
        val strengths = mutableListOf<String>()
        val content = response.content.lowercase()

        // Check for factual indicators
        if (content.contains("according to") || content.contains("specifically")) {
            strengths.add("Shows factual precision")
        }

        // Check for creativity indicators
        if (content.length > 500) {
            strengths.add("Demonstrates elaboration")
        }

        return strengths
    }

    private fun analyzeWeaknesses(response: TemperatureResponse): List<String> {
        // Could analyze for repetition, short responses, etc.
        return emptyList()
    }
}
