# RAG + Local LLM Integration Guide

## Overview

The project now supports **fully local RAG (Retrieval-Augmented Generation)** with Ollama.

## What Was Merged

From `features/agent_rag` branch:
- **RAG Indexing Pipeline**: Documents indexing with Ollama embeddings
- **RAG Search Service**: Semantic search with reranking
- **Mini-Chat Command**: Interactive chat with RAG + task memory
- **Task Context Feature**: Context-aware responses with RAG query history
- **Index Commands**: `index run`, `index stats`, `index compare`, `index clear`

## Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   User Query    │────>│  RAG Search      │────>│  Ollama Embed   │
└─────────────────┘     │  (local)         │     │  (bge-m3)       │
                        └──────────────────┘     └─────────────────┘
                                 │
                                 v
                        ┌──────────────────┐
                        │  Chunk Score     │
                        │  (similarity)    │
                        └──────────────────┘
                                 │
                                 v
                        ┌──────────────────┐     ┌─────────────────┐
                        │  LLM Generation  │────>│  Ollama Chat    │
                        │  (local)         │     │  (deepseek-r1)  │
                        └──────────────────┘     └─────────────────┘
```

## Environment Variables

```bash
# Use local Ollama for both chat and RAG
export LLM_PROVIDER=ollama
export OLLAMA_BASE_URL=http://localhost:11434
export OLLAMA_MODEL=deepseek-r1:8b

# Optional: Use DeepSeek for chat, Ollama for RAG embeddings
export DEEPSEEK_API_KEY=your_api_key
export LLM_PROVIDER=deepseek
```

## Required Ollama Models

```bash
# 1. Chat model (for generation)
ollama pull deepseek-r1:8b

# 2. Embedding model (for RAG retrieval)
ollama pull bge-m3

# 3. Optional: Reranker model (for enhanced RAG)
ollama pull bge-reranker:latest

# 4. Optional: Query rewriter model
ollama pull deepseek-r1:1.5b
```

## Usage

### 1. Index Your Project

```bash
./gradlew :composeApp:jvmJar

# Index the current project
java -jar composeApp/build/libs/composeApp-*-jvm.jar index run

# Check index statistics
java -jar composeApp/build/libs/composeApp-*-jvm.jar index stats
```

### 2. Chat with RAG

```bash
# Interactive chat with RAG enabled
java -jar composeApp/build/libs/composeApp-*-jvm.jar chat --rag

# Compare RAG vs non-RAG responses
java -jar composeApp/build/libs/composeApp-*-jvm.jar chat --compare

# Mini-chat with task memory
java -jar composeApp/build/libs/composeApp-*-jvm.jar minichat
```

### 3. RAG Modes

**BASELINE Mode** (default):
- Uses embeddings for retrieval
- No reranking or query rewriting
- Fastest response time

**ENHANCED Mode** (optional):
- Uses embeddings + reranking
- Query rewriting for better retrieval
- Higher quality, slower response

## Comparison: Local vs Cloud

| Metric | Local (Ollama) | Cloud (DeepSeek) |
|--------|----------------|------------------|
| **Quality** | Good (7B models) | Excellent (32B models) |
| **Speed** | 2-10 sec | 0.5-2 sec |
| **Privacy** | 100% local | Data sent to API |
| **Cost** | Free (CPU/GPU) | $0.14/1M tokens |
| **Offline** | Yes | No |

## RAG Quality Assessment

### Retrieval Quality
- **Embedding Model**: bge-m3 (multilingual, 1024 dims)
- **Similarity Threshold**: 0.3 (configurable)
- **Max Chunks**: 5 per query

### Generation Quality
- **Model**: deepseek-r1:8b (reasoning model)
- **Context Window**: 4000 tokens (optimized)
- **Response Format**: Markdown with sources

## Configuration

Edit `RagConfig.kt` to customize:

```kotlin
data class RagConfig(
    val maxChunks: Int = 5,
    val relevanceThreshold: Float = 0.3f,
    val enableReranking: Boolean = false,
    val enableQueryRewrite: Boolean = false
)
```

## Troubleshooting

### Ollama Connection Issues
```bash
# Check if Ollama is running
curl http://localhost:11434/api/tags

# Start Ollama server
ollama serve
```

### Model Not Found
```bash
# List available models
ollama list

# Pull required models
ollama pull bge-m3
ollama pull deepseek-r1:8b
```

### Empty Index
```bash
# Rebuild index
java -jar composeApp/build/libs/composeApp-*-jvm.jar index clear
java -jar composeApp/build/libs/composeApp-*-jvm.jar index run
```

## File Structure

```
composeApp/src/
├── commonMain/kotlin/ru/agent/features/rag/
│   ├── data/
│   │   ├── local/dao/          # Room DAOs
│   │   ├── remote/             # Ollama API clients
│   │   └── repository/         # Data repositories
│   ├── domain/
│   │   ├── model/              # RAG models
│   │   ├── service/            # RAG services
│   │   └── formatter/          # Response formatting
│   └── di/                     # Koin modules
└── jvmMain/kotlin/ru/agent/features/rag/
    ├── chunker/                # Text chunking strategies
    ├── collector/              # File collection
    └── pipeline/               # Indexing pipeline
```

## Performance Tips

1. **Use SSD** for index storage (faster embedding retrieval)
2. **Limit chunk size** for faster embedding (default: 500 tokens)
3. **Enable caching** for repeated queries
4. **Use smaller models** on CPU-only machines
5. **Batch embeddings** during indexing (default: 10 texts)

## Future Enhancements

- [ ] Hybrid search (keyword + semantic)
- [ ] Cross-encoder reranking with local models
- [ ] Multi-modal RAG (images + text)
- [ ] Streaming responses
- [ ] Distributed indexing
