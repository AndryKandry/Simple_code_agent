package ru.agent.core.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

fun initKoin(
    setupContext: KoinApplication.() -> Unit = {},
    appModule: Module = module {}
) {
    startKoin {
        setupContext()

        modules(appModule +
                platformModule +
                featuresModule
        )
    }
}

fun createEmptyModule(): Module = module {}

expect val platformModule: Module
