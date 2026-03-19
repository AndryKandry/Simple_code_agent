package ru.agent.features.chat.domain.model

import ru.agent.features.rag.domain.model.ChunkScore

data class Message(
    val id: String,
    val content: String,
    val senderType: SenderType,
    val timestamp: Long,
    /**
     * RAG sources - релевантные фрагменты кода, использованные для генерации ответа.
     * Заполняется когда RAG включен и найдены релевантные чанки.
     */
    val sources: List<ChunkScore>? = null
)
