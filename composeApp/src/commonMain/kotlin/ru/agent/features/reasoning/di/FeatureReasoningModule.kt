package ru.agent.features.reasoning.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.agent.features.reasoning.data.repository.ReasoningRepositoryImpl
import ru.agent.features.reasoning.domain.repository.ReasoningRepository
import ru.agent.features.reasoning.domain.usecase.CompareReasoningMethodsUseCase
import ru.agent.features.reasoning.presentation.ReasoningViewModel

/**
 * Koin module for Reasoning Comparison feature
 */
val featureReasoningModule = module {
    // Repository - uses DeepSeekApiClient from chat feature
    singleOf(::ReasoningRepositoryImpl) bind ReasoningRepository::class

    // Use Cases
    singleOf(::CompareReasoningMethodsUseCase)

    // ViewModel
    viewModelOf(::ReasoningViewModel)
}
