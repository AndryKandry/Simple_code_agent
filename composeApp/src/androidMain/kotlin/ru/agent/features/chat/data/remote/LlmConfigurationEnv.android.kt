package ru.agent.features.chat.data.remote

/**
 * Android implementation of environment variable access.
 * Note: Android uses System.getenv() like JVM.
 */
internal actual fun getEnvVariable(name: String): String? {
    return System.getenv(name)
}
