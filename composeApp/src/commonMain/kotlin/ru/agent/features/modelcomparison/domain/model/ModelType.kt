package ru.agent.features.modelcomparison.domain.model

/**
 * Represents different model tiers for comparison
 *
 * Models are selected from HuggingFace Inference Providers based on their parameter count:
 * - WEAK: Small models (1B-4B params) - fast but limited reasoning
 * - MEDIUM: Medium models (7B-8B params) - balanced performance
 * - STRONG: Large models (32B+ params) - best quality but slower
 *
 * Provider is specified after model name with colon: "model:provider"
 * Supported providers: novita, fireworks, together, cerebras, cohere
 *
 * Why weak models may give shorter/simpler responses:
 * 1. Limited context window - smaller memory for complex reasoning
 * 2. Less training data compressed into fewer parameters
 * 3. Simpler architecture - less capacity for nuanced understanding
 * 4. May "hallucinate" less but also provide less detailed analysis
 *
 * Note: Response length doesn't always correlate with quality!
 * Strong models can be more concise while being more accurate.
 */
enum class ModelType(
    val displayName: String,
    val description: String,
    val huggingFaceId: String,
    val huggingFaceUrl: String,
    val parameters: String,
    val architecture: String,
    val maxOutputTokens: Int,
    val temperature: Float,
    val inputCostPer1kTokens: Double,
    val outputCostPer1kTokens: Double
) {
    /**
     * Qwen3 4B Thinking - Small but capable model with reasoning
     * Good for: Simple Q&A, basic tasks, quick responses
     * Limitations: Limited reasoning depth compared to larger models
     */
    WEAK(
        displayName = "Weak",
        description = "Qwen3 4B Thinking - Small reasoning model",
        huggingFaceId = "Qwen/Qwen3-4B-Thinking-2507",
        huggingFaceUrl = "https://huggingface.co/Qwen/Qwen3-4B-Thinking-2507",
        parameters = "4B",
        architecture = "Qwen3",
        maxOutputTokens = 512,
        temperature = 0.7f,
        inputCostPer1kTokens = 0.0,
        outputCostPer1kTokens = 0.0
    ),

    /**
     * Qwen 2.5 7B - Balanced performance
     * Good for: General tasks, balanced quality/speed
     * Strong reasoning capabilities with efficient inference
     */
    MEDIUM(
        displayName = "Medium",
        description = "Qwen 2.5 7B - Balanced performance",
        huggingFaceId = "Qwen/Qwen2.5-7B-Instruct:together",
        huggingFaceUrl = "https://huggingface.co/Qwen/Qwen2.5-7B-Instruct",
        parameters = "7B",
        architecture = "Qwen2",
        maxOutputTokens = 512,
        temperature = 0.7f,
        inputCostPer1kTokens = 0.0,
        outputCostPer1kTokens = 0.0
    ),

    /**
     * DeepSeek R1 - Strong reasoning model
     * Good for: Complex reasoning, detailed analysis, code
     * Excellent chain-of-thought reasoning capabilities
     */
    STRONG(
        displayName = "Strong",
        description = "DeepSeek R1 - Advanced reasoning",
        huggingFaceId = "deepseek-ai/DeepSeek-R1:novita",
        huggingFaceUrl = "https://huggingface.co/deepseek-ai/DeepSeek-R1",
        parameters = "671B",
        architecture = "DeepSeek",
        maxOutputTokens = 1024,
        temperature = 0.7f,
        inputCostPer1kTokens = 0.0,
        outputCostPer1kTokens = 0.0
    );

    /**
     * Returns a formatted string with model info for display
     */
    fun getModelInfo(): String = "$displayName ($parameters)"

    /**
     * Returns markdown link to HuggingFace model page
     */
    fun getMarkdownLink(): String = "[$huggingFaceId]($huggingFaceUrl)"

    companion object {
        val DEFAULT_MODELS = listOf(WEAK, MEDIUM, STRONG)
    }
}
