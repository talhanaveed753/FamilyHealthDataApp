package com.example.healthconnect_tablet.data.service

import com.example.healthconnect_tablet.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * Claude (Anthropic) implementation of LLMService
 * Uses the Anthropic API to send messages to Claude models
 */
class ClaudeService(private val apiKey: String) : LLMService {
    
    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val BASE_URL = "https://api.anthropic.com/v1/messages"
    private val MODEL = "claude-3-5-haiku-20241022"
    
    override suspend fun sendMessage(
        messages: List<ChatMessage>,
        systemPrompt: String?
    ): Result<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val requestBody = JSONObject().apply {
                put("model", MODEL)
                put("max_tokens", 1024)
                
                // Add system prompt if provided (separate field for Anthropic API)
                systemPrompt?.let {
                    put("system", it)
                }
                
                put("messages", JSONArray().apply {
                    // Add conversation history (only user/assistant messages)
                    messages.forEach { message ->
                        put(JSONObject().apply {
                            put("role", message.role.name.lowercase())
                            put("content", message.content)
                        })
                    }
                })
            }

            val request = Request.Builder()
                .url(BASE_URL)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .post(requestBody.toString().toRequestBody(JSON))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Claude API error: ${response.code} - ${response.message}"))
                }

                val responseBody = response.body?.string()
                    ?: return@withContext Result.failure(IOException("Empty response body"))

                val jsonResponse = JSONObject(responseBody)
                val contentArray = jsonResponse.getJSONArray("content")
                val content = if (contentArray.length() > 0) {
                    contentArray.getJSONObject(0).getString("text")
                } else {
                    "No response received"
                }

                val assistantMessage = ChatMessage(
                    id = System.currentTimeMillis().toString(),
                    content = content,
                    role = ChatMessage.Role.ASSISTANT,
                    timestamp = System.currentTimeMillis()
                )

                Result.success(assistantMessage)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getProviderName(): String = "Claude (Anthropic)"
    
    override fun getModelName(): String = MODEL
} 