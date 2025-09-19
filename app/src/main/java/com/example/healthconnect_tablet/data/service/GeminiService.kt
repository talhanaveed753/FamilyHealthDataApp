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
 * Google Gemini implementation of LLMService
 * Uses the Google AI Studio API to send messages to Gemini models
 */
class GeminiService(private val apiKey: String) : LLMService {
    
    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val MODEL = "gemini-1.5-flash"
    private val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"
    
    override suspend fun sendMessage(
        messages: List<ChatMessage>,
        systemPrompt: String?
    ): Result<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val requestBody = JSONObject().apply {
                put("generationConfig", JSONObject().apply {
                    put("maxOutputTokens", 1024)
                    put("temperature", 0.7)
                })
                
                put("contents", JSONArray().apply {
                    // Add system prompt if provided (as first user message for Gemini)
                    systemPrompt?.let {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", "System: $it")
                                })
                            })
                        })
                        put(JSONObject().apply {
                            put("role", "model")
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", "Understood. I'll follow these instructions.")
                                })
                            })
                        })
                    }
                    
                    // Add conversation history
                    messages.forEach { message ->
                        put(JSONObject().apply {
                            val role = when (message.role) {
                                ChatMessage.Role.USER -> "user"
                                ChatMessage.Role.ASSISTANT -> "model"
                                else -> "user"
                            }
                            put("role", role)
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", message.content)
                                })
                            })
                        })
                    }
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody(JSON))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Gemini API error: ${response.code} - ${response.message}"))
                }

                val responseBody = response.body?.string()
                    ?: return@withContext Result.failure(IOException("Empty response body"))

                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.getJSONArray("candidates")
                val content = if (candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val contentObj = candidate.getJSONObject("content")
                    val parts = contentObj.getJSONArray("parts")
                    if (parts.length() > 0) {
                        parts.getJSONObject(0).getString("text")
                    } else {
                        "No response received"
                    }
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
    
    override fun getProviderName(): String = "Google Gemini"
    
    override fun getModelName(): String = MODEL
}