package com.example.fyp_fitledger

import android.content.Context
import androidx.lifecycle.ViewModel

class UserViewModel : ViewModel() {
    var userID: String? = null
    var gender: String? = null
    var age: Int? = null
    var height: Double? = null
    var weight: Double? = null
    var bodyFatPercent: Double? = null
    var bodyMassIndex: Double? = null
    var bodyDensity: Double? = null
    var accFat: Boolean = false

    fun updateUserID(id: String) {
        this.userID = id
    }

    fun updateGender(selectedGender: String) {
        this.gender = selectedGender
    }

    fun updateAge(selectedAge: Int) {
        this.age = selectedAge
    }

    fun updateHeight(selectedHeight: Double) {
        this.height = selectedHeight
    }

    fun updateWeight(selectedWeight: Double) {
        this.weight = selectedWeight
    }

    fun updateBodyFatPercent(calcBodyFatPercent: Double) {
        this.bodyFatPercent = calcBodyFatPercent
    }

    fun updateBodyMassIndex(calcBodyMassIndex: Double) {
        this.bodyMassIndex = calcBodyMassIndex
    }

    fun updateBodyDensity(calcBodyDensity: Double) {
        this.bodyDensity = calcBodyDensity
    }

    fun updateAccuFat(accFat: Boolean) {
        this.accFat = accFat
    }

    fun saveToPreferences(context: Context) {
        val sharedPref = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("gender", gender)
            putInt("age", age?: 0)
            putFloat("height", height?.toFloat() ?: 0f)
            putFloat("weight", weight?.toFloat() ?: 0f)
            putFloat("bodyFatPercent", bodyFatPercent?.toFloat() ?: 0f)
            apply()
        }
    }

    fun loadFromPreferences(context: Context) {
        val sharedPref = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        gender = sharedPref.getString("gender", null)
        age = sharedPref.getInt("age", 0)
        height = sharedPref.getFloat("height", 0f).toDouble()
        weight = sharedPref.getFloat("weight", 0f).toDouble()
        bodyFatPercent = sharedPref.getFloat("bodyFatPercent", 0f).toDouble()
    }
}
