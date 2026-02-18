package ru.agent.features.reasoning.domain.model

/**
 * Status of a reasoning request
 */
enum class ReasoningStatus {
    IDLE,
    LOADING,
    SUCCESS,
    ERROR
}
