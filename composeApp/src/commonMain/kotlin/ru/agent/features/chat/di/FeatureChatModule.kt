package ru.agent.features.chat.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.agent.core.database.AppDatabase
import ru.agent.features.chat.data.local.dao.ChatSessionDao
import ru.agent.features.chat.data.local.dao.MessageDao
import ru.agent.features.chat.data.remote.DeepSeekApiClient
import ru.agent.features.chat.data.repository.ChatRepositoryImpl
import ru.agent.features.chat.data.repository.ChatSessionRepositoryImpl
import ru.agent.features.chat.domain.optimization.ContextOptimizer
import ru.agent.features.chat.domain.repository.ChatRepository
import ru.agent.features.chat.domain.repository.ChatSessionRepository
import ru.agent.features.chat.domain.usecase.CalculateTokenStatsUseCase
import ru.agent.features.chat.domain.usecase.ClearChatHistoryUseCase
import ru.agent.features.chat.domain.usecase.CreateChatSessionUseCase
import ru.agent.features.chat.domain.usecase.DeleteChatSessionUseCase
import ru.agent.features.chat.domain.usecase.GetAllChatSessionsUseCase
import ru.agent.features.chat.domain.usecase.GetChatHistoryUseCase
import ru.agent.features.chat.domain.usecase.GetOptimizedContextUseCase
import ru.agent.features.chat.domain.usecase.SendMessageUseCase
import ru.agent.features.chat.domain.split.MessageSplitter
import ru.agent.features.chat.domain.usecase.SplitMessageUseCase
import ru.agent.features.chat.presentation.ChatViewModel

val featureChatModule = module {
    // DAOs
    single<ChatSessionDao> { get<AppDatabase>().getChatSessionDao() }
    single<MessageDao> { get<AppDatabase>().getMessageDao() }

    // API Client
    single {
        DeepSeekApiClient(
            httpClient = get(),
            apiKey = get<String>(qualifier = named("deepseek_api_key"))
        )
    }

    // Token Optimization
    single { ContextOptimizer(maxTokens = 4000, keepRecentMessages = 4) }

    // Repositories
    singleOf(::ChatRepositoryImpl) bind ChatRepository::class
    singleOf(::ChatSessionRepositoryImpl) bind ChatSessionRepository::class

    // Use Cases
    singleOf(::SendMessageUseCase)
    singleOf(::GetChatHistoryUseCase)
    singleOf(::ClearChatHistoryUseCase)
    singleOf(::CreateChatSessionUseCase)
    singleOf(::GetAllChatSessionsUseCase)
    singleOf(::DeleteChatSessionUseCase)
    singleOf(::GetOptimizedContextUseCase)
    singleOf(::CalculateTokenStatsUseCase)

    // Message Splitting
    singleOf(::MessageSplitter)
    singleOf(::SplitMessageUseCase)

    // ViewModel
    viewModelOf(::ChatViewModel)
}
