package com.example.fyp_fitledger.data.local.dao

import android.content.Context
import com.example.fyp_fitledger.data.model.FoodModel
import com.example.fyp_fitledger.data.model.FoodPortion
import com.example.fyp_fitledger.data.model.RootJson

interface FoodDao {

    fun insertFood(food: FoodModel): Long
    fun insertFoodPortion(portion: FoodPortion): Long
    fun getAllFoodName(): List<String>
    fun getFoodByName(name: String): FoodModel?
    fun getFoodById(foodId: Long): FoodModel?
    fun getFoodIdByName(name: String): Long?
    fun getPortionsByFoodId(foodId: Long): List<FoodPortion>
    fun searchFoods(keyword: String): List<FoodModel>
    fun getFoodsByCategory(category: String): List<FoodModel>

    fun importFoodDataFromJson(context: Context, jsonFileName: String)
    fun loadJsonFromAssets(context: Context, fileName: String): String
    fun parseFoodJson(json: String): RootJson
}