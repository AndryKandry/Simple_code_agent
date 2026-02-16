package ru.agent.core.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.agent.core.handlers.NetworkErrorHandling
import ru.agent.core.handlers.NetworkErrorHandlingImpl

val coreModule = module {
    singleOf(::NetworkErrorHandlingImpl) bind NetworkErrorHandling::class
}
