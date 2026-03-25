package ru.agent.features.chat.data.remote.dto

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

internal actual fun getCurrentTimestamp(): String {
    return "ollama-${(NSDate().timeIntervalSince1970 * 1000).toLong()}"
}
