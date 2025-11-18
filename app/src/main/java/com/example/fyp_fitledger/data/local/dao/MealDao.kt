package com.example.fyp_fitledger.data.local.dao

import com.example.fyp_fitledger.data.model.MealLogFoods
import com.example.fyp_fitledger.data.model.MealLogs
import com.example.fyp_fitledger.data.model.NutrientWithQty
import com.example.fyp_fitledger.data.model.Nutrients

interface MealDao {

    fun insertMealLog(mealLog: MealLogs): Long
    fun insertMealLogFood(mealLogFood: MealLogFoods): Long
    fun getMealLogsByUser(userId: String): List<MealLogs>
    fun getMealLogFoods(logId: Long): List<MealLogFoods>
    fun deleteMealLog(logId: Long)
    fun deleteMealLogFood(mealLogId: Long)

    fun insertNutrientPlan(userId: String, nutrientValue: List<Float>): Long
    fun insertNutrientPlan(userId: String, nutrientValue: Nutrients): Long
    fun getNutrientPlanByUserId(userId: String): Nutrients?
    fun getAllNutrientByDate(userId: String, date: String): List<NutrientWithQty>
    fun getTotalCaloriesByDate(userId: String, date: String): Double
    fun getMacronutrientsByDate(userId: String, date: String): List<Triple<Double, Double, Double>>

    fun markMealLogSynced(id: Long)
    fun markMealLogFoodSynced(id: Long)
    fun markNutrientPlanSynced(id: Long)
}