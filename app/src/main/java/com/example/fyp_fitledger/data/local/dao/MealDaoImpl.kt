package com.example.fyp_fitledger.data.local.dao

import android.content.ContentValues
import android.util.Log
import com.example.fyp_fitledger.data.local.DatabaseHelper
import com.example.fyp_fitledger.data.model.MealLogFoods
import com.example.fyp_fitledger.data.model.MealLogs
import com.example.fyp_fitledger.data.model.NutrientWithQty
import com.example.fyp_fitledger.data.model.Nutrients

class MealDaoImpl(private val dbHelper: DatabaseHelper): MealDao {

    override fun insertMealLog(mealLog: MealLogs): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("User_ID", mealLog.userId)
            put("Date", mealLog.date)
            put("Time", mealLog.time)
            put("Notes", mealLog.notes)
            put("isSynced", mealLog.isSynced)
        }
        return db.insert("MealLog", null, values)
    }

    override fun insertMealLogFood(mealLogFood: MealLogFoods): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("Log_ID", mealLogFood.logId)
            put("Food", mealLogFood.food)
            put("Quantity", mealLogFood.quantity)
            put("isSynced", mealLogFood.isSynced)
        }
        return db.insert("MealLogFood", null, values)
    }

    override fun getMealLogsByUser(userId: String): List<MealLogs> {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<MealLogs>()

        val cursor = db.rawQuery(
            "SELECT Log_ID, User_ID, Date, Time, Notes, isSynced FROM MealLog WHERE User_ID = ?",
            arrayOf(userId)
        )

        if (cursor.moveToFirst()) {
            do {
                list.add(
                    MealLogs(
                        logId = cursor.getLong(0),
                        userId = cursor.getString(1),
                        date = cursor.getString(2),
                        time = cursor.getString(3),
                        notes = cursor.getString(4),
                        isSynced = cursor.getInt(5)
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    override fun getMealLogFoods(logId: Long): List<MealLogFoods> {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<MealLogFoods>()

        val cursor = db.rawQuery(
            "SELECT MealLog_ID, Log_ID, Food, Quantity, isSynced FROM MealLogFood WHERE Log_ID = ?",
            arrayOf(logId.toString())
        )

        if (cursor.moveToFirst()) {
            do {
                list.add(
                    MealLogFoods(
                        mealLogId = cursor.getLong(0),
                        logId = cursor.getLong(1),
                        food = cursor.getString(2),
                        quantity = cursor.getDouble(3),
                        isSynced = cursor.getInt(4)
                    )
                )
            } while (cursor.moveToNext())
        }

        cursor.close()
        return list
    }

    override fun deleteMealLog(logId: Long) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            // 1. Delete all MealLogFood rows that belong to this MealLog
            db.delete("MealLogFood", "Log_ID = ?", arrayOf(logId.toString()))
            // 2. Delete the MealLog itself
            db.delete("MealLog", "Log_ID = ?", arrayOf(logId.toString()))

            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e("DB-Delete", "Error deleting MealLog and related foods", e)
        } finally {
            db.endTransaction()
        }
    }

    override fun deleteMealLogFood(mealLogId: Long) {
        val db = dbHelper.writableDatabase
        db.delete("MealLogFood", "MealLog_ID = ?", arrayOf(mealLogId.toString()))
    }


    override fun insertNutrientPlan(userId: String, nutrientValue: List<Float>): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("User_ID", userId)
            put("Calories", nutrientValue[0])
            put("Protein", nutrientValue[1])
            put("Carbohydrates", nutrientValue[2])
            put("Fat", nutrientValue[3])
            put("Iron", nutrientValue[4])
            put("Calcium", nutrientValue[5])
            put("Potassium", nutrientValue[6])
            put("Magnesium", nutrientValue[7])
            put("Zinc", nutrientValue[8])
            put("Sodium", nutrientValue[9])
            put("VitaminD", nutrientValue[10])
            put("VitaminA", nutrientValue[11])
            put("VitaminC", nutrientValue[12])
            put("VitaminK", nutrientValue[13])
            put("VitaminB12", nutrientValue[14])
            put("isSynced", 0)
        }
        return db.insert("NutrientRequirement", null, values)
    }

    override fun insertNutrientPlan(userId: String, nutrientValue: Nutrients): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("User_ID", userId)
            put("Calories", nutrientValue.calories)
            put("Protein", nutrientValue.protein)
            put("Carbohydrates", nutrientValue.carbohydrates)
            put("Fat", nutrientValue.fat)
            put("Iron", nutrientValue.iron)
            put("Calcium", nutrientValue.calcium)
            put("Potassium", nutrientValue.potassium)
            put("Magnesium", nutrientValue.magnesium)
            put("Zinc", nutrientValue.zinc)
            put("Sodium", nutrientValue.sodium)
            put("VitaminD", nutrientValue.vitaminD)
            put("VitaminA", nutrientValue.vitaminA)
            put("VitaminC", nutrientValue.vitaminC)
            put("VitaminK", nutrientValue.vitaminK)
            put("VitaminB12", nutrientValue.vitaminB12)
            put("isSynced", 0)
        }
        return db.insert("NutrientRequirement", null, values)
    }

    override fun getNutrientPlanByUserId(userId: String): Nutrients? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM NutrientRequirement WHERE User_ID = ? ORDER BY NutrietReq_ID DESC LIMIT 1",
            arrayOf(userId)
        )

        cursor.use {
            if (cursor.moveToFirst()) {
                return Nutrients(
                    calories = cursor.getFloat(cursor.getColumnIndexOrThrow("Calories")),
                    protein = cursor.getFloat(cursor.getColumnIndexOrThrow("Protein")),
                    carbohydrates = cursor.getFloat(cursor.getColumnIndexOrThrow("Carbohydrates")),
                    fat = cursor.getFloat(cursor.getColumnIndexOrThrow("Fat")),
                    iron = cursor.getFloat(cursor.getColumnIndexOrThrow("Iron")),
                    calcium = cursor.getFloat(cursor.getColumnIndexOrThrow("Calcium")),
                    potassium = cursor.getFloat(cursor.getColumnIndexOrThrow("Potassium")),
                    magnesium = cursor.getFloat(cursor.getColumnIndexOrThrow("Magnesium")),
                    zinc = cursor.getFloat(cursor.getColumnIndexOrThrow("Zinc")),
                    sodium = cursor.getFloat(cursor.getColumnIndexOrThrow("Sodium")),
                    vitaminD = cursor.getFloat(cursor.getColumnIndexOrThrow("VitaminD")),
                    vitaminA = cursor.getFloat(cursor.getColumnIndexOrThrow("VitaminA")),
                    vitaminC = cursor.getFloat(cursor.getColumnIndexOrThrow("VitaminC")),
                    vitaminK = cursor.getFloat(cursor.getColumnIndexOrThrow("VitaminK")),
                    vitaminB12 = cursor.getFloat(cursor.getColumnIndexOrThrow("VitaminB12"))
                )
            }
        }
        return null
    }

    override fun getAllNutrientByDate(userId: String, date: String): List<NutrientWithQty> {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<NutrientWithQty>()

        val query = """
            SELECT f.*, mlf.Quantity
            FROM MealLog ml
            JOIN MealLogFood mlf ON ml.Log_ID = mlf.Log_ID
            JOIN Food f ON f.Food_Name = mlf.Food
            WHERE ml.User_ID = ? AND ml.Date = ?
        """

        val cursor = db.rawQuery(query, arrayOf(userId, date))

        while (cursor.moveToNext()) {

            val item = NutrientWithQty(
                calories = cursor.getFloat(cursor.getColumnIndexOrThrow("Calories")),
                protein = cursor.getFloat(cursor.getColumnIndexOrThrow("Protein")),
                carbohydrates = cursor.getFloat(cursor.getColumnIndexOrThrow("Carbohydrates")),
                fat = cursor.getFloat(cursor.getColumnIndexOrThrow("Fat")),
                iron = cursor.getFloat(cursor.getColumnIndexOrThrow("Iron")),
                calcium = cursor.getFloat(cursor.getColumnIndexOrThrow("Calcium")),
                potassium = cursor.getFloat(cursor.getColumnIndexOrThrow("Potassium")),
                magnesium = cursor.getFloat(cursor.getColumnIndexOrThrow("Magnesium")),
                zinc = cursor.getFloat(cursor.getColumnIndexOrThrow("Zinc")),
                sodium = cursor.getFloat(cursor.getColumnIndexOrThrow("Sodium")),
                vitaminD = cursor.getFloat(cursor.getColumnIndexOrThrow("VitaminD")),
                vitaminA = cursor.getFloat(cursor.getColumnIndexOrThrow("VitaminA")),
                vitaminC = cursor.getFloat(cursor.getColumnIndexOrThrow("VitaminC")),
                vitaminK = cursor.getFloat(cursor.getColumnIndexOrThrow("VitaminK")),
                vitaminB12 = cursor.getFloat(cursor.getColumnIndexOrThrow("VitaminB12")),
                quantity = cursor.getFloat(cursor.getColumnIndexOrThrow("Quantity"))
            )

            list.add(item)
        }

        cursor.close()
        return list
    }

    override fun getTotalCaloriesByDate(userId: String, date: String): Double {
        val db = dbHelper.readableDatabase
        var totalCalories = 0.0

        val query = """
            SELECT f.Calories, mf.Quantity
            FROM MealLog ml
            JOIN MealLogFood mf ON ml.Log_ID = mf.Log_ID
            JOIN Food f ON mf.Food = f.Food_Name
            WHERE ml.User_ID = ? AND ml.Date = ?
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(userId, date))
        cursor.use {
            while (it.moveToNext()) {
                val caloriesPerUnit = it.getDouble(it.getColumnIndexOrThrow("Calories"))
                val quantity = it.getDouble(it.getColumnIndexOrThrow("Quantity"))
                totalCalories += caloriesPerUnit * quantity
            }
        }

        return totalCalories
    }

    override fun getMacronutrientsByDate(userId: String, date: String): List<Triple<Double, Double, Double>> {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<Triple<Double, Double, Double>>()

        val query = """
            SELECT f.Protein, f.Carbohydrates, f.Fat, mlf.Quantity
            FROM MealLog ml
            JOIN MealLogFood mlf ON ml.Log_ID = mlf.Log_ID
            JOIN Food f ON f.Food_Name = mlf.Food
            WHERE ml.Date = ? AND ml.User_ID = ?
        """

        val cursor = db.rawQuery(query, arrayOf(date, userId))
        cursor.use {
            while (it.moveToNext()) {
                val protein = it.getDouble(it.getColumnIndexOrThrow("Protein"))
                val carbs = it.getDouble(it.getColumnIndexOrThrow("Carbohydrates"))
                val fat = it.getDouble(it.getColumnIndexOrThrow("Fat"))
                val quantity = it.getDouble(it.getColumnIndexOrThrow("Quantity"))

                list.add(Triple(protein * quantity, carbs * quantity, fat * quantity))
            }
        }

        return list
    }


    override fun markMealLogSynced(id: Long) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply { put("isSynced", 1) }
        db.update("MealLog", values, "Log_ID = ?", arrayOf(id.toString()))
    }

    override fun markMealLogFoodSynced(id: Long) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply { put("isSynced", 1) }
        db.update("MealLogFood", values, "MealLog_ID = ?", arrayOf(id.toString()))
    }

    override fun markNutrientPlanSynced(id: Long) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply { put("isSynced", 1) }
        db.update("NutrientRequirement", values, "NutrietReq_ID = ?", arrayOf(id.toString()))
    }
}