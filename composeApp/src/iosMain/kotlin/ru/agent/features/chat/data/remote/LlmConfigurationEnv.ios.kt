package ru.agent.features.chat.data.remote

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.getenv

/**
 * iOS implementation of environment variable access using POSIX.
 */
@OptIn(ExperimentalForeignApi::class)
internal actual fun getEnvVariable(name: String): String? {
    return memScoped {
        getenv(name)?.toKString()
    }
}
