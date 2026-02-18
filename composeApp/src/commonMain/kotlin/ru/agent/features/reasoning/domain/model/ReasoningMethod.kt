package ru.agent.features.reasoning.domain.model

/**
 * Enum representing different reasoning methods for AI responses
 */
enum class ReasoningMethod(
    val displayName: String,
    val description: String,
    val systemPrompt: String,
    val colorHex: String
) {
    DIRECT(
        displayName = "Direct Answer",
        description = "Without instructions",
        systemPrompt = "",
        colorHex = "#4CAF50"
    ),
    STEP_BY_STEP(
        displayName = "Step by Step",
        description = "Solve step by step",
        systemPrompt = "Solve the problem step by step. Break the solution into logical steps and explain each step.",
        colorHex = "#2196F3"
    ),
    META_PROMPT(
        displayName = "Meta Prompt",
        description = "First create prompt",
        systemPrompt = "Before solving, create an optimal prompt. Then use it to solve the problem.",
        colorHex = "#FF9800"
    ),
    EXPERTS(
        displayName = "Expert Panel",
        description = "Group of experts",
        systemPrompt = "You are a group of experts: Analyst, Engineer, Critic. Each one provides their opinion in turn.",
        colorHex = "#9C27B0"
    )
}
