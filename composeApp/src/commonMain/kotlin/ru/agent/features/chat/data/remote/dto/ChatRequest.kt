package ru.agent.features.chat.data.remote.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    val maxTokens: Int = 2000,
    @SerialName("stop")
    val stop: List<String>? = null,
    @SerialName("top_p")
    val topP: Double? = null,
    @SerialName("frequency_penalty")
    val frequencyPenalty: Double? = null,
    @SerialName("presence_penalty")
    val presencePenalty: Double? = null
)
