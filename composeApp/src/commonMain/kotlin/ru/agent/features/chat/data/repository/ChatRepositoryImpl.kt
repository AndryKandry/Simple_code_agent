package ru.agent.features.chat.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.core.handlers.NetworkErrorHandling
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.chat.data.remote.DeepSeekApiClient
import ru.agent.features.chat.data.remote.dto.ChatRequest
import ru.agent.features.chat.data.remote.dto.MessageDto
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.model.SenderType
import ru.agent.features.chat.domain.repository.ChatRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ChatRepositoryImpl(
    private val deepSeekApiClient: DeepSeekApiClient,
    private val networkErrorHandling: NetworkErrorHandling
) : ChatRepository {

    private val chatHistory = mutableListOf<Message>()
    private val mutex = Mutex()

    private val logger = Logger.withTag("ChatRepository")

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun sendMessage(message: String): ResultWrapper<Message> {
        logger.i { "sendMessage called with: ${message.take(50)}..." }

        return withContext(Dispatchers.IO) {
            val userMessage = Message(
                id = Uuid.random().toString(),
                content = message,
                senderType = SenderType.USER,
                timestamp = currentTimeMillis()
            )

            try {
                mutex.withLock {
                    chatHistory.add(userMessage)
                    logger.d { "User message added to history. Total messages: ${chatHistory.size}" }

                    // Prepare request with chat history
                    val messages = chatHistory.map { msg ->
                        MessageDto(
                            role = when (msg.senderType) {
                                SenderType.USER -> "user"
                                SenderType.ASSISTANT -> "assistant"
                            },
                            content = msg.content
                        )
                    }

                    val request = ChatRequest(messages = messages)
                    logger.i { "Sending request to DeepSeek API with ${messages.size} messages" }

                    // Send to API
                    val response = deepSeekApiClient.sendMessage(request)
                    logger.i { "Received response from DeepSeek API. ID: ${response.id}, choices: ${response.choices.size}" }

                    // Validate response
                    if (response.choices.isEmpty()) {
                        logger.e { "Empty response from API" }
                        chatHistory.remove(userMessage)
                        return@withContext ResultWrapper.Error(
                            throwable = IllegalStateException("Empty response from API"),
                            message = "Received empty response from DeepSeek API"
                        )
                    }

                    // Create assistant message
                    val assistantMessage = Message(
                        id = response.id,
                        content = response.choices.first().message.content,
                        senderType = SenderType.ASSISTANT,
                        timestamp = currentTimeMillis()
                    )
                    chatHistory.add(assistantMessage)
                    logger.d { "Assistant message added. Content preview: ${assistantMessage.content.take(50)}..." }

                    ResultWrapper.Success(assistantMessage)
                }
            } catch (e: Exception) {
                logger.e(throwable = e) { "Error sending message to DeepSeek API" }
                // Remove user message on error
                mutex.withLock {
                    chatHistory.remove(userMessage)
                }
                networkErrorHandling.transformToResultWrapper(e)
            }
        }
    }

    override suspend fun getChatHistory(): List<Message> {
        return mutex.withLock {
            val history = chatHistory.toList()
            logger.d { "getChatHistory called. Size: ${history.size}" }
            history
        }
    }

    override suspend fun clearHistory() {
        mutex.withLock {
            logger.i { "clearHistory called" }
            chatHistory.clear()
        }
    }
}
