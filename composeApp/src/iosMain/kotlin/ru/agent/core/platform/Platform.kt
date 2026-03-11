package ru.agent.core.platform

import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager

/**
 * iOS implementation of platform-specific functions.
 */

actual fun getWorkingDirectory(): String {
    // On iOS, use the app's documents directory as the working directory
    val fileManager = NSFileManager.defaultManager
    val documentsUrl = fileManager.URLsForDirectory(
        directory = 9u, // NSDocumentDirectory
        inDomains = 1u  // NSUserDomainMask
    ).firstOrNull() as? platform.Foundation.NSURL

    return documentsUrl?.path ?: NSBundle.mainBundle.bundlePath
}
