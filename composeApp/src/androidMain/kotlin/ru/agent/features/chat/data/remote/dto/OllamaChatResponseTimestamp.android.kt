package ru.agent.features.chat.data.remote.dto

import java.time.Instant

internal actual fun getCurrentTimestamp(): String {
    return "ollama-${Instant.now().toEpochMilli()}"
}
