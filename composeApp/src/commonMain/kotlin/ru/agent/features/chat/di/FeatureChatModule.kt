package ru.agent.features.chat.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.agent.core.database.AppDatabase
import ru.agent.features.chat.data.local.dao.ChatSessionDao
import ru.agent.features.chat.data.local.dao.MessageDao
import ru.agent.features.chat.data.local.dao.MessageSummaryDao
import ru.agent.features.chat.data.local.dao.TokenUsageDao
import ru.agent.features.chat.data.remote.DeepSeekApiClient
import ru.agent.features.chat.data.remote.DeepSeekSummaryApi
import ru.agent.features.chat.data.repository.ChatRepositoryImpl
import ru.agent.features.chat.data.repository.ChatSessionRepositoryImpl
import ru.agent.features.chat.data.repository.MessageSummaryRepositoryImpl
import ru.agent.features.chat.data.repository.TokenMetricsRepositoryImpl
import ru.agent.features.chat.domain.model.CompressionConfig
import ru.agent.features.chat.domain.optimization.ContextOptimizer
import ru.agent.features.chat.domain.optimization.SummaryGenerator
import ru.agent.features.chat.domain.repository.ChatRepository
import ru.agent.features.chat.domain.repository.ChatSessionRepository
import ru.agent.features.chat.domain.repository.MessageSummaryRepository
import ru.agent.features.chat.domain.repository.SummaryApi
import ru.agent.features.chat.domain.repository.TokenMetricsRepository
import ru.agent.features.chat.domain.usecase.ClearChatHistoryUseCase
import ru.agent.features.chat.domain.usecase.CompressHistoryUseCase
import ru.agent.features.chat.domain.usecase.CreateChatSessionUseCase
import ru.agent.features.chat.domain.usecase.DeleteChatSessionUseCase
import ru.agent.features.chat.domain.usecase.GenerateSummaryUseCase
import ru.agent.features.chat.domain.usecase.GetAllChatSessionsUseCase
import ru.agent.features.chat.domain.usecase.GetChatHistoryUseCase
import ru.agent.features.chat.domain.usecase.GetOptimizedContextUseCase
import ru.agent.features.chat.domain.usecase.GetTokenMetricsUseCase
import ru.agent.features.chat.domain.usecase.SendMessageUseCase
import ru.agent.features.chat.presentation.ChatViewModel

val featureChatModule = module {
    // DAOs
    single<ChatSessionDao> { get<AppDatabase>().getChatSessionDao() }
    single<MessageDao> { get<AppDatabase>().getMessageDao() }
    single<MessageSummaryDao> { get<AppDatabase>().getMessageSummaryDao() }
    single<TokenUsageDao> { get<AppDatabase>().getTokenUsageDao() }

    // API Client
    single {
        DeepSeekApiClient(
            httpClient = get(),
            apiKey = get<String>(qualifier = named("deepseek_api_key"))
        )
    }

    // Summary API - DeepSeek implementation
    single<SummaryApi> { DeepSeekSummaryApi(deepSeekApiClient = get()) }

    // Configuration
    single { CompressionConfig.DEFAULT }

    // Token Optimization
    single { ContextOptimizer(maxTokens = 4000, keepRecentMessages = 4) }

    // Summary Generator - depends on SummaryApi abstraction
    single { SummaryGenerator(summaryApi = get()) }

    // Repositories
    singleOf(::ChatRepositoryImpl) bind ChatRepository::class
    singleOf(::ChatSessionRepositoryImpl) bind ChatSessionRepository::class
    singleOf(::MessageSummaryRepositoryImpl) bind MessageSummaryRepository::class
    singleOf(::TokenMetricsRepositoryImpl) bind TokenMetricsRepository::class

    // Use Cases
    singleOf(::SendMessageUseCase)
    singleOf(::GetChatHistoryUseCase)
    singleOf(::ClearChatHistoryUseCase)
    singleOf(::CreateChatSessionUseCase)
    singleOf(::GetAllChatSessionsUseCase)
    singleOf(::DeleteChatSessionUseCase)
    singleOf(::GetOptimizedContextUseCase)
    singleOf(::CompressHistoryUseCase)
    singleOf(::GenerateSummaryUseCase)
    singleOf(::GetTokenMetricsUseCase)

    // ViewModel
    viewModelOf(::ChatViewModel)
}
