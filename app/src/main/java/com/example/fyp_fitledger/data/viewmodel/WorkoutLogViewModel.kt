package com.example.fyp_fitledger.data.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.fyp_fitledger.data.model.SetEntry
import com.google.common.reflect.TypeToken
import com.google.gson.Gson

class WorkoutLogViewModel : ViewModel() {
    val addedExercises: MutableList<String> = mutableListOf()
    val exerciseEntries: MutableMap<String, MutableList<SetEntry>> = mutableMapOf()
    var startTimeMillis: Long = 0L

    private val PREF_KEY = "WORKOUT_TEMP_JSON"

    fun addExercise(name: String) {
        if (!addedExercises.contains(name)) {
            addedExercises.add(name)
                exerciseEntries[name] = mutableListOf(SetEntry())
        }
    }

    fun removeExercise(name: String) {
        addedExercises.remove(name)
        exerciseEntries.remove(name)
    }

    fun clearExercises() {
        addedExercises.clear()
        exerciseEntries.clear()
    }

    fun ensureSetsForExercise(name: String, count: Int) {
        val list = exerciseEntries.getOrPut(name) { mutableListOf() }
        while (list.size < count) list.add(SetEntry())
    }

    fun updateSetEntry(name: String, setIndex: Int, reps: String?, weight: String?, checked: Boolean) {
        if (setIndex < 0) return
        val list = exerciseEntries.getOrPut(name) { mutableListOf() }
        while (list.size <= setIndex) list.add(SetEntry())
        val e = list[setIndex]
        e.reps = reps
        e.weight = weight
        e.checked = checked
    }

    fun saveToPrefs(context: Context) {
        try {
            val prefs = context.getSharedPreferences("WorkoutTemp", Context.MODE_PRIVATE)
            val map = HashMap<String, Any?>()
            map["exercises"] = addedExercises
            map["entries"] = exerciseEntries
            map["startTimeMillis"] = startTimeMillis
            val json = Gson().toJson(map)
            prefs.edit().putString(PREF_KEY, json).apply()
        } catch (_: Exception) {}
    }

    fun loadFromPrefs(context: Context) {
        try {
            val prefs = context.getSharedPreferences("WorkoutTemp", Context.MODE_PRIVATE)
            val json = prefs.getString(PREF_KEY, null) ?: return
            val gson = Gson()
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val parsed = gson.fromJson<Map<String, Any>>(json, type)

            // load exercises
            val ex = parsed["exercises"]
            if (ex is List<*>) {
                addedExercises.clear()
                ex.forEach { if (it is String) addedExercises.add(it) }
            }

            // load entries
            val entriesObj = parsed["entries"]
            if (entriesObj is Map<*, *>) {
                exerciseEntries.clear()
                for ((k, v) in entriesObj) {
                    if (k is String && v is List<*>) {
                        val list = mutableListOf<SetEntry>()
                        for (item in v) {
                            if (item is Map<*, *>) {
                                val reps = item["reps"] as? String
                                val weight = item["weight"] as? String
                                val checked = when (val c = item["checked"]) {
                                    is Boolean -> c
                                    is Number -> c.toInt() != 0
                                    else -> false
                                }
                                list.add(SetEntry(reps, weight, checked))
                            }
                        }
                        if (list.isNotEmpty()) exerciseEntries[k] = list
                    }
                }
            }
        } catch (_: Exception) {}
    }

    fun clearPrefs(context: Context) {
        try {
            val prefs = context.getSharedPreferences("WorkoutTemp", Context.MODE_PRIVATE)
            prefs.edit().remove(PREF_KEY).apply()
        } catch (_: Exception) {}
    }
}
