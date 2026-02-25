package ru.agent.features.chat.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.core.handlers.NetworkErrorHandling
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.chat.data.local.dao.ChatSessionDao
import ru.agent.features.chat.data.local.dao.MessageDao
import ru.agent.features.chat.data.local.mapper.MessageMapper.toDomain
import ru.agent.features.chat.data.local.mapper.MessageMapper.toEntity
import ru.agent.features.chat.data.remote.DeepSeekApiClient
import ru.agent.features.chat.data.remote.dto.ChatRequest
import ru.agent.features.chat.data.remote.dto.MessageDto
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.model.SenderType
import ru.agent.features.chat.domain.optimization.ContextOptimizer
import ru.agent.features.chat.domain.optimization.OptimizedContext
import ru.agent.features.chat.domain.optimization.ToonEncoder
import ru.agent.features.chat.domain.repository.ChatRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Implementation of ChatRepository using Room database for persistent storage.
 *
 * Uses DeepSeek API for AI responses and stores all messages in local database.
 * Includes context optimization to manage token limits.
 */
class ChatRepositoryImpl(
    private val deepSeekApiClient: DeepSeekApiClient,
    private val networkErrorHandling: NetworkErrorHandling,
    private val messageDao: MessageDao,
    private val chatSessionDao: ChatSessionDao,
    private val contextOptimizer: ContextOptimizer
) : ChatRepository {

    private val logger = Logger.withTag("ChatRepository")

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun sendMessage(sessionId: String, message: String): ResultWrapper<Message> {
        logger.i { "sendMessage called for session: $sessionId, message: ${message.take(50)}..." }

        return withContext(Dispatchers.IO) {
            try {
                // Step 1: Create user message with UUID and estimated token count
                val userTokens = ToonEncoder.estimateTokens(message)
                val userMessage = Message(
                    id = Uuid.random().toString(),
                    content = message,
                    senderType = SenderType.USER,
                    timestamp = currentTimeMillis(),
                    tokenCount = userTokens
                )

                // Step 2: Save user message to database
                messageDao.insertMessage(userMessage.toEntity(sessionId))
                logger.d { "User message saved to database with ID: ${userMessage.id}" }

                // Step 3: Increment message count in session
                chatSessionDao.incrementMessageCount(sessionId, currentTimeMillis())
                logger.d { "Session message count incremented for session: $sessionId" }

                // Step 4: Get optimized context
                val optimizedContext = getOptimizedContext(sessionId)
                logger.d {
                    "Context optimized for API request. " +
                    "Strategy: ${optimizedContext.strategy}, " +
                    "Tokens: ~${optimizedContext.estimatedTokens}, " +
                    "Messages: ${optimizedContext.messages.size}"
                }

                // Step 5: Prepare API request with optimized messages
                val messages = optimizedContext.messages.map { msg ->
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

                // Step 6: Call DeepSeek API
                val response = deepSeekApiClient.sendMessage(request)
                logger.i { "Received response from DeepSeek API. ID: ${response.id}, choices: ${response.choices.size}" }

                // Validate response
                if (response.choices.isEmpty()) {
                    logger.e { "Empty response from API" }
                    // Remove user message on error
                    messageDao.deleteMessageById(userMessage.id)
                    return@withContext ResultWrapper.Error(
                        throwable = IllegalStateException("Empty response from API"),
                        message = "Received empty response from DeepSeek API"
                    )
                }

                // Step 7: Create and save assistant message with token count from API
                val totalTokens = response.usage.totalTokens
                val assistantMessage = Message(
                    id = response.id,
                    content = response.choices.first().message.content,
                    senderType = SenderType.ASSISTANT,
                    timestamp = currentTimeMillis(),
                    tokenCount = totalTokens
                )

                messageDao.insertMessage(assistantMessage.toEntity(sessionId))
                logger.d { "Assistant message saved with ID: ${assistantMessage.id}, tokens: $totalTokens" }

                // Increment message count for assistant message
                chatSessionDao.incrementMessageCount(sessionId, currentTimeMillis())

                // Step 8: Update session title if this was first exchange (2 messages)
                val messageCount = messageDao.getMessageCount(sessionId)
                if (messageCount == 2) {
                    // This is the first exchange - update title based on user message
                    val newTitle = generateTitleFromMessage(message)
                    val session = chatSessionDao.getSessionById(sessionId)
                    if (session != null && session.title == "New Chat") {
                        chatSessionDao.updateSession(
                            session.copy(
                                title = newTitle,
                                updatedAt = currentTimeMillis()
                            )
                        )
                        logger.i { "Session title updated to: $newTitle" }
                    }
                }

                logger.i { "sendMessage completed successfully for session: $sessionId" }
                ResultWrapper.Success(assistantMessage)

            } catch (e: Exception) {
                logger.e(throwable = e) { "Error sending message to DeepSeek API" }
                networkErrorHandling.transformToResultWrapper(e)
            }
        }
    }

    override suspend fun getChatHistory(sessionId: String): List<Message> {
        return withContext(Dispatchers.IO) {
            val messages = messageDao.getMessagesForSession(sessionId).toDomain()
            logger.d { "getChatHistory: Loaded ${messages.size} messages for session $sessionId" }
            messages
        }
    }

    override fun getChatHistoryFlow(sessionId: String): Flow<List<Message>> {
        logger.d { "getChatHistoryFlow: Subscribing to messages flow for session $sessionId" }
        return messageDao.getMessagesForSessionFlow(sessionId)
            .map { entities -> entities.toDomain() }
    }

    override suspend fun clearHistory(sessionId: String) {
        withContext(Dispatchers.IO) {
            logger.i { "clearHistory: Clearing messages for session $sessionId" }
            messageDao.deleteMessagesForSession(sessionId)

            // Reset message count in session
            val session = chatSessionDao.getSessionById(sessionId)
            if (session != null) {
                chatSessionDao.updateSession(
                    session.copy(
                        messageCount = 0,
                        updatedAt = currentTimeMillis()
                    )
                )
            }

            logger.i { "clearHistory: Messages cleared for session $sessionId" }
        }
    }

    override suspend fun getOptimizedContext(sessionId: String): OptimizedContext {
        val messages = getChatHistory(sessionId)
        val optimized = contextOptimizer.optimize(messages)

        logger.d {
            "Optimized context for session: $sessionId. " +
            "Strategy: ${optimized.strategy}, " +
            "Tokens: ${optimized.estimatedTokens}, " +
            "Truncated: ${optimized.truncatedCount}"
        }

        return optimized
    }

    /**
     * Generate a session title from the first user message.
     * Takes first 50 characters or first line, whichever is shorter.
     */
    private fun generateTitleFromMessage(message: String): String {
        val firstLine = message.lines().firstOrNull() ?: message
        return if (firstLine.length > 50) {
            firstLine.take(47) + "..."
        } else {
            firstLine
        }
    }
}
