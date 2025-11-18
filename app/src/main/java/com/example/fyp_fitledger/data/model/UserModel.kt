package com.example.fyp_fitledger.data.model

data class UserProfile(
    val gender: String,
    val age: Int,
    val height: Double,
    val weight: Double,
    val bodyFatPercent: Double,
    val targetBodyFat: Double,
    val targetWeight: Double,
    val dietPlan: String
)

data class User(
    val userId: String,
    val gender: String?,
    val age: Int?,
    val height: Double?,
    val weight: Double?,
    val bodyFatPercent: Double?,
    val targetBodyFat: Double?,
    val targetWeight: Double?,
    val dietPlan: String?,
    val isSynced: Int = 0
)