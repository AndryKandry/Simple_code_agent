package ru.agent.core.di

import org.koin.core.module.Module
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