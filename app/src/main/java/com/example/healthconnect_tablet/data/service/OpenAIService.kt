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
 * OpenAI implementation of LLMService
 * Uses the OpenAI API to send messages to GPT models
 */
class OpenAIService(private val apiKey: String) : LLMService {
    
    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val BASE_URL = "https://api.openai.com/v1/chat/completions"
    private val MODEL = "gpt-4o-mini"

    override suspend fun sendMessage(
        messages: List<ChatMessage>,
        systemPrompt: String?
    ): Result<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val requestBody = JSONObject().apply {
                put("model", MODEL)
                put("max_tokens", 1024)
                put("temperature", 0.7)
                
                put("messages", JSONArray().apply {
                    // Add system prompt if provided
                    systemPrompt?.let {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", it)
                        })
                    }
                    
                    // Add conversation history
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
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody(JSON))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("OpenAI API error: ${response.code} - ${response.message}"))
                }

                val responseBody = response.body?.string()
                    ?: return@withContext Result.failure(IOException("Empty response body"))

                val jsonResponse = JSONObject(responseBody)
                val choices = jsonResponse.getJSONArray("choices")
                val content = if (choices.length() > 0) {
                    choices.getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
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
    
    override fun getProviderName(): String = "OpenAI"
    
    override fun getModelName(): String = MODEL
}