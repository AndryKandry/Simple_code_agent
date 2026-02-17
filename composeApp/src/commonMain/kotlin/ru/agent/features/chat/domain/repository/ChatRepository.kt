package ru.agent.features.chat.domain.repository

import ru.agent.common.wrappers.ResultWrapper
import ru.agent.features.chat.domain.model.Message

interface ChatRepository {
    suspend fun sendMessage(message: String): ResultWrapper<Message>
    suspend fun getChatHistory(): List<Message>
    suspend fun clearHistory()
}
