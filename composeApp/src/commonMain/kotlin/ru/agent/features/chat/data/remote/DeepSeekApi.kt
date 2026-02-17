package ru.agent.features.chat.data.remote

object DeepSeekApi {
    const val BASE_URL = "https://api.deepseek.com/v1"
    const val MODEL = "deepseek-chat"
    const val TIMEOUT = 60_000L
    const val MAX_TOKENS = 2000
    const val TEMPERATURE = 0.7
}
