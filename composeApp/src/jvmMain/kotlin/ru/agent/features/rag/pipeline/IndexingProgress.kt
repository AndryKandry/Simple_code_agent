package ru.agent.features.rag.pipeline

import ru.agent.features.rag.domain.model.IndexStats

/**
 * Sealed interface representing progress updates during indexing.
 */
sealed interface IndexingProgress {
    /**
     * Checking Ollama server connectivity.
     */
    object CheckingOllama : IndexingProgress {
        override fun toString(): String = "Checking Ollama connection..."
    }

    /**
     * Clearing existing index.
     */
    object ClearingIndex : IndexingProgress {
        override fun toString(): String = "Clearing existing index..."
    }

    /**
     * Collecting files from git repository.
     */
    object CollectingFiles : IndexingProgress {
        override fun toString(): String = "Collecting files..."
    }

    /**
     * Processing a file.
     *
     * @property path Path of the file being processed
     * @property current Current file index
     * @property total Total number of files
     */
    data class ProcessingFile(
        val path: String,
        val current: Int,
        val total: Int
    ) : IndexingProgress {
        override fun toString(): String = "Processing file ($current/$total): $path"
    }

    /**
     * Generating embeddings for chunks.
     *
     * @property chunkIndex Current chunk index
     * @property total Total number of chunks
     */
    data class GeneratingEmbeddings(
        val chunkIndex: Int,
        val total: Int
    ) : IndexingProgress {
        override fun toString(): String = "Generating embeddings ($chunkIndex/$total)"
    }

    /**
     * Saving chunks to database.
     *
     * @property count Number of chunks being saved
     */
    data class SavingChunks(
        val count: Int
    ) : IndexingProgress {
        override fun toString(): String = "Saving $count chunks to database..."
    }

    /**
     * Indexing completed successfully.
     *
     * @property stats Statistics from the indexing operation
     */
    data class Completed(val stats: IndexStats) : IndexingProgress {
        override fun toString(): String = "Indexing completed: ${stats.totalChunks} chunks from ${stats.totalFiles} files"
    }

    /**
     * Indexing failed with an error.
     *
     * @property message Error message describing what went wrong
     */
    data class Error(val message: String) : IndexingProgress {
        override fun toString(): String = "Error: $message"
    }
}
