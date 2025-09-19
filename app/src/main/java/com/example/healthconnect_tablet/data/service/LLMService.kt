package com.example.healthconnect_tablet.data.service

import com.example.healthconnect_tablet.data.model.ChatMessage

/**
 * Abstract interface for LLM service providers
 * Allows easy switching between Claude, OpenAI, and other providers
 */
interface LLMService {
    
    /**
     * Send a message to the LLM and get a response
     * @param messages List of conversation messages
     * @param systemPrompt Optional system prompt for context
     * @return Result containing the LLM response or error
     */
    suspend fun sendMessage(
        messages: List<ChatMessage>,
        systemPrompt: String? = null
    ): Result<ChatMessage>
    
    /**
     * Get the name of the LLM provider
     */
    fun getProviderName(): String
    
    /**
     * Get the model name being used
     */
    fun getModelName(): String
} 