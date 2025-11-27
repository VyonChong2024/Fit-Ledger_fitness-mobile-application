package com.example.fyp_fitledger.data.model

data class WorkoutPlanDays(
    val day: String,
    val workoutName: String,
    val exercises: List<Exercises>
)

data class WorkoutPlan(
    val day: String,
    val workoutName: String,
    val exercises: List<Exercises>,
    val createdDate: String,
    val planName: String
)

data class Exercises(
    val name: String,
    val sets: Int,
    val reps: Int
)