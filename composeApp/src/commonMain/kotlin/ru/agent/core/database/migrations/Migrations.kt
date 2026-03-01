package ru.agent.core.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Migration from database version 2 to version 3.
 *
 * Adds two new tables for context compression feature:
 * - message_summaries: stores AI-generated summaries of old messages
 * - token_usage: tracks compression metrics and token savings
 */
val Migration2To3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        // Create message_summaries table
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS message_summaries (
                id TEXT NOT NULL PRIMARY KEY,
                sessionId TEXT NOT NULL,
                summary TEXT NOT NULL,
                startMessageId TEXT NOT NULL,
                endMessageId TEXT NOT NULL,
                messageCount INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                tokenCount INTEGER NOT NULL,
                FOREIGN KEY(sessionId) REFERENCES chat_sessions(id) ON DELETE CASCADE
            )
        """)

        // Create token_usage table
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS token_usage (
                id TEXT NOT NULL PRIMARY KEY,
                sessionId TEXT NOT NULL,
                tokensBefore INTEGER NOT NULL,
                tokensAfter INTEGER NOT NULL,
                compressionRatio REAL NOT NULL,
                messagesProcessed INTEGER NOT NULL,
                strategy TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                FOREIGN KEY(sessionId) REFERENCES chat_sessions(id) ON DELETE CASCADE
            )
        """)

        // Create indexes for message_summaries
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_message_summaries_sessionId ON message_summaries(sessionId)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_message_summaries_startMessageId ON message_summaries(startMessageId)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_message_summaries_endMessageId ON message_summaries(endMessageId)")

        // Create indexes for token_usage
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_token_usage_sessionId ON token_usage(sessionId)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_token_usage_timestamp ON token_usage(timestamp)")
    }
}
