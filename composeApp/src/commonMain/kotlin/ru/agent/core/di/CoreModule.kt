package ru.agent.core.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.agent.core.handlers.NetworkErrorHandling
import ru.agent.core.handlers.NetworkErrorHandlingImpl
import ru.agent.core.network.createHttpClient

val coreModule = module {
    singleOf(::NetworkErrorHandlingImpl) bind NetworkErrorHandling::class
    single { createHttpClient() }

    // DeepSeek API Key - should be provided from environment or build config
    single(qualifier = named("deepseek_api_key")) {
        getDeepSeekApiKey()
    }
}

expect fun getDeepSeekApiKey(): String
