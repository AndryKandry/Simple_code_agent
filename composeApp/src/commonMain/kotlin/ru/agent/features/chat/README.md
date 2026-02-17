# Chat Feature with DeepSeek AI

## Overview

This feature provides a chat interface for interacting with DeepSeek AI assistant. Users can send messages and receive responses in real-time.

## Features

- Real-time chat with DeepSeek AI
- Message history within session
- Modern Material 3 UI design
- Loading indicators
- Error handling with retry capability
- Responsive layout for desktop

## Setup

### 1. Get DeepSeek API Key

1. Visit [DeepSeek Platform](https://platform.deepseek.com/)
2. Sign up or log in
3. Navigate to API Keys section
4. Create a new API key

### 2. Configure API Key

#### For Desktop (JVM):

Set environment variable:

**macOS/Linux:**
```bash
export DEEPSEEK_API_KEY="your-api-key-here"
```

**Windows:**
```cmd
set DEEPSEEK_API_KEY=your-api-key-here
```

Or add to your shell profile (~/.bashrc, ~/.zshrc, etc.) for persistence.

#### For Android:

Add to `local.properties`:
```properties
deepseek.api.key=your-api-key-here
```

Then update `KoinAndroidSetup.kt` to read from BuildConfig.

#### For iOS:

Add to Info.plist or use a configuration file.

### 3. Run the Application

**Desktop:**
```bash
./gradlew :composeApp:run
```

**Android:**
```bash
./gradlew :composeApp:installDebug
```

## Architecture

### Layers:

- **Presentation**: Compose UI, ViewModel
- **Domain**: Use Cases, Models, Repository Interfaces
- **Data**: Repository Implementation, API Client, DTOs

### Key Components:

- `ChatViewModel` - Manages UI state and business logic
- `SendMessageUseCase` - Handles message sending logic
- `ChatRepository` - Abstracts data operations
- `DeepSeekApiClient` - Network communication with DeepSeek API

## Usage

1. Open the app
2. Click "Open Chat" button
3. Type your message in the input field
4. Press Enter or click Send button
5. Wait for AI response
6. Continue the conversation

## Error Handling

- Network errors are displayed in Snackbar
- On error, the message is preserved in input field for retry
- User can manually retry by sending again

## Limitations

- Chat history is stored in memory only (cleared on app restart)
- No persistence between sessions
- No support for images or files (text only)

## Future Improvements

- [ ] Add persistence for chat history
- [ ] Support for multiple chat sessions
- [ ] Image/file support
- [ ] Voice input
- [ ] Keyboard shortcuts (Desktop)
- [ ] Dark/Light theme toggle
- [ ] Export chat history

## Troubleshooting

### "Failed to send message"

- Check your internet connection
- Verify API key is correctly set
- Check API quota/limits on DeepSeek platform

### "Empty response from API"

- This indicates an API issue
- Try again in a few moments
- Check DeepSeek status page

### App crashes on startup

- Ensure API key is configured
- Check logs for specific error

## License

Part of the Simple Code Agent project.
