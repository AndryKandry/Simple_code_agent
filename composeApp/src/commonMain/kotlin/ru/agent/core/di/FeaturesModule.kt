package ru.agent.core.di

import org.koin.dsl.module
import ru.agent.features.chat.di.featureChatModule
import ru.agent.features.comparison.di.featureComparisonModule
import ru.agent.features.main.di.featureMainModule

val featuresModule = module {
    includes(
        coreModule,

        // Main menu
        featureMainModule,

        // Chat
        featureChatModule,

        // API Comparison
        featureComparisonModule,
    )
}
