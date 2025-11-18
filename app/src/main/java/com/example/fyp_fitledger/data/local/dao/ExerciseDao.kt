package com.example.fyp_fitledger.data.local.dao

import com.example.fyp_fitledger.data.model.Exercise

interface ExerciseDao {

    fun insertExercise(exercise: Exercise): Long
    fun getExerciseById(id: Long): Exercise?
    fun getExerciseIdByName(name: String): Long?
    fun getExerciseByName(name: String): Exercise?
    fun getAllExercises(): List<Exercise>
    fun getAllExerciseNames(): List<String>
    fun deleteExercise(id: Long): Boolean
    fun updateExercise(exercise: Exercise): Boolean
}