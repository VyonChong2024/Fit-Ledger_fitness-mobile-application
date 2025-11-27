package com.example.fyp_fitledger.data.model

// One set of an exercise
data class WorkoutSet(
    val setNo: String,
    val reps: Int,
    val weightUsed: Double
)

// One exercise with all its sets
data class WorkoutExercise(
    val exerciseId: Long,
    val sets: List<WorkoutSet>
)

// Complete workout log
data class WorkoutLog(
    val date: String,
    val startTime: Long,
    val duration: Int,
    val notes: String,
    val exercises: List<WorkoutExercise>
)

data class WorkoutMuscleData(
    val date: String,
    val muscles: List<String>
)

data class SetEntry(
    var reps: String? = null,
    var weight: String? = null,
    var checked: Boolean = false
)