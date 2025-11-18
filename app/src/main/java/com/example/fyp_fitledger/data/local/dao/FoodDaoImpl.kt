package com.example.fyp_fitledger.data.local.dao

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.util.Log
import com.example.fyp_fitledger.data.local.DatabaseHelper
import com.example.fyp_fitledger.data.model.FoodModel
import com.example.fyp_fitledger.data.model.FoodPortion
import com.example.fyp_fitledger.data.model.RootJson
import com.google.gson.Gson
import kotlin.text.contains

class FoodDaoImpl(private val dbHelper: DatabaseHelper): FoodDao {

    override fun insertFood(food: FoodModel): Long {
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put("Food_Name", food.Food_Name)
            put("Calories", food.Calories)
            put("Protein", food.Protein)
            put("Carbohydrates", food.Carbohydrates)
            put("Fat", food.Fat)
            put("Iron", food.Iron)
            put("Calcium", food.Calcium)
            put("Potassium", food.Potassium)
            put("Magnesium", food.Magnesium)
            put("Zinc", food.Zinc)
            put("Sodium", food.Sodium)
            put("VitaminD", food.VitaminD)
            put("VitaminA", food.VitaminA)
            put("VitaminC", food.VitaminC)
            put("VitaminK", food.VitaminK)
            put("VitaminB12", food.VitaminB12)
            put("Category", food.Category)
        }

