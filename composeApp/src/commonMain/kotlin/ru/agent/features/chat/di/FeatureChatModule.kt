package ru.agent.features.chat.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.agent.core.database.AppDatabase
import ru.agent.features.chat.data.local.dao.ChatSessionDao
import ru.agent.features.chat.data.local.dao.MessageDao
import ru.agent.features.chat.data.remote.DeepSeekApiClient
import ru.agent.features.chat.data.remote.LlmApiClient
import ru.agent.features.chat.data.remote.LlmClientFactory
import ru.agent.features.chat.data.remote.LlmConfiguration
import ru.agent.features.chat.data.remote.OllamaApiClient
import ru.agent.features.chat.data.repository.ChatSessionRepositoryImpl
import ru.agent.features.chat.domain.optimization.ContextOptimizer
import ru.agent.features.chat.domain.repository.ChatSessionRepository
import ru.agent.features.chat.domain.usecase.ClearChatHistoryUseCase
import ru.agent.features.chat.domain.usecase.CreateChatSessionUseCase
import ru.agent.features.chat.domain.usecase.DeleteChatSessionUseCase
import ru.agent.features.chat.domain.usecase.GetAllChatSessionsUseCase
import ru.agent.features.chat.domain.usecase.GetChatHistoryUseCase
import ru.agent.features.chat.domain.usecase.GetOptimizedContextUseCase
import ru.agent.features.chat.domain.usecase.SaveMessageUseCase
import ru.agent.features.chat.domain.usecase.SendMessageUseCase
import ru.agent.features.chat.domain.usecase.SendSilentMessageUseCase
import ru.agent.features.chat.presentation.ChatViewModel

/**
 * Koin DI module for Chat feature (common code).
 *
 * Note: ChatRepository registration is in FeatureChatJvmModule because
 * ChatRepositoryImpl depends on JVM-specific McpOrchestrator.
 */
val featureChatModule = module {
    // DAOs
    single<ChatSessionDao> { get<AppDatabase>().getChatSessionDao() }
    single<MessageDao> { get<AppDatabase>().getMessageDao() }

    // LLM Configuration
    single { LlmConfiguration.fromEnvironment() }

    // LLM API Clients
    single {
        DeepSeekApiClient(
            httpClient = get(),
            apiKey = get<String>(qualifier = named("deepseek_api_key"))
        )
    }

    single {
        val config = get<LlmConfiguration>()
        OllamaApiClient(
            httpClient = get(),
            baseUrl = config.ollamaBaseUrl,
            model = config.ollamaModel
        )
    }

    // LLM Client Factory
    single {
        LlmClientFactory(
            deepSeekClient = get(),
            ollamaClient = get(),
            config = get()
        )
    }

    // Main LLM Client (based on configuration)
    single<LlmApiClient> { get<LlmClientFactory>().createClient() }

    // Token Optimization
    single { ContextOptimizer(maxTokens = 4000, keepRecentMessages = 4) }

    // Repositories (ChatRepositoryImpl is registered in FeatureChatJvmModule)
    singleOf(::ChatSessionRepositoryImpl) bind ChatSessionRepository::class

    // Use Cases
    singleOf(::SendMessageUseCase)
    singleOf(::GetChatHistoryUseCase)
    singleOf(::ClearChatHistoryUseCase)
    singleOf(::CreateChatSessionUseCase)
    singleOf(::GetAllChatSessionsUseCase)
    singleOf(::DeleteChatSessionUseCase)
    singleOf(::GetOptimizedContextUseCase)
    singleOf(::SendSilentMessageUseCase)
    singleOf(::SaveMessageUseCase)

    // ViewModel
    viewModel {
        ChatViewModel(
            sendMessageUseCase = get(),
            sendSilentMessageUseCase = get(),
            saveMessageUseCase = get(),
            getChatHistoryUseCase = get(),
            clearChatHistoryUseCase = get(),
            getAllChatSessionsUseCase = get(),
            createChatSessionUseCase = get(),
            deleteChatSessionUseCase = get(),
            addMessageToMemoryUseCase = get(),
            clearShortTermMemoryUseCase = get(),
            createDefaultProfileUseCase = get(),
            // Task use cases
            getTaskStateUseCase = get(),
            createTaskFromMessageUseCase = get(),
            generateTaskPlanUseCase = get(),
            validateTaskResultUseCase = get(),
            pauseTaskUseCase = get(),
            resumeTaskUseCase = get(),
            cancelTaskUseCase = get(),
            transitionTaskStageUseCase = get(),
            updateTaskStateUseCase = get()
        )
    }
}
