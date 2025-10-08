package com.example.fyp_fitledger

//Represents the request body for OpenAI (e.g., message, model type).
data class ChatRequest(
    val model: String = "gpt-3.5-turbo",
    val messages: List<Message>,
    val max_tokens: Int = 100
)

data class Message(
    val role: String,  // "user" or "assistant"
    val content: String
)