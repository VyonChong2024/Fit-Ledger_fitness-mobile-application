package com.example.fyp_fitledger.data.model

data class Exercise(
    val id: Long = 0L,
    val name: String,
    val instruction: String,
    val category: String,
    val muscleGroup: String,
    val equipmentUsed: String,
    val gifUrl: String
)
