package ru.agent.features.rag.domain.model

/**
 * Data class for RAG source information to be passed through the application layers.
 * Contains information about the document source and relevance score.
 */
data class RagSource(
    val fileName: String,
    val filePath: String,
    val startLine: Int,
    val endLine: Int,
    val similarity: Float,
    val rank: Int
) {
    companion object {
        /**
         * Create RagSource from ChunkScore
         */
        fun fromChunkScore(chunk: ChunkScore): RagSource {
            return RagSource(
                fileName = chunk.fileName,
                filePath = chunk.source,
                startLine = chunk.startLine,
                endLine = chunk.endLine,
                similarity = chunk.similarity,
                rank = chunk.rank
            )
        }
    }
}
