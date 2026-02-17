package ru.agent.core.di

import org.koin.core.module.Module
import org.koin.dsl.module
import ru.agent.core.database.AppDatabase
import ru.agent.core.database.getDatabaseBuilder
import ru.agent.core.database.getRoomDatabase

val iosDatabaseModule = module {
    single<AppDatabase> {
        getRoomDatabase(
            getDatabaseBuilder()
        )
    }
}

actual val platformModule: Module = iosDatabaseModule

actual fun getDeepSeekApiKey(): String {
    // In production, read from UserDefaults or config
    throw IllegalStateException(
        "DeepSeek API key is not configured for iOS. " +
        "Please implement API key retrieval from UserDefaults or config. " +
        "Get your API key at: https://platform.deepseek.com/api_keys"
    )
}