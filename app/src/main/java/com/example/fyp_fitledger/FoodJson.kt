package com.example.fyp_fitledger

import androidx.room.Entity
import androidx.room.PrimaryKey

data class FoodJson(
    val fdcId: Int,
    val description: String,
    val foodNutrients: List<FoodNutrient>,
    val foodCategory: FoodCategory?,
    val foodPortions: List<FoodPortionJson>?
)

data class FoodNutrient(
    val amount: Double?,
    val nutrient: NutrientInfo?
)

data class NutrientInfo(
    val name: String?,
    val unitName: String?
)

data class FoodCategory(
    val description: String?
)

data class FoodPortionJson(
    val measureUnit: MeasureUnit?,
    val gramWeight: Double?
)

data class MeasureUnit(
    val name: String?
)

@Entity(tableName = "FoodPortion")
data class FoodPortion(
    @PrimaryKey(autoGenerate = true) val FoodPortion_ID: Int = 0,
    val Food_ID: Int,
    val Unit: String,
    val UnitValue: String
)

data class RootJson(
    val FoundationFoods: List<FoodJson>
)


