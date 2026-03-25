package ru.agent.features.chat.data.remote

/**
 * JVM implementation of environment variable access.
 */
internal actual fun getEnvVariable(name: String): String? {
    return System.getenv(name)
}
