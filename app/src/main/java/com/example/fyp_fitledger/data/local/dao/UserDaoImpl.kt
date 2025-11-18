package com.example.fyp_fitledger.data.local.dao

import android.content.ContentValues
import android.database.Cursor
import com.example.fyp_fitledger.data.local.DatabaseHelper
import com.example.fyp_fitledger.data.model.User

class UserDaoImpl(private val dbHelper: DatabaseHelper): UserDao {

    override fun insertUser(user: User): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("User_ID", user.userId)
            put("Gender", user.gender)
            put("Age", user.age)
            put("Height", user.height)
            put("Weight", user.weight)
            put("BodyFatPercent", user.bodyFatPercent)
            put("TargetBodyFat", user.targetBodyFat)
            put("TargetWeight", user.targetWeight)
            put("DietPlan", user.dietPlan)
            put("isSynced", user.isSynced)
        }
        return db.insert("User", null, values)
    }

    override fun updateUser(user: User): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("Gender", user.gender)
            put("Age", user.age)
            put("Height", user.height)
            put("Weight", user.weight)
            put("BodyFatPercent", user.bodyFatPercent)
            put("TargetBodyFat", user.targetBodyFat)
            put("TargetWeight", user.targetWeight)
            put("DietPlan", user.dietPlan)
            put("isSynced", user.isSynced)
        }
        return db.update(
            "User",
            values,
            "User_ID = ?",
            arrayOf(user.userId)
        )
    }

    override fun getUserById(userId: String): User? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "User",
            null,
            "User_ID = ?",
            arrayOf(userId),
            null,
            null,
            null
        )

        var user: User? = null

        if (cursor.moveToFirst()) {
            user = User(
                userId = cursor.getString(cursor.getColumnIndexOrThrow("User_ID")),
                gender = cursor.getString(cursor.getColumnIndexOrThrow("Gender")),
                age = cursor.getInt(cursor.getColumnIndexOrThrow("Age")),
                height = cursor.getDouble(cursor.getColumnIndexOrThrow("Height")),
                weight = cursor.getDouble(cursor.getColumnIndexOrThrow("Weight")),
                bodyFatPercent = cursor.getDouble(cursor.getColumnIndexOrThrow("BodyFatPercent")),
                targetBodyFat = cursor.getDouble(cursor.getColumnIndexOrThrow("TargetBodyFat")),
                targetWeight = cursor.getDouble(cursor.getColumnIndexOrThrow("TargetWeight")),
                dietPlan = cursor.getString(cursor.getColumnIndexOrThrow("DietPlan")),
                //isSynced = cursor.getInt(cursor.getColumnIndexOrThrow("isSynced"))
            )
        }

        cursor.close()
        return user
    }

    override fun isUserExist(userId: String): Boolean {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT 1 FROM User WHERE User_ID = ? LIMIT 1",
            arrayOf(userId)
        )
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }

    override fun markUserSynced(userId: String): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("isSynced", 1)
        }
        return db.update("User", values, "User_ID = ?", arrayOf(userId))
    }

    override fun updateWeight(userId: String, weight: Double, now: String): Long {
        val suc = updateUserColumns(userId, mapOf("Weight" to weight))
        if (suc) {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("User_ID", userId)
                put("Weight", weight)
                put("Date", now)
                put("isSynced", 0)
            }
            return db.insert("WeightHistory", null, values)
        }
        return -1L
    }

    override fun updateBodyFatPercent(userId: String, bodyFat: Double, now: String): Long {
        val suc = updateUserColumns(userId, mapOf("BodyFatPercent" to bodyFat))
        if (suc) {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("User_ID", userId)
                put("BodyFatPercent", bodyFat)
                put("Date", now)
                put("isSynced", 0)
            }
            return db.insert("BodyFatHistory", null, values)
        }
        return -1L
    }

    override fun updateHeight(userId: String, height: Double) = updateUserColumns(userId, mapOf("Height" to height))
    override fun updateTargetBodyFat(userId: String, targetBodyFat: Double) = updateUserColumns(userId, mapOf("TargetBodyFat" to targetBodyFat))
    override fun updateTargetWeight(userId: String, targetWeight: Double) = updateUserColumns(userId, mapOf("TargetWeight" to targetWeight))

    override fun getWeightHistory(userId: String): List<Pair<Double, String>> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT Weight, Date FROM WeightHistory WHERE User_ID = ? ORDER BY Date ASC",
            arrayOf(userId)
        )
        val list = mutableListOf<Pair<Double, String>>()
        while (cursor.moveToNext()) {
            val weight = cursor.getDouble(cursor.getColumnIndexOrThrow("Weight"))
            val date = cursor.getString(cursor.getColumnIndexOrThrow("Date"))
            list.add(weight to date)
        }
        cursor.close()
        return list
    }

    override fun markWeightSynced(weightHistoryId: Long) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply { put("isSynced", 1) }
        db.update("WeightHistory", values, "Weight_His_ID = ?", arrayOf(weightHistoryId.toString()))
    }


    override fun getBodyFatHistory(userId: String): List<Pair<Double, String>> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT BodyFatPercent, Date FROM BodyFatHistory WHERE User_ID = ? ORDER BY Date ASC",
            arrayOf(userId)
        )
        val list = mutableListOf<Pair<Double, String>>()
        while (cursor.moveToNext()) {
            val bodyFat = cursor.getDouble(cursor.getColumnIndexOrThrow("BodyFatPercent"))
            val date = cursor.getString(cursor.getColumnIndexOrThrow("Date"))
            list.add(bodyFat to date)
        }
        cursor.close()
        return list
    }

    override fun markBodyFatSynced(bodyFatHistoryId: Long) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply { put("isSynced", 1) }
        db.update("BodyFatHistory", values, "BodyFat_His_ID = ?", arrayOf(bodyFatHistoryId.toString()))
    }


    // Generic getter
    override fun getUserColumn(userId: String, columnName: String): Any? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "User",
            arrayOf(columnName),
            "User_ID = ?",
            arrayOf(userId),
            null,
            null,
            null
        )
        var result: Any? = null
        if (cursor.moveToFirst()) {
            result = when (cursor.getType(0)) {
                Cursor.FIELD_TYPE_INTEGER -> cursor.getInt(0)
                Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(0)
                Cursor.FIELD_TYPE_STRING -> cursor.getString(0)
                else -> null
            }
        }
        cursor.close()
        return result
    }

    // Generic updater
    override fun updateUserColumns(userId: String, updates: Map<String, Any>): Boolean {
        val db = dbHelper.writableDatabase
        val contentValues = ContentValues()
        updates.forEach { (key, value) ->
            when (value) {
                is Int -> contentValues.put(key, value)
                is Float -> contentValues.put(key, value)
                is Double -> contentValues.put(key, value)
                is String -> contentValues.put(key, value)
                is Long -> contentValues.put(key, value)
                else -> throw IllegalArgumentException("Unsupported type for column $key")
            }
        }
        val rows = db.update("User", contentValues, "User_ID = ?", arrayOf(userId))
        return rows > 0
    }
}