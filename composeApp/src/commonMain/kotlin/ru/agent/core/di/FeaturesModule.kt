package ru.agent.core.di

import org.koin.dsl.module
import ru.agent.features.main.di.featureMainModule

val featuresModule = module {
    includes(
        coreModule,

        // Main menu
        featureMainModule,
    )
}
