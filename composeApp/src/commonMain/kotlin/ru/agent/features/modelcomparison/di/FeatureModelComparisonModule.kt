package ru.agent.features.modelcomparison.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.agent.features.modelcomparison.data.remote.HuggingFaceApiClient
import ru.agent.features.modelcomparison.data.repository.ModelComparisonRepositoryImpl
import ru.agent.features.modelcomparison.domain.repository.ModelComparisonRepository
import ru.agent.features.modelcomparison.domain.usecase.CompareModelsUseCase
import ru.agent.features.modelcomparison.domain.usecase.GenerateAnalysisUseCase
import ru.agent.features.modelcomparison.presentation.ModelComparisonViewModel

/**
 * Koin module for Model Comparison feature
 *
 * Requires HuggingFace API key provided via Koin qualifier "huggingface_api_key"
 * Get your free API key at: https://huggingface.co/settings/tokens
 *
 * Note: Platform-specific modules (jvmMain, etc.) should provide the API key
 * from environment variables or build config.
 */
val featureModelComparisonModule = module {
    // API Client for HuggingFace Inference Providers API
    single {
        HuggingFaceApiClient(
            httpClient = get(),
            apiKey = getOrNull<String>(qualifier = named("huggingface_api_key"))
                ?: "" // Will fail gracefully if no API key - platform modules should provide this
        )
    }

    // Repository
    singleOf(::ModelComparisonRepositoryImpl) bind ModelComparisonRepository::class

    // Use Cases
    singleOf(::CompareModelsUseCase)
    singleOf(::GenerateAnalysisUseCase)

    // ViewModel
    viewModelOf(::ModelComparisonViewModel)
}
