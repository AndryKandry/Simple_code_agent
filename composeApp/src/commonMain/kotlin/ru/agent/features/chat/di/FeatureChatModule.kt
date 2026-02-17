package ru.agent.features.chat.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.agent.core.network.createHttpClient
import ru.agent.features.chat.data.remote.DeepSeekApiClient
import ru.agent.features.chat.data.repository.ChatRepositoryImpl
import ru.agent.features.chat.domain.repository.ChatRepository
import ru.agent.features.chat.domain.usecase.ClearChatHistoryUseCase
import ru.agent.features.chat.domain.usecase.GetChatHistoryUseCase
import ru.agent.features.chat.domain.usecase.SendMessageUseCase
import ru.agent.features.chat.presentation.ChatViewModel

val featureChatModule = module {
    // API Client
    single {
        DeepSeekApiClient(
            httpClient = get(),
            apiKey = get<String>(qualifier = org.koin.core.qualifier.named("deepseek_api_key"))
        )
    }

    // Repository
    singleOf(::ChatRepositoryImpl) bind ChatRepository::class

    // Use Cases
    singleOf(::SendMessageUseCase)
    singleOf(::GetChatHistoryUseCase)
    singleOf(::ClearChatHistoryUseCase)

    // ViewModel
    viewModelOf(::ChatViewModel)
}
