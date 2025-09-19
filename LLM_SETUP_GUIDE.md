# LLM Integration Setup Guide

This guide helps new developers set up LLM (Large Language Model) integration in the HealthConnect Tablet app.

##  Quick Start

### 1. Choose Your LLM Provider

The app supports three LLM providers:
- **Claude (Anthropic)** - Currently in use (will be deprecated)
- **OpenAI (GPT)** - Ready to use (need api key, contact pi)
- **Google Gemini** - Ready to use (need api key, contact pi)

### 2. Get Your API Key

#### For Claude (Anthropic):
1. Go to https://console.anthropic.com/
2. Sign up/Login to your account
3. Navigate to "API Keys" section
4. Create a new API key
5. Copy the key (starts with `sk-ant-api03-...`)

#### For OpenAI:
1. Go to https://platform.openai.com/
2. Sign up/Login to your account
3. Navigate to "API Keys" section
4. Create a new API key
5. Copy the key (starts with `sk-...`)

#### For Google Gemini:
1. Go to https://aistudio.google.com/
2. Sign up/Login with your Google account
3. Click "Get API Key"
4. Create a new API key
5. Copy the key

### 3. Add API Key to Project

1. Open the `local.properties` file in the root directory
2. Add your API key (only add the one you're using):

```properties
# Choose ONE provider:

# For Claude (Anthropic)
claude.api.key=sk-ant-api03-your-key-here

# OR for OpenAI
openai.api.key=sk-your-openai-key-here

# OR for Google Gemini
gemini.api.key=your-gemini-key-here
```

⚠️ **Important**: Never commit API keys to version control! The `local.properties` file is already in `.gitignore`.

### 4. Build and Run

1. Clean and rebuild the project:
   ```bash
   ./gradlew clean build
   ```
2. Run the app - LLM chat features should now work!

## 🔧 Architecture Overview

### File Structure
```
app/src/main/java/.../data/service/
├── LLMService.kt           # Interface for all LLM providers
├── ClaudeService.kt        # Anthropic Claude implementation
├── OpenAIService.kt        # OpenAI GPT implementation  
└── GeminiService.kt        # Google Gemini implementation
```

### How It Works

1. **API Key Loading**: `app/build.gradle.kts:29-36` reads keys from `local.properties`
2. **BuildConfig Generation**: Keys become `BuildConfig.CLAUDE_API_KEY`, etc.
3. **Service Creation**: ViewModels directly instantiate the chosen LLM service
4. **API Calls**: Services implement the `LLMService` interface for consistent usage

### Naming Convention Explanation

- `local.properties`: Uses dot notation → `claude.api.key`
- `BuildConfig`: Uses Android constants → `CLAUDE_API_KEY`

This follows standard Android conventions for properties vs. constants.

## 🔄 Switching LLM Providers

Switching providers is **super simple** - just change **one line** in each ViewModel!

### Current Setup (Claude):
```kotlin
private val llmService: LLMService = ClaudeService(BuildConfig.CLAUDE_API_KEY)
```

### To Switch to OpenAI:
```kotlin
private val llmService: LLMService = OpenAIService(BuildConfig.OPENAI_API_KEY)
```

### To Switch to Gemini:
```kotlin
private val llmService: LLMService = GeminiService(BuildConfig.GEMINI_API_KEY)
```

**Files to update:**
- `MemberDashboardViewModel.kt:37`
- `FamilyDashboardViewModel.kt:34`

That's it! Change one line in each file, rebuild, and you're using the new provider.

## 🎯 Current Models Used

- **Claude**: `claude-3-5-haiku-20241022`
- **OpenAI**: `gpt-4o-mini`
- **Gemini**: `gemini-1.5-flash`

You can change these in the respective service files if needed.

## 🛠️ Adding New LLM Providers

1. Create a new service class implementing `LLMService`
2. Add API key loading to `build.gradle.kts`
3. Update ViewModels to use the new service
4. Update this guide!

## 🔍 Troubleshooting

### "No LLM provider configured" Error
- Check that you've added at least one API key to `local.properties`
- Rebuild the project after adding keys
- Verify the key format is correct

### API Errors
- **401 Unauthorized**: Invalid API key
- **429 Rate Limited**: You've exceeded the API rate limits
- **Network errors**: Check internet connection

### Build Issues
- Clean and rebuild: `./gradlew clean build`
- Sync project files in Android Studio
- Check that `local.properties` exists and has correct keys

## 💡 Tips for Developers

1. **One provider at a time**: You'll typically only have one API key configured
2. **Test with small messages**: LLM APIs can be expensive
3. **Handle errors gracefully**: Network issues are common
4. **Simple switching**: Just change one line per ViewModel to switch providers
5. **Monitor usage**: Keep track of API costs

## 📱 Where LLM is Used

Currently used in:
- `MemberDashboardViewModel.kt:37` - Individual member chat
- `FamilyDashboardViewModel.kt:34` - Family-wide chat

Both use the same LLM service for consistent experience.

---

**Need help?** Check the existing implementations in the service files for examples!