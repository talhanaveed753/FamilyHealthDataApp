package com.example.healthconnect_tablet.data.model

/**
 * Data model for chat messages in LLM conversations
 * Supports multiple LLM providers (Claude, OpenAI, etc.)
 */
data class ChatMessage(
    val id: String,
    val content: String,
    val role: Role,
    val timestamp: Long
) {
    enum class Role {
        USER,
        ASSISTANT,
        SYSTEM
    }
} 