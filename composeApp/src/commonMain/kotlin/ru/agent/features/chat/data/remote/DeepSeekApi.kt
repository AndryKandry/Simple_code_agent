package ru.agent.features.chat.data.remote

object DeepSeekApi {
    const val BASE_URL = "https://api.deepseek.com/v1"
    const val MODEL = "deepseek-chat"
    const val TIMEOUT = 120_000L // 2 minutes - LLM can take time to generate responses
    const val MAX_TOKENS = 2000
    const val TEMPERATURE = 0.7
}
