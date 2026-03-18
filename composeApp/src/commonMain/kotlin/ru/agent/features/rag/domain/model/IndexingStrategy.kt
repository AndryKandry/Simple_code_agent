package ru.agent.features.rag.domain.model

/**
 * Strategy for indexing documents into chunks.
 */
enum class IndexingStrategy {
    /**
     * Fixed-size chunking based on character or token count.
     */
    FIXED_SIZE,

    /**
     * Structure-based chunking that respects code structure
     * (functions, classes, sections).
     */
    STRUCTURE_BASED
}
