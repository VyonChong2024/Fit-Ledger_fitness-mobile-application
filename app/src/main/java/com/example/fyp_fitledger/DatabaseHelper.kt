package com.example.fyp_fitledger

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader

class DatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        // Create table statement
        executeSqlFromFile(db, "create_table.sql")
        executeSqlFromFile(db, "insert_statements.sql")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS users")
        onCreate(db)
    }

    companion object {
        private const val DATABASE_NAME = "mydatabase.db"
        private const val DATABASE_VERSION = 1
    }

    private fun executeSqlFromFile(db: SQLiteDatabase, fileName: String) {
        try {
            val assetFiles = context.assets.list("")
            Log.d("SQLite", "Files in assets: ${assetFiles?.joinToString()}")

            // Check if the file exists
            if (!assetFiles!!.contains(fileName)) {
                Log.e("SQLite", "File '$fileName' not found in assets!")
                return
            }

            val inputStream = context.assets.open(fileName) // Now context is accessible
            val reader = BufferedReader(InputStreamReader(inputStream))

            var line: String?
            val stringBuilder = StringBuilder()

            while (reader.readLine().also { line = it } != null) {
                stringBuilder.append(line).append("\n")  // Append each line
            }

            reader.close()
            inputStream.close()

            val sqlCommands = stringBuilder.toString().split(";")  // Split by semicolon

            for (command in sqlCommands) {
                if (command.trim().isNotEmpty()) {
                    db.execSQL(command.trim())  // Execute each command
                }
            }
            Log.d("SQLite", "SQL file executed successfully!")

        } catch (e: Exception) {
            Log.e("SQLite", "Error executing SQL file", e)
        }
    }

    @SuppressLint("Range")
    fun getColumnData(tableName: String, columnName: String): List<String?> {
        val resultList = mutableListOf<String?>()
        val db = readableDatabase // Use readableDatabase for querying
        var cursor: Cursor? = null

        try {
            val projection = arrayOf(columnName) // Only select the specified column
            cursor = db.query(
                tableName,
                projection,
                null, // No WHERE clause to get all rows
                null,
                null,
                null,
                null
            )

            cursor.let {
                while (it.moveToNext()) {
                    val columnIndex = it.getColumnIndexOrThrow(columnName)
                    resultList.add(it.getString(columnIndex)) // Assuming the column is of TEXT type
                    // If the column is of a different type (INTEGER, REAL, BLOB),
                    // use the appropriate cursor method (getInt, getDouble, getBlob).
                }
            }
        } catch (e: Exception) {
            Log.e("SQLite", "Error retrieving data from $tableName.$columnName", e)
        } finally {
            cursor?.close()
            db.close() // Consider if you want to keep the database open for other operations
        }

        return resultList
    }


    //For Workout Log Activity
    fun getLatestPlanIdForUser(userId: String): Int? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT Plan_ID FROM WorkoutPlan WHERE User_ID = ? ORDER BY CreatedDate DESC LIMIT 1", arrayOf(userId))
        return if (cursor.moveToFirst()) cursor.getInt(0) else null
    }

    fun getPlanDayId(planId: Int?, dayName: String): Int? {
        if (planId == null) return null
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT PlanDay_ID FROM WorkoutPlanDay WHERE Plan_ID = ? AND DayName = ?", arrayOf(planId.toString(), dayName))
        return if (cursor.moveToFirst()) cursor.getInt(0) else null
    }

    fun getSetsAndRepsForExercise(planDayId: Int?, exerciseName: String): Pair<Int?, Int?> {
        if (planDayId == null) return Pair(null, null)
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT Sets, Reps FROM WorkoutPlanExercise WHERE PlanDay_ID = ? AND ExerciseName = ?", arrayOf(planDayId.toString(), exerciseName))
        return if (cursor.moveToFirst()) Pair(cursor.getInt(0), cursor.getInt(1)) else Pair(null, null)
    }

    fun getPreviousRecord(userId: String, exerciseName: String, setNo: Int): String? {
        val db = readableDatabase
        val cursor = db.rawQuery("""
            SELECT es.WeightUsed, es.Reps 
            FROM ExerciseSet es
            JOIN WorkoutExercise we ON es.WorkoutExercise_ID = we.WorkoutExercise_ID
            JOIN WorkoutLog wl ON we.Log_ID = wl.Log_ID
            JOIN Exercise e ON we.Exercise_ID = e.Exercise_ID
            WHERE wl.User_ID = ? AND e.Name = ? AND es.Set_No = ?
            ORDER BY wl.Date DESC, wl.StartTime DESC LIMIT 1
        """, arrayOf(userId, exerciseName, setNo.toString()))
        return if (cursor.moveToFirst()) "${cursor.getDouble(0)} kg * ${cursor.getInt(1)}" else null
    }

    fun getExerciseIdByName(name: String): Int? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT Exercise_ID FROM Exercise WHERE Name = ?", arrayOf(name))
        return if (cursor.moveToFirst()) cursor.getInt(0) else null.also { cursor.close() }
    }


    fun insertWorkoutExercise(logId: Int, exerciseId: Int): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("Log_ID", logId)
            put("Exercise_ID", exerciseId)
        }
        return db.insert("WorkoutExercise", null, values).toInt()
    }

    fun insertExerciseSet(workoutExerciseId: Int, setNo: String, reps: Int, weight: Float): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("WorkoutExercise_ID", workoutExerciseId)
            put("Set_No", setNo)
            put("Reps", reps)
            put("WeightUsed", weight)
        }
        return db.insert("ExerciseSet", null, values)
    }

    fun insertWorkoutLog(userId: String, date: String, duration: Int): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("User_ID", userId)
            put("Date", date)
            put("Duration", duration)
        }
        return db.insert("WorkoutLog", null, values).toInt()
    }

    fun deleteWorkoutExercise(workoutExerciseId: Int) {
        writableDatabase.delete("WorkoutExercise", "WorkoutExercise_ID = ?", arrayOf(workoutExerciseId.toString()))
    }



    fun importFoodDataFromJson(context: Context, jsonFileName: String) {
        val db = this.writableDatabase
        val json = loadJsonFromAssets(context, jsonFileName)
        val rootJson = parseFoodJson(json)
        val foodList = rootJson.FoundationFoods

        db.beginTransaction()
        try {
            for (item in foodList) {
                val nutrientMap = item.foodNutrients.associateBy { it.nutrient?.name }

                fun get(name: String, vararg altNames: String): Double {
                    val allNames = listOf(name) + altNames

                    // Handle "Energy" kcal case
                    if (allNames.any { it.contains("Energy", ignoreCase = true) }) {
                        val energyNutrient = item.foodNutrients.find {
                            it.nutrient?.name in allNames && it.nutrient?.unitName?.lowercase() == "kcal"
                        }
                        return energyNutrient?.amount?.toDouble() ?: 0.0
                    }

                    val nutrient = allNames.firstNotNullOfOrNull { key ->
                        item.foodNutrients.find { it.nutrient?.name == key }
                    }

                    return nutrient?.amount?.toDouble() ?: 0.0
                }

                val foodValues = ContentValues().apply {
                    put("Food_Name", item.description)
                    put("Calories", get("Energy (Atwater General Factors)", "Energy"))
                    put("Protein", get("Protein"))
                    put("Carbohydrates", get("Carbohydrate, by difference"))
                    put("Fat", get("Total lipid (fat)"))
                    put("Iron", get("Iron, Fe"))
                    put("Calcium", get("Calcium, Ca"))
                    put("Potassium", get("Potassium, K"))
                    put("Magnesium", get("Magnesium, Mg"))
                    put("Zinc", get("Zinc, Zn"))
                    put("Sodium", get("Sodium, Na"))
                    put("VitaminD", get("Vitamin D (D2 + D3)", "Vitamin D"))
                    put("VitaminA", get("Vitamin A, RAE", "Vitamin A"))
                    put("VitaminC", get("Vitamin C, total ascorbic acid"))
                    put("VitaminK", get("Vitamin K (phylloquinone)"))
                    put("VitaminB12", get("Vitamin B-12"))
                    put("Category", item.foodCategory?.description ?: "Unknown")
                }

                val foodId = db.insert("Food", null, foodValues)

                item.foodPortions?.forEach { portion ->
                    if (portion.measureUnit?.name != null && portion.gramWeight != null) {
                        val portionValues = ContentValues().apply {
                            put("Food_ID", foodId)
                            put("Unit", portion.measureUnit.name)
                            put("UnitValue", portion.gramWeight.toString())
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

    fun loadJsonFromAssets(context: Context, fileName: String): String {
        return context.assets.open(fileName).bufferedReader().use { it.readText() }
    }

    data class RootJson(
        val FoundationFoods: List<FoodJson>
    )

    data class FoodJson(
        val fdcId: Int,
        val description: String,
        val foodNutrients: List<FoodNutrient>,
        val foodCategory: FoodCategory?,
        val foodPortions: List<FoodPortionJson>?
    )

    // This method should now be able to parse the JSON correctly
    fun parseFoodJson(json: String): RootJson {
        val gson = Gson()
        return gson.fromJson(json, RootJson::class.java)
    }

    // DatabaseHelper.kt

    fun getFoodByName(foodName: String): Food? {
        val db = readableDatabase
        val cursor = db.query(
            "Food", // The table name
            null, // Columns (null means all columns)
            "Food_Name = ?", // Selection
            arrayOf(foodName), // Selection arguments
            null, // Group By
            null, // Having
            null // Order By
        )

        return if (cursor.moveToFirst()) {
            val food = Food(
                Food_ID = cursor.getInt(cursor.getColumnIndexOrThrow("Food_ID")),
                Food_Name = cursor.getString(cursor.getColumnIndexOrThrow("Food_Name")),
                Calories = cursor.getDouble(cursor.getColumnIndexOrThrow("Calories")),
                Protein = cursor.getDouble(cursor.getColumnIndexOrThrow("Protein")),
                Carbohydrates = cursor.getDouble(cursor.getColumnIndexOrThrow("Carbohydrates")),
                Fat = cursor.getDouble(cursor.getColumnIndexOrThrow("Fat")),
                Iron = cursor.getDouble(cursor.getColumnIndexOrThrow("Iron")),
                Calcium = cursor.getDouble(cursor.getColumnIndexOrThrow("Calcium")),
                Potassium = cursor.getDouble(cursor.getColumnIndexOrThrow("Potassium")),
                Magnesium = cursor.getDouble(cursor.getColumnIndexOrThrow("Magnesium")),
                Zinc = cursor.getDouble(cursor.getColumnIndexOrThrow("Zinc")),
                Sodium = cursor.getDouble(cursor.getColumnIndexOrThrow("Sodium")),
                VitaminD = cursor.getDouble(cursor.getColumnIndexOrThrow("VitaminD")),
                VitaminA = cursor.getDouble(cursor.getColumnIndexOrThrow("VitaminA")),
                VitaminC = cursor.getDouble(cursor.getColumnIndexOrThrow("VitaminC")),
                VitaminK = cursor.getDouble(cursor.getColumnIndexOrThrow("VitaminK")),
                VitaminB12 = cursor.getDouble(cursor.getColumnIndexOrThrow("VitaminB12")),
                Category = cursor.getString(cursor.getColumnIndexOrThrow("Category"))
            )
            cursor.close()
            food
        } else {
            cursor.close()
            null
        }
    }

    fun getPortionsByFoodId(foodId: Int): List<FoodPortion> {
        val db = readableDatabase
        val cursor = db.query(
            "FoodPortion", // The table name
            null, // Columns (null means all columns)
            "Food_ID = ?", // Selection
            arrayOf(foodId.toString()), // Selection arguments
            null, // Group By
            null, // Having
            null // Order By
        )

        val portions = mutableListOf<FoodPortion>()
        while (cursor.moveToNext()) {
            val portion = FoodPortion(
                FoodPortion_ID = cursor.getInt(cursor.getColumnIndexOrThrow("FoodPortion_ID")),
                Food_ID = cursor.getInt(cursor.getColumnIndexOrThrow("Food_ID")),
                Unit = cursor.getString(cursor.getColumnIndexOrThrow("Unit")),
                UnitValue = cursor.getString(cursor.getColumnIndexOrThrow("UnitValue"))
            )
            portions.add(portion)
        }
        cursor.close()
        return portions
    }


    fun getUserByUid(uid: String): User? {
        val db = readableDatabase
        val cursor = db.query(
            "User",
            null,
            "User_ID = ?",
            arrayOf(uid),
            null,
            null,
            null
        )

        var user: User? = null
        if (cursor.moveToFirst()) {
            val userId = cursor.getString(cursor.getColumnIndexOrThrow("User_ID"))

            user = User(userId)
        }

        cursor.close()
        return user
    }


    data class User(
        val userId: String,
    )
}

