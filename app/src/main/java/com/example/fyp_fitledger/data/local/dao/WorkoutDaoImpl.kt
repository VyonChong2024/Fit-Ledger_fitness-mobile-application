package com.example.fyp_fitledger.data.local.dao

import android.content.ContentValues
import com.example.fyp_fitledger.data.local.DatabaseHelper
import com.example.fyp_fitledger.data.model.WorkoutMuscleData

class WorkoutDaoImpl(private val dbHelper: DatabaseHelper): WorkoutDao {

    // WorkoutPlan & PlanDay
    // ----------------------

    override fun insertWorkoutPlan(userId: String, planName: String, createdDate: String): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("User_ID", userId)
            put("PlanName", planName)
            put("CreatedDate", createdDate)
        }
        return db.insert("WorkoutPlan", null, values)
    }

    override fun insertWorkoutPlanDay(planId: Long, dayName: String, workoutName: String): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("Plan_ID", planId)
            put("DayName", dayName)
            put("WorkoutName", workoutName)
        }
        return db.insert("WorkoutPlanDay", null, values)
    }

    override fun insertWorkoutPlanExercise(planDayId: Long, exerciseName: String, sets: Int, reps: Int): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("PlanDay_ID", planDayId)
            put("ExerciseName", exerciseName)
            put("Sets", sets)
            put("Reps", reps)
        }
        return db.insert("WorkoutPlanExercise", null, values)
    }

    override fun getLatestPlanIdForUser(userId: String): Int? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT Plan_ID FROM WorkoutPlan WHERE User_ID = ? ORDER BY CreatedDate DESC LIMIT 1",
            arrayOf(userId)
        )
        cursor.use { return if (it.moveToFirst()) it.getInt(0) else null }
    }

    override fun getPlanDayId(planId: Int?, dayName: String): Int? {
        if (planId == null) return null
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT PlanDay_ID FROM WorkoutPlanDay WHERE Plan_ID = ? AND DayName = ?",
            arrayOf(planId.toString(), dayName)
        )
        cursor.use { return if (it.moveToFirst()) it.getInt(0) else null }
    }

    override fun getTodayWorkoutName(userId: String, dayName: String): String? {
        val db = dbHelper.readableDatabase

        val cursor = db.rawQuery(
            """
            SELECT wpd.WorkoutName
            FROM WorkoutPlan wp
            JOIN WorkoutPlanDay wpd ON wp.Plan_ID = wpd.Plan_ID
            WHERE wp.User_ID = ? AND wpd.DayName = ?
            ORDER BY wp.CreatedDate DESC
            LIMIT 1
        """.trimIndent(),
            arrayOf(userId, dayName)
        )

        cursor.use {
            return if (it.moveToFirst()) it.getString(0) else null
        }
    }

    override fun getTodayExercisePlanName(userId: String, dayName: String): List<String> {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<String>()

        val cursor = db.rawQuery(
            """
                SELECT wpe.ExerciseName 
                FROM WorkoutPlan wp
                JOIN WorkoutPlanDay wpd ON wp.Plan_ID = wpd.Plan_ID
                JOIN WorkoutPlanExercise wpe ON wpd.PlanDay_ID = wpe.PlanDay_ID
                WHERE wpd.DayName = ? AND wp.User_ID = ?
            """.trimIndent(),
            arrayOf(dayName, userId)
        )

        while (cursor.moveToNext()) {
            val name = cursor.getString(0)
            if (!name.isNullOrEmpty()) {
                list.add(name)
            }
        }

        cursor.close()
        db.close()

        return list
    }

    override fun getSetsAndRepsForExercise(planDayId: Int?, exerciseName: String): Pair<Int?, Int?> {
        if (planDayId == null) return Pair(null, null)
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT Sets, Reps FROM WorkoutPlanExercise WHERE PlanDay_ID = ? AND ExerciseName = ?",
            arrayOf(planDayId.toString(), exerciseName)
        )
        cursor.use { return if (it.moveToFirst()) Pair(it.getInt(0), it.getInt(1)) else Pair(null, null) }
    }


    override fun markWorkoutPlanSynced(id: Long) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply { put("isSynced", 1) }
        db.update("WorkoutPlan", values, "Plan_ID = ?", arrayOf(id.toString()))
    }

    override fun markWorkoutPlanDaySynced(id: Long) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply { put("isSynced", 1) }
        db.update("WorkoutPlanDay", values, "PlanDay_ID = ?", arrayOf(id.toString()))
    }

    override fun markWorkoutPlanExerciseSynced(id: Long) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply { put("isSynced", 1) }
        db.update("WorkoutPlanExercise", values, "PlanExercise_ID = ?", arrayOf(id.toString()))
    }


    // WorkoutLog & Exercises
    // ----------------------

    override fun insertWorkoutLog(userId: String, date: String, startTime: Long, duration: Int, notes: String?): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("User_ID", userId)
            put("Date", date)
            put("StartTime", startTime)
            put("Duration", duration)
            put("Notes", notes)
        }
        return db.insert("WorkoutLog", null, values)
    }

    override fun insertWorkoutExercise(logId: Long, exerciseId: Long): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("Log_ID", logId)
            put("Exercise_ID", exerciseId)
        }
        return db.insert("WorkoutExercise", null, values)
    }

    override fun insertExerciseSet(workoutExerciseId: Long, setNo: String, reps: Int, weight: Double): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("WorkoutExercise_ID", workoutExerciseId)
            put("Set_No", setNo)
            put("Reps", reps)
            put("WeightUsed", weight)
        }
        return db.insert("ExerciseSet", null, values)
    }

    override fun getPreviousRecord(userId: String, exerciseName: String, setNo: Int): String? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("""
            SELECT es.WeightUsed, es.Reps
            FROM ExerciseSet es
            JOIN WorkoutExercise we ON es.WorkoutExercise_ID = we.WorkoutExercise_ID
            JOIN WorkoutLog wl ON we.Log_ID = wl.Log_ID
            JOIN Exercise e ON we.Exercise_ID = e.Exercise_ID
            WHERE wl.User_ID = ? AND e.Name = ? AND es.Set_No = ?
            ORDER BY wl.Date DESC, wl.StartTime DESC LIMIT 1
        """, arrayOf(userId, exerciseName, setNo.toString()))
        cursor.use { return if (it.moveToFirst()) "${it.getDouble(0)} kg * ${it.getInt(1)}" else null }
    }

    override fun getExerciseNamesByDate(userId: String, date: String): List<String> {
        val db = dbHelper.readableDatabase
        val exerciseNames = mutableListOf<String>()

        val query = """
            SELECT DISTINCT e.Name
            FROM ExerciseSet es
            JOIN WorkoutExercise we ON es.WorkoutExercise_ID = we.WorkoutExercise_ID
            JOIN Exercise e ON we.Exercise_ID = e.Exercise_ID
            JOIN WorkoutLog wl ON we.Log_ID = wl.Log_ID
            WHERE wl.User_ID = ? AND wl.Date = ?
        """

        val cursor = db.rawQuery(query, arrayOf(userId, date))
        cursor.use {
            while (it.moveToNext()) {
                exerciseNames.add(it.getString(it.getColumnIndexOrThrow("Name")))
            }
        }

        return exerciseNames
    }

    override fun getMuscleGroupForExercise(exerciseName: String): String? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT MuscleGroup FROM Exercise WHERE Name = ?", arrayOf(exerciseName))
        var result: String? = null
        if (cursor.moveToFirst()) {
            result = cursor.getString(cursor.getColumnIndexOrThrow("MuscleGroup"))
        }
        cursor.close()
        return result
    }

    override fun getMusclesTrainedByDate(userId: String, date: String): WorkoutMuscleData {
        val db = dbHelper.readableDatabase
        val muscles = mutableListOf<String>()

        val cursor = db.rawQuery(
            """
            SELECT e.MuscleGroup
            FROM WorkoutLog wl
            JOIN WorkoutExercise we ON we.Log_ID = wl.Log_ID
            JOIN Exercise e ON e.Exercise_ID = we.Exercise_ID
            WHERE wl.Date = ? AND wl.User_ID = ?
            """.trimIndent(),
            arrayOf(date, userId)
        )

        while (cursor.moveToNext()) {
            val muscle = cursor.getString(0)
            if (!muscle.isNullOrEmpty()) {
                muscles.add(muscle)   // return raw value
            }
        }

        cursor.close()
        db.close()

        return WorkoutMuscleData(date, muscles)
    }


    // Delete functions with cascade
    // ----------------------
    override fun deleteWorkoutExercise(workoutExerciseId: Long) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            // Delete ExerciseSets first
            db.delete("ExerciseSet", "WorkoutExercise_ID = ?", arrayOf(workoutExerciseId.toString()))
            // Then delete WorkoutExercise
            db.delete("WorkoutExercise", "WorkoutExercise_ID = ?", arrayOf(workoutExerciseId.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun deleteWorkoutLog(logId: Long) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            // Delete all WorkoutExercises & their sets
            val cursor = db.query("WorkoutExercise", arrayOf("WorkoutExercise_ID"), "Log_ID = ?", arrayOf(logId.toString()), null, null, null)
            cursor.use {
                while (it.moveToNext()) {
                    val exerciseId = it.getLong(it.getColumnIndexOrThrow("WorkoutExercise_ID"))
                    deleteWorkoutExercise(exerciseId)
                }
            }
            // Delete WorkoutLog
            db.delete("WorkoutLog", "Log_ID = ?", arrayOf(logId.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }


    override fun markWorkoutLogSynced(logId: Long) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply { put("isSynced", 1) }
        db.update("WorkoutLog", values, "Log_ID = ?", arrayOf(logId.toString()))
    }

    override fun markWorkoutExerciseSynced(exerciseId: Long) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply { put("isSynced", 1) }
        db.update("WorkoutExercise", values, "WorkoutExercise_ID = ?", arrayOf(exerciseId.toString()))
    }

    override fun markExerciseSetSynced(exerciseSetId: Long) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply { put("isSynced", 1) }
        db.update("ExerciseSet", values, "ExerciseSet_ID = ?", arrayOf(exerciseSetId.toString()))
    }
}