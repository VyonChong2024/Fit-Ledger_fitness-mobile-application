package com.example.fyp_fitledger.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Food")
data class FoodModel(
    @PrimaryKey(autoGenerate = true) val Food_ID: Long = 0,
    val Food_Name: String,
    val Calories: Double?,
    val Protein: Double?,
    val Carbohydrates: Double?,
    val Fat: Double?,
    val Iron: Double?,
    val Calcium: Double?,
    val Potassium: Double?,
    val Magnesium: Double?,
    val Zinc: Double?,
    val Sodium: Double?,
    val VitaminD: Double?,
    val VitaminA: Double?,
    val VitaminC: Double?,
    val VitaminK: Double?,
    val VitaminB12: Double?,
    val Category: String?
)

@Entity(tableName = "FoodPortion")
data class FoodPortion(
    @PrimaryKey(autoGenerate = true) val FoodPortion_ID: Long = 0,
    val Food_ID: Long,
    val Unit: String,
    val UnitValue: String
)

data class FoodPortionValue(
    val name: String,
    val amount: Double
)

data class MealLog(
    val date: String,
    val time: String,
    val notes: String,
    val foods: List<FoodPortionValue>
)

data class Nutrients(
    val calories: Float,
    val protein: Float,
    val carbohydrates: Float,
    val fat: Float,
    val iron: Float,
    val calcium: Float,
    val potassium: Float,
    val magnesium: Float,
    val zinc: Float,
    val sodium: Float,
    val vitaminD: Float,
    val vitaminA: Float,
    val vitaminC: Float,
    val vitaminK: Float,
    val vitaminB12: Float
)

data class NutrientWithQty(
    val calories: Float,
    val protein: Float,
    val carbohydrates: Float,
    val fat: Float,
    val iron: Float,
    val calcium: Float,
    val potassium: Float,
    val magnesium: Float,
    val zinc: Float,
    val sodium: Float,
    val vitaminD: Float,
    val vitaminA: Float,
    val vitaminC: Float,
    val vitaminK: Float,
    val vitaminB12: Float,
    val quantity: Float
)