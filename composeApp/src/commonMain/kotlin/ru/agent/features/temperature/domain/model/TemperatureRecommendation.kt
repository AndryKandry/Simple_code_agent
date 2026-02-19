package ru.agent.features.temperature.domain.model

/**
 * Recommendation for using a specific temperature level
 */
data class TemperatureRecommendation(
    val level: TemperatureLevel,
    val bestFor: List<String>,
    val examples: List<String>,
    val pros: List<String>,
    val cons: List<String>
) {
    companion object {
        val DEFAULT_RECOMMENDATIONS = listOf(
            TemperatureRecommendation(
                level = TemperatureLevel.LOW,
                bestFor = listOf(
                    "Factual Q&A",
                    "Code generation",
                    "Data extraction",
                    "Classification tasks",
                    "Consistent outputs needed"
                ),
                examples = listOf(
                    "What is the capital of France?",
                    "Extract all dates from this text",
                    "Classify this email as spam or not"
                ),
                pros = listOf(
                    "Consistent, reproducible results",
                    "More factual and precise",
                    "Less hallucination"
                ),
                cons = listOf(
                    "Less creative",
                    "Can be repetitive",
                    "Limited variety"
                )
            ),
            TemperatureRecommendation(
                level = TemperatureLevel.MEDIUM,
                bestFor = listOf(
                    "General conversation",
                    "Content writing",
                    "Translation",
                    "Summarization",
                    "Balanced responses"
                ),
                examples = listOf(
                    "Explain quantum computing to a beginner",
                    "Write a product description",
                    "Summarize this article"
                ),
                pros = listOf(
                    "Good balance of creativity and accuracy",
                    "Natural-sounding responses",
                    "Versatile for most tasks"
                ),
                cons = listOf(
                    "May not be optimal for extremes",
                    "Can vary between runs"
                )
            ),
            TemperatureRecommendation(
                level = TemperatureLevel.HIGH,
                bestFor = listOf(
                    "Creative writing",
                    "Brainstorming",
                    "Story generation",
                    "Marketing copy",
                    "Idea generation"
                ),
                examples = listOf(
                    "Write a sci-fi story about AI",
                    "Brainstorm startup ideas",
                    "Create a catchy slogan"
                ),
                pros = listOf(
                    "Highly creative and diverse",
                    "Unique outputs each time",
                    "Great for ideation"
                ),
                cons = listOf(
                    "Less predictable",
                    "May include inaccuracies",
                    "Can be too experimental"
                )
            )
        )
    }
}
