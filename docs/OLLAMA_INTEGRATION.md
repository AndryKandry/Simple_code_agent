# Ollama Integration Guide

## Overview

This CLI application now supports local LLM inference through Ollama, enabling you to use AI models without relying on cloud APIs.

## What is Ollama?

Ollama is a tool for running large language models (LLMs) locally on your machine. It supports various models including:
- DeepSeek (deepseek-r1:8b, deepseek-r1:1.5b)
- Qwen (qwen2.5-coder:3b-instruct, qwen2.5-coder:7b-instruct)
- Llama (llama3, llama2)
- And many more

## Installation

### macOS
```bash
curl -fsSL https://ollama.com/install.sh | sh
```

### Linux
```bash
curl -fsSL https://ollama.com/install.sh | sh
```

### Windows
Download from [ollama.com](https://ollama.com/download)

## Pull a Model

```bash
# For coding tasks (recommended)
ollama pull qwen2.5-coder:3b-instruct

# For reasoning tasks
ollama pull deepseek-r1:1.5b

# List available models
ollama list
```

## Start Ollama Server

```bash
# Start the server (usually starts automatically on install)
ollama serve
```

The server runs on `http://localhost:11434` by default.

## Configuration

Set environment variables to use Ollama:

```bash
# For bash/zsh
export LLM_PROVIDER=ollama
export OLLAMA_BASE_URL=http://localhost:11434
export OLLAMA_MODEL=deepseek-r1:1.5b

# Or add to ~/.bashrc or ~/.zshrc for persistence
echo 'export LLM_PROVIDER=ollama' >> ~/.bashrc
echo 'export OLLAMA_BASE_URL=http://localhost:11434' >> ~/.bashrc
echo 'export OLLAMA_MODEL=deepseek-r1:1.5b' >> ~/.bashrc
```

## Usage Examples

### Basic Chat

```bash
# Using default provider (DeepSeek)
./gradlew :composeApp:runCli --args="chat 'Hello, how are you?'"

# Using Ollama
LLM_PROVIDER=ollama ./gradlew :composeApp:runCli --args="chat 'Hello, how are you?'"

# With specific model
LLM_PROVIDER=ollama OLLAMA_MODEL=qwen2.5-coder:3b-instruct ./gradlew :composeApp:runCli --args="chat 'Write a Kotlin function'"
```

## Model Recommendations

| Use Case | Recommended Model | Notes |
|----------|------------------|-------|
| Quick chat | deepseek-r1:1.5b | Fast, lightweight |
| Code generation | qwen2.5-coder:3b-instruct | Good balance |
| Complex coding | qwen2.5-coder:7b-instruct | Better quality |

## Troubleshooting

### Ollama Server Not Running
```bash
ollama serve
```

### Model Not Found
```bash
ollama pull <model-name>
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `LLM_PROVIDER` | `deepseek` | Provider to use |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama server URL |
| `OLLAMA_MODEL` | `deepseek-r1:8b` | Model to use |

## Important Notes

### Models That Don't Support Tools

Some Ollama models (like `deepseek-r1:1.5b` and `deepseek-r1:8b`) don't support function calling (tools). The application will **automatically** retry requests without tools when these models are used.

**Models with limited/no tool support:**
- `deepseek-r1:1.5b` - Fast, but doesn't support tools well
- `deepseek-r1:8b` - Good reasoning, but limited tool support

**Models with better tool support:**
- `qwen2.5-coder:3b-instruct` - Better for coding with tools
- `qwen2.5-coder:7b-instruct` - Best tool support
- `llama3:8b` - General purpose with tools

### Quick Start Scripts

Use the provided scripts for easy access:

```bash
# Start with default model (deepseek-r1:1.5b)
./agent-ollama

# Start with specific model
./agent-ollama qwen2.5-coder:3b-instruct

# Start with DeepSeek (cloud)
./agent-deepseek
```

Or use Gradle directly:

```bash
# With Ollama
LLM_PROVIDER=ollama OLLAMA_MODEL=deepseek-r1:1.5b ./gradlew :composeApp:runCli

# With DeepSeek
LLM_PROVIDER=deepseek ./gradlew :composeApp:runCli
```
