package com.example.fyp_fitledger.data.model

data class MealLogs(
    val logId: Long = 0,
    val userId: String,
    val date: String,
    val time: String,
    val notes: String?,
    val isSynced: Int = 0
)

data class MealLogFoods(
    val mealLogId: Long = 0,
    val logId: Long,
    val food: String,
    val quantity: Double,
    val isSynced: Int = 0
)