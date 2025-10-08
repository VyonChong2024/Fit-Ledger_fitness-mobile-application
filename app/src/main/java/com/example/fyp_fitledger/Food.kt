package com.example.fyp_fitledger

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Food")
data class Food(
    @PrimaryKey(autoGenerate = true) val Food_ID: Int = 0,
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


