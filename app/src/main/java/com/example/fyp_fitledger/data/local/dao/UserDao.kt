package com.example.fyp_fitledger.data.local.dao

import com.example.fyp_fitledger.data.model.User

interface UserDao {
    fun insertUser(user: User): Long
    fun updateUser(user: User): Int
    fun getUserById(userId: String): User?
    fun isUserExist(userId: String): Boolean
    fun markUserSynced(userId: String): Int

    fun updateHeight(userId: String, height: Double): Boolean
    fun updateWeight(userId: String, weight: Double, now: String): Long
    fun updateBodyFatPercent(userId: String, bodyFat: Double, now: String): Long
    fun updateTargetBodyFat(userId: String, targetBodyFat: Double): Boolean
    fun updateTargetWeight(userId: String, targetWeight: Double): Boolean

    fun getUserColumn(userId: String, columnName: String): Any?
    fun updateUserColumns(userId: String, updates: Map<String, Any>): Boolean

    fun getWeightHistory(userId: String): List<Pair<Double, String>>
    fun markWeightSynced(weightHistoryId: Long)

    fun markBodyFatSynced(bodyFatHistoryId: Long)
    fun getBodyFatHistory(userId: String): List<Pair<Double, String>>

}