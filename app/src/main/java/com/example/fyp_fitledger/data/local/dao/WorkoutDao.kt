package com.example.fyp_fitledger.data.local.dao

import com.example.fyp_fitledger.data.model.WorkoutMuscleData

interface WorkoutDao {

    fun insertWorkoutPlan(userId: String, planName: String, createdDate: String): Long
    fun insertWorkoutPlanDay(planId: Long, dayName: String, workoutName: String): Long
    fun insertWorkoutPlanExercise(planDayId: Long, exerciseName: String, sets: Int, reps: Int): Long
    fun getLatestPlanIdForUser(userId: String): Int?
    fun getPlanDayId(planId: Int?, dayName: String): Int?
    fun getTodayExercisePlanName(userId: String, dayName: String): List<String>
    fun getSetsAndRepsForExercise(planDayId: Int?, exerciseName: String): Pair<Int?, Int?>

    fun markWorkoutPlanSynced(id: Long)
    fun markWorkoutPlanDaySynced(id: Long)
    fun markWorkoutPlanExerciseSynced(id: Long)

    fun insertWorkoutLog(userId: String, date: String, startTime: Long, duration: Int, notes: String? = null): Long
    fun insertWorkoutExercise(logId: Long, exerciseId: Long): Long
    fun insertExerciseSet(workoutExerciseId: Long, setNo: String, reps: Int, weight: Double): Long
    fun getPreviousRecord(userId: String, exerciseName: String, setNo: Int): String?
    fun getExerciseNamesByDate(userId: String, date: String): List<String>

    fun getMusclesTrainedByDate(userId: String, date: String): WorkoutMuscleData

    fun deleteWorkoutExercise(workoutExerciseId: Long)
    fun deleteWorkoutLog(logId: Long)

    fun markWorkoutLogSynced(logId: Long)
    fun markWorkoutExerciseSynced(exerciseId: Long)
    fun markExerciseSetSynced(exerciseSetId: Long)
}