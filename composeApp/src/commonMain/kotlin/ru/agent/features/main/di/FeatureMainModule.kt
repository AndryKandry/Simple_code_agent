package ru.agent.features.main.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ru.agent.features.main.domain.usecases.ExampleUseCase
import ru.agent.features.main.presentation.MainViewModel

val featureMainModule = module {
    viewModelOf(::MainViewModel)

    singleOf(::ExampleUseCase)
}
