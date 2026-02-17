package ru.agent.features.chat.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ChatRequest(
    @EncodeDefault
    @SerialName("model")
    val model: String = "deepseek-chat",
    @SerialName("messages")
    val messages: List<MessageDto>,
    @EncodeDefault
    @SerialName("temperature")
    val temperature: Double = 0.7,
    @EncodeDefault
    @SerialName("max_tokens")
    val maxTokens: Int = 2000
)
