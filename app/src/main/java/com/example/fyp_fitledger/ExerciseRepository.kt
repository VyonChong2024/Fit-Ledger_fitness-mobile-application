package com.example.fyp_fitledger

class ExerciseRepository(private val dbHelper: DatabaseHelper) {
    fun getExerciseSummaries(): List<ExerciseSummary> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT Name, Gif_URL, Category, EquipmentUsed FROM Exercise", null)
        val summaries = mutableListOf<ExerciseSummary>()

        if (cursor.moveToFirst()) {
            do {
                val name = cursor.getString(0)
                val gifUrl = cursor.getString(1)
                val category = cursor.getString(2) ?: ""
                val equipment = cursor.getString(3) ?: ""
                summaries.add(
                    ExerciseSummary(
                        name = name,
                        category = category,
                        equipment = equipment,
                        gifUrl = gifUrl
                    )
                )
            } while (cursor.moveToNext())
        }

        cursor.close()
        return summaries
    }

    fun getExerciseByName(name: String): ExerciseDetailData {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM Exercise WHERE name = ?", arrayOf(name))

        if (cursor.moveToFirst()) {
            val instruction = cursor.getString(cursor.getColumnIndexOrThrow("Instruction"))
            val category = cursor.getString(cursor.getColumnIndexOrThrow("Category"))
            val muscles = cursor.getString(cursor.getColumnIndexOrThrow("MuscleGroup"))
            val equipment = cursor.getString(cursor.getColumnIndexOrThrow("EquipmentUsed"))
            val gifUrl = cursor.getString(cursor.getColumnIndexOrThrow("Gif_URL"))

            cursor.close()
            return ExerciseDetailData(name, instruction, category, muscles, equipment, gifUrl)
        } else {
            cursor.close()
            throw Exception("Exercise not found")
        }
    }

}


data class ExerciseSummary(
    val name: String,
    val category: String,
    val equipment: String?,
    val gifUrl: String
)

data class ExerciseDetailData(
    val name: String,
    val instruction: String,
    val category: String,
    val muscles: String,
    val equipment: String,
    val gifUrl: String
)

