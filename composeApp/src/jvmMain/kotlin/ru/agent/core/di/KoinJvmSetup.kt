package ru.agent.core.di

import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.agent.core.database.AppDatabase
import ru.agent.core.database.getDatabaseBuilder
import ru.agent.core.database.getRoomDatabase

val desktopDatabaseModule = module {
    single<AppDatabase> {
        getRoomDatabase(
            getDatabaseBuilder()
        )
    }

    // HuggingFace API Key for Model Comparison feature
    single(qualifier = named("huggingface_api_key")) {
        System.getenv("HUGGINGFACE_API_KEY")
            ?: System.getenv("HF_TOKEN")
            ?: "" // Empty string - will show error in UI if not configured
    }
}

actual val platformModule: Module = desktopDatabaseModule

actual fun getDeepSeekApiKey(): String {
    // In production, this should read from environment variable or config file
    val apiKey = System.getenv("DEEPSEEK_API_KEY")
    if (apiKey.isNullOrBlank()) {
        throw IllegalStateException(
            "DeepSeek API key is not configured. " +
            "Please set the DEEPSEEK_API_KEY environment variable with your API key. " +
            "Get your API key at: https://platform.deepseek.com/api_keys"
        )
    }
    return apiKey
}