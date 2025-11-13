package com.example.fyp_fitledger.data.model

data class WorkoutPlanDays(
    val day: String,
    val workoutName: String,
    val exercises: List<Exercises>
)

data class Exercises(
    val name: String,
    val sets: Int,
    val reps: Int
)