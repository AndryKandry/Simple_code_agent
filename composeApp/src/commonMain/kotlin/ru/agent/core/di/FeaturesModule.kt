package ru.agent.core.di

import org.koin.dsl.module
import ru.agent.features.chat.di.featureChatModule
import ru.agent.features.invariant.di.featureInvariantModule
import ru.agent.features.main.di.featureMainModule
import ru.agent.features.memory.di.featureMemoryModule
import ru.agent.features.profile.di.featureProfileModule
import ru.agent.features.rag.di.featureRagModule
import ru.agent.features.scheduler.di.featureSchedulerModule
import ru.agent.features.task.di.featureTaskModule

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

        // Task
        featureTaskModule,

        // Invariant
        featureInvariantModule,

        // Scheduler
        featureSchedulerModule,

        // RAG (Retrieval Augmented Generation)
        featureRagModule,

        // Note: mcpModule is defined in jvmMain and should be loaded separately for CLI applications
        // It's not included here because it depends on JVM-specific components
    )
}

// Note: cliModule is defined in jvmMain and should be loaded separately for CLI applications
// It's not included here because it depends on JVM-specific components
