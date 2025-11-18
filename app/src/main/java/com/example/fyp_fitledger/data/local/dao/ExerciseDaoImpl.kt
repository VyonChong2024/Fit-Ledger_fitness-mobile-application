package com.example.fyp_fitledger.data.local.dao

import android.content.ContentValues
import com.example.fyp_fitledger.data.local.DatabaseHelper
import com.example.fyp_fitledger.data.model.Exercise

class ExerciseDaoImpl(private val dbHelper: DatabaseHelper): ExerciseDao {

    override fun insertExercise(exercise: Exercise): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("Name", exercise.name)
            put("Instruction", exercise.instruction)
            put("Category", exercise.category)
            put("MuscleGroup", exercise.muscleGroup)
            put("EquipmentUsed", exercise.equipmentUsed)
            put("Gif_URL", exercise.gifUrl)
        }
        return db.insert("Exercise", null, values)
    }

    override fun getExerciseById(id: Long): Exercise? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM Exercise WHERE Exercise_ID = ?",
            arrayOf(id.toString())
        )

        cursor.use {
            if (cursor.moveToFirst()) {
                return Exercise(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("Exercise_ID")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("Name")),
                    instruction = cursor.getString(cursor.getColumnIndexOrThrow("Instruction")),
                    category = cursor.getString(cursor.getColumnIndexOrThrow("Category")),
                    muscleGroup = cursor.getString(cursor.getColumnIndexOrThrow("MuscleGroup")),
                    equipmentUsed = cursor.getString(cursor.getColumnIndexOrThrow("EquipmentUsed")),
                    gifUrl = cursor.getString(cursor.getColumnIndexOrThrow("Gif_URL"))
                )
            }
        }
        return null
    }

    override fun getExerciseIdByName(name: String): Long? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT Exercise_ID FROM Exercise WHERE Name = ?",
            arrayOf(name)
        )
        cursor.use {
            return if (it.moveToFirst()) it.getLong(0) else null
        }
    }

    override fun getExerciseByName(name: String): Exercise? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM Exercise WHERE Name = ?",
            arrayOf(name)
        )
        cursor.use {
            if (cursor.moveToFirst()) {
                return Exercise(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("Exercise_ID")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("Name")),
                    instruction = cursor.getString(cursor.getColumnIndexOrThrow("Instruction")),
                    category = cursor.getString(cursor.getColumnIndexOrThrow("Category")),
                    muscleGroup = cursor.getString(cursor.getColumnIndexOrThrow("MuscleGroup")),
                    equipmentUsed = cursor.getString(cursor.getColumnIndexOrThrow("EquipmentUsed")),
                    gifUrl = cursor.getString(cursor.getColumnIndexOrThrow("Gif_URL"))
                )
            }
        }
        return null
    }

    override fun getAllExercises(): List<Exercise> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM Exercise", null)

        val list = mutableListOf<Exercise>()
        cursor.use {
            while (cursor.moveToNext()) {
                list.add(
                    Exercise(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("Exercise_ID")),
                        name = cursor.getString(cursor.getColumnIndexOrThrow("Name")),
                        instruction = cursor.getString(cursor.getColumnIndexOrThrow("Instruction")),
                        category = cursor.getString(cursor.getColumnIndexOrThrow("Category")),
                        muscleGroup = cursor.getString(cursor.getColumnIndexOrThrow("MuscleGroup")),
                        equipmentUsed = cursor.getString(cursor.getColumnIndexOrThrow("EquipmentUsed")),
                        gifUrl = cursor.getString(cursor.getColumnIndexOrThrow("Gif_URL"))
                    )
                )
            }
        }
        return list
    }

    override fun getAllExerciseNames(): List<String> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT Name FROM Exercise", null)

        val names = mutableListOf<String>()

        cursor.use {
            while (cursor.moveToNext()) {
                names.add(cursor.getString(cursor.getColumnIndexOrThrow("Name")))
            }
        }

        return names
    }

    override fun deleteExercise(id: Long): Boolean {
        val db = dbHelper.writableDatabase
        return db.delete("Exercise", "Exercise_ID = ?", arrayOf(id.toString())) > 0
    }

    override fun updateExercise(exercise: Exercise): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("Name", exercise.name)
            put("Instruction", exercise.instruction)
            put("Category", exercise.category)
            put("MuscleGroup", exercise.muscleGroup)
            put("EquipmentUsed", exercise.equipmentUsed)
            put("Gif_URL", exercise.gifUrl)
        }
        val rows = db.update(
            "Exercise",
            values,
            "Exercise_ID = ?",
            arrayOf(exercise.id.toString())
        )
        return rows > 0
    }
}