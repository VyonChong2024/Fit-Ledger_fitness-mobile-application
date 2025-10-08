package com.example.fyp_fitledger

//Represents the response data structure from OpenAI.
data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)