package ru.agent.core.di

import org.koin.dsl.module
import ru.agent.features.chat.di.featureChatModule
import ru.agent.features.main.di.featureMainModule
import ru.agent.features.memory.di.featureMemoryModule
import ru.agent.features.profile.di.featureProfileModule

val featuresModule = module {
    includes(
        coreModule,

        // Main menu
        featureMainModule,

        // Chat
        featureChatModule,

        // Memory
        featureMemoryModule,

        // Profile
        featureProfileModule,
    )
}
