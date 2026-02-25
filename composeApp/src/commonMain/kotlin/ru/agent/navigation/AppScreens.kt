package ru.agent.navigation

enum class AppScreens(val title: String) {
    Main("main"),
    Chat("chat?sessionId={sessionId}");

    companion object {
        fun chatWithSession(sessionId: String) = "chat?sessionId=$sessionId"
    }
}