        return db.insert("Food", null, values)
    }

    override fun insertFoodPortion(portion: FoodPortion): Long {
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put("Food_ID", portion.Food_ID)
            put("Unit", portion.Unit)
            put("UnitValue", portion.UnitValue)
        }

        return db.insert("FoodPortion", null, values)
    }

    override fun getAllFoodName(): List<String> {
        val db = dbHelper.readableDatabase
        val foodNames = mutableListOf<String>()

        val cursor = db.rawQuery("SELECT Food_Name FROM Food", null)
        if (cursor.moveToFirst()) {
            do {
                val name = cursor.getString(cursor.getColumnIndexOrThrow("Food_Name"))
                foodNames.add(name)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return foodNames
    }

    override fun getFoodByName(name: String): FoodModel? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "Food",
            null,
            "Food_Name = ?",
            arrayOf(name),
            null, null, null
        )

        cursor.use {
            if (!cursor.moveToFirst()) return null
            return cursor.toFoodModel()
        }
    }

    override fun getFoodById(foodId: Long): FoodModel? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "Food",
            null,
            "Food_ID = ?",
            arrayOf(foodId.toString()),
            null, null, null
        )

        cursor.use {
            if (!cursor.moveToFirst()) return null
            return cursor.toFoodModel()
        }
    }

    override fun getFoodIdByName(name: String): Long? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT Food_ID FROM Food WHERE Food_Name = ? LIMIT 1",
            arrayOf(name)
        )

        cursor.use {
            return if (cursor.moveToFirst()) cursor.getLong(0) else null
        }
    }

    override fun getPortionsByFoodId(foodId: Long): List<FoodPortion> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "FoodPortion",
            null,
            "Food_ID = ?",
            arrayOf(foodId.toString()),
            null, null, null
        )

        val list = mutableListOf<FoodPortion>()

        cursor.use {
            while (cursor.moveToNext()) {
                list.add(
                    FoodPortion(
                        FoodPortion_ID = cursor.getLong(cursor.getColumnIndexOrThrow("FoodPortion_ID")),
                        Food_ID = cursor.getLong(cursor.getColumnIndexOrThrow("Food_ID")),
                        Unit = cursor.getString(cursor.getColumnIndexOrThrow("Unit")),
                        UnitValue = cursor.getString(cursor.getColumnIndexOrThrow("UnitValue"))
                    )
                )
            }
        }
        return list
    }

    override fun searchFoods(keyword: String): List<FoodModel> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM Food WHERE Food_Name LIKE ?",
            arrayOf("%$keyword%")
        )

        val list = mutableListOf<FoodModel>()

        cursor.use {
            while (cursor.moveToNext()) {
                list.add(cursor.toFoodModel())
            }
        }

        return list
    }

    override fun getFoodsByCategory(category: String): List<FoodModel> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "Food",
            null,
            "Category = ?",
            arrayOf(category),
            null, null, null
        )

        val list = mutableListOf<FoodModel>()

        cursor.use {
            while (cursor.moveToNext()) {
                list.add(cursor.toFoodModel())
            }
        }

        return list
    }

    // Convert cursor → FoodModel
    private fun Cursor.toFoodModel(): FoodModel {
        return FoodModel(
            Food_ID = getLong(getColumnIndexOrThrow("Food_ID")),
            Food_Name = getString(getColumnIndexOrThrow("Food_Name")),
            Calories = getDouble(getColumnIndexOrThrow("Calories")),
            Protein = getDouble(getColumnIndexOrThrow("Protein")),
            Carbohydrates = getDouble(getColumnIndexOrThrow("Carbohydrates")),
            Fat = getDouble(getColumnIndexOrThrow("Fat")),
            Iron = getDouble(getColumnIndexOrThrow("Iron")),
            Calcium = getDouble(getColumnIndexOrThrow("Calcium")),
            Potassium = getDouble(getColumnIndexOrThrow("Potassium")),
            Magnesium = getDouble(getColumnIndexOrThrow("Magnesium")),
            Zinc = getDouble(getColumnIndexOrThrow("Zinc")),
            Sodium = getDouble(getColumnIndexOrThrow("Sodium")),
            VitaminD = getDouble(getColumnIndexOrThrow("VitaminD")),
            VitaminA = getDouble(getColumnIndexOrThrow("VitaminA")),
            VitaminC = getDouble(getColumnIndexOrThrow("VitaminC")),
            VitaminK = getDouble(getColumnIndexOrThrow("VitaminK")),
            VitaminB12 = getDouble(getColumnIndexOrThrow("VitaminB12")),
            Category = getString(getColumnIndexOrThrow("Category"))
        )
    }

    override fun importFoodDataFromJson(context: Context, jsonFileName: String) {
        val db = dbHelper.writableDatabase
        val json = loadJsonFromAssets(context, jsonFileName)
        val rootJson = parseFoodJson(json)
        val foodList = rootJson.FoundationFoods

        db.beginTransaction()
        try {
            for (item in foodList) {
                val nutrientMap = item.foodNutrients.associateBy { it.nutrient?.name ?: "" }

                fun get(vararg names: String): Double {
                    val key = names.firstOrNull { it in nutrientMap }
                    return nutrientMap[key]?.amount?.toDouble() ?: 0.0
                }

                // Special handling: Energy (kcal only)
                fun getEnergyKcal(): Double {
                    return item.foodNutrients.firstOrNull {
                        it.nutrient?.name?.contains("Energy", ignoreCase = true) == true &&
                                it.nutrient?.unitName?.equals("kcal", ignoreCase = true) == true
                    }?.amount?.toDouble() ?: 0.0
                }

                val foodValues = ContentValues().apply {
                    put("Food_Name", item.description)
                    put("Calories", getEnergyKcal())
                    put("Protein", get("Protein"))
                    put("Carbohydrates", get("Carbohydrate, by difference"))
                    put("Fat", get("Total lipid (fat)"))

                    // Minerals
                    put("Iron", get("Iron, Fe"))
                    put("Calcium", get("Calcium, Ca"))
                    put("Potassium", get("Potassium, K"))
                    put("Magnesium", get("Magnesium, Mg"))
                    put("Zinc", get("Zinc, Zn"))
                    put("Sodium", get("Sodium, Na"))

                    // Vitamins
                    put("VitaminD", get("Vitamin D (D2 + D3)", "Vitamin D"))
                    put("VitaminA", get("Vitamin A, RAE", "Vitamin A"))
                    put("VitaminC", get("Vitamin C, total ascorbic acid"))
                    put("VitaminK", get("Vitamin K (phylloquinone)"))
                    put("VitaminB12", get("Vitamin B-12"))

                    put("Category", item.foodCategory?.description ?: "Unknown")
                }

                // Insert Food
                val foodId = db.insert("Food", null, foodValues)

                // Insert Portions
                item.foodPortions?.forEach { portion ->
                    val unit = portion.measureUnit?.name
                    val gramWeight = portion.gramWeight

                    if (unit != null && gramWeight != null) {
                        val portionValues = ContentValues().apply {
                            put("Food_ID", foodId)
                            put("Unit", unit)
                            put("UnitValue", gramWeight.toString())
                        }

                        db.insert("FoodPortion", null, portionValues)
                        Log.d("Insert-Portion", "Inserted portion for Food_ID: $foodId")
                    }
                }
            }

            db.setTransactionSuccessful()

        } catch (e: Exception) {
            Log.e("DB-Import", "Error during food import", e)
        } finally {
            db.endTransaction()
        }
    }

    override fun loadJsonFromAssets(context: Context, fileName: String): String {
        return context.assets.open(fileName).bufferedReader().use { it.readText() }
    }

    override fun parseFoodJson(json: String): RootJson {
        val gson = Gson()
        return gson.fromJson(json, RootJson::class.java)
    }
}