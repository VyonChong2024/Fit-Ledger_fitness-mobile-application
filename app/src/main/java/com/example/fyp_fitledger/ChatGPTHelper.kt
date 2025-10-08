package com.example.fyp_fitledger

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import android.util.Log

//Handles API requests and responses.
class ChatGPTHelper {
    private val functions: FirebaseFunctions = Firebase.functions

    fun getChatGPTResponse(userMessage: String, onResult: (String) -> Unit, onError: (Exception) -> Unit) {
        val messages = listOf(
            mapOf("role" to "user", "content" to userMessage)
        )

        val requestData = mapOf("messages" to messages)

        Log.d("ChatGPT", "Sending Request: $requestData")

        Firebase.functions
            .getHttpsCallable("getChatGPTResponse")
            .call(requestData)
            .addOnSuccessListener { result ->
                try {
                    Log.d("ChatGPT", "Firebase Response: $result") // ✅ Log raw response
                    val response = result.getData() as? Map<*, *> ?: throw Exception("Invalid response format")
                    val choices = response["choices"] as? List<*> ?: throw Exception("No choices in response")
                    val firstChoice = choices.firstOrNull() as? Map<*, *> ?: throw Exception("Empty choices")
                    val message = firstChoice["message"] as? Map<*, *>
                    val content = message?.get("content") as? String ?: "No response"

                    onResult(content)  // Pass response back to caller
                } catch (e: Exception) {
                    Log.e("ChatGPT", "Error parsing response: ${e.message}")
                    onError(e)
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatGPT", "Firebase Error: ${e.message}")
                onError(e)
            }
    }


}


