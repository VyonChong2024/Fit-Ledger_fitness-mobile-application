package com.example.fyp_fitledger

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WorkoutPlanViewModel : ViewModel() {
    var goal: String? = null
    var targetBodyFat: Double? = null
    var targetWeight: Double? = null
    var activityLevel: String? = null
    var activityFrequency: String? = null
    var targetMuscles: String? = null
    var daySelected: String? = null
    var duration: Int? = null
    var dietPlan: String? = null

    // Functions to update values
    fun updateGoal(goal: List<String>) {
        this.goal = goal.joinToString(", ")
    }
    fun updateGoal(goal: String) {
        this.goal = goal
    }

    fun updateTargetBodyFat(bodyFat: Double) {
        this.targetBodyFat = bodyFat
    }

    fun updateTargetWeight(weight: Double) {
        this.targetWeight = weight
    }

    fun updateActivityLevel(level: String) {
        this.activityLevel = level
    }

    fun updateActivityFrequency(frequency: String) {
        this.activityFrequency = frequency
    }

    fun updateTargetMuscles(muscles: List<String>) {
        this.targetMuscles = muscles.joinToString(", ")
    }
    fun updateTargetMuscles(muscles: String) {
        this.targetMuscles = muscles
    }

    fun updateDaySelected(day: List<String>) {
        this.daySelected = day.joinToString(", ")
    }
    fun updateDaySelected(day: String) {
        this.daySelected = day
    }

    fun updateDuration(duration: Int) {
        this.duration = duration
    }

    fun updateDietPlan(plan: String) {
        this.dietPlan = plan
    }

    // Load user current weight & body fat % (from SharedPreferences)
    fun loadUserData(context: Context) {
        val sharedPref = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val weight = sharedPref.getFloat("weight", 0f).toDouble()
        val bodyFat = sharedPref.getFloat("bodyFatPercent", 0f).toDouble()
    }
}
