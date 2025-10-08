package com.example.fyp_fitledger

import android.content.ContentValues
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import java.time.LocalDate

class FinishSetupFragment : Fragment() {

    private lateinit var btnNext: Button

    private lateinit var userViewModel: UserViewModel
    private lateinit var workoutPlanViewModel: WorkoutPlanViewModel

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var database: SQLiteDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_finish_setup, container, false)

        btnNext = view.findViewById(R.id.btContinue)

        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)
        workoutPlanViewModel = ViewModelProvider(requireActivity()).get(WorkoutPlanViewModel::class.java)

        dbHelper = DatabaseHelper(requireContext())
        database = dbHelper.writableDatabase


        val userId = userViewModel.userID
        val gender = userViewModel.gender
        val age = userViewModel.age
        val height = userViewModel.height
        val weight = userViewModel.weight
        val bodyFatPercent = userViewModel.bodyFatPercent

        val targetBodyFat = workoutPlanViewModel.targetBodyFat
        val targetWeight = workoutPlanViewModel.targetWeight
        val dietPlan = workoutPlanViewModel.dietPlan

        Log.d("---Finish Fragment", "user id: $userId")
        Log.d("---Finish Fragment", "gender: $gender")
        Log.d("---Finish Fragment", "age: $age")
        Log.d("---Finish Fragment", "height: $height")
        Log.d("---Finish Fragment", "weight: $weight")
        Log.d("---Finish Fragment", "bodyFatPercent: $bodyFatPercent")

        Log.d("---Finish Fragment", "targetBodyFat: $targetBodyFat")
        Log.d("---Finish Fragment", "targetWeight: $targetWeight")
        Log.d("---Finish Fragment", "bodyFatPercent: $bodyFatPercent")


        val workoutPlan = WorkoutPlanFragment.workoutPlan?.let { parseWorkoutPlans(it) } ?: emptyList()

        if(userId != null) {
            try {
                database.beginTransaction()
                try {
                    //insert user table data
                    insertUserData(userId, gender!!, age!!, height!!, weight!!, bodyFatPercent!!, targetBodyFat, targetWeight, dietPlan!!)

                    //insert workout plan data
                    if (!workoutPlan.isEmpty()) {
                        val planId = insertWorkoutPlan(userId, "April Plan")
                        for (day in workoutPlan) {
                            val dayId = insertWorkoutPlanDay(planId, day.day, day.workoutName)
                            for (exercise in day.exercises) {
                                insertWorkoutPlanExercise(dayId, exercise.name, exercise.sets, exercise.reps)
                            }
                        }
                    }

                    //insert body fat and weight history record
                    insertBodyFatRecord(userId, bodyFatPercent)
                    insertWeightRecord(userId, weight)

                    //insert nutrient plan data
                    val nutrientPlan = DietNutrientPlanFragment.responseString
                    Log.d("---------------", "nutrient plan: $nutrientPlan")
                    val nutrientPlanList = parseNutrientPlan(nutrientPlan)
                    insertNutrientPlan(userId, nutrientPlanList)

                    //Final Data saved, set the user is complete setup
                    markSetupCompleteForUser(userId)

                    database.setTransactionSuccessful()
                    btnNext.isEnabled = true
                    Toast.makeText(requireContext(), "Saving data successful", Toast.LENGTH_SHORT).show()
                    Log.d("FinishSetupFragment", "Saving data successful")
                } finally {
                    database.endTransaction()
                    Log.d("FinishSetupFragment", "Transaction End")
                }
            }
            catch (e: Exception) {
                btnNext.isEnabled = false
                Toast.makeText(requireContext(), "Unexpected error occurs", Toast.LENGTH_SHORT).show()
                Log.e("FinishSetupFragment", "Error saving data: ${e.message}", e)
            }

        } else {
            btnNext.isEnabled = false
            Log.e("FinishSetupFragment", "User ID is null")
            Toast.makeText(requireContext(), "User ID is null", Toast.LENGTH_SHORT).show()
        }


        btnNext.setOnClickListener {
            val intent = Intent(requireActivity(), HomeActivity::class.java)
            startActivity(intent)
            requireActivity().supportFragmentManager.beginTransaction()
                .remove(this)
                .commit()
        }

        return view
    }

    fun parseWorkoutPlans(response: String): List<WorkoutPlanDays> {
        val lines = response.trim().split("\n")
        val plan = mutableListOf<WorkoutPlanDays>()

        for (line in lines) {
            val parts = line.split("|")
            if (parts.size >= 3) {
                val day = parts[0].trim()
                val workoutName = parts[1].trim()
                val exercises = parts.drop(2).mapNotNull {
                    val split = it.split(":")
                    if (split.size == 3) {
                        val name = split[0].trim()
                        val sets = split[1].trim().toIntOrNull()
                        val reps = split[2].trim().toIntOrNull()
                        if (sets != null && reps != null) {
                            Exercises(name, sets, reps)
                        } else null
                    } else null
                }
                plan.add(WorkoutPlanDays(day, workoutName, exercises))
            }
        }

        return plan
    }

    fun parseNutrientPlan(input: String): List<Float> {
        val valueList = mutableListOf<Float>()
        val entries = input.split("|")

        for (entry in entries) {
            if (entry.isNotBlank()) {
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val value = parts[1].trim()
                    valueList.add(value.toFloat())
                } else {
                    Log.d("FinishSetupFragment", "Error occurs in parse nutritient plan")
                }
            }
        }
        return valueList.toList() // Convert to immutable List if needed
    }

    fun insertWorkoutPlan(userId: String, name: String): Long {
        val values = ContentValues().apply {
            put("User_ID", userId)
            put("PlanName", name)
            put("CreatedDate", LocalDate.now().toString())
        }
        return database.insert("WorkoutPlan", null, values)
    }

    fun insertWorkoutPlanDay(planId: Long, day: String, workoutName: String): Long {
        val values = ContentValues().apply {
            put("Plan_ID", planId)
            put("DayName", day)
            put("WorkoutName", workoutName)
        }
        return database.insert("WorkoutPlanDay", null, values)
    }

    fun insertWorkoutPlanExercise(planDayId: Long, name: String, sets: Int, reps: Int): Long {
        val values = ContentValues().apply {
            put("PlanDay_ID", planDayId)
            put("ExerciseName", name)
            put("Sets", sets)
            put("Reps", reps)
        }
        return database.insert("WorkoutPlanExercise", null, values)
    }

    fun insertUserData(userId: String, gender: String, age: Int, height: Double, weight: Double, bodyFatPercent: Double, targetBodyFat: Double?, targetWeight: Double?, dietPlan: String): Long {

        val values = ContentValues().apply {
            put("User_ID", userId)
            put("Gender", gender)
            put("Age", age)
            put("Height", height)
            put("Weight", weight)
            put("BodyFatPercent", bodyFatPercent)
            put("TargetBodyFat", if (targetBodyFat == null) bodyFatPercent else targetBodyFat)  //used current body fat if target no set
            put("TargetWeight", if (targetWeight == null) weight else targetWeight) //used current weight if target no set
            put("DietPlan", dietPlan)
        }
        return database.insert("User", null, values)
    }

    fun insertBodyFatRecord(userId: String, bodyFatPercent: Double): Long {
        val values = ContentValues().apply {
            put("User_ID", userId)
            put("BodyFatPercent", bodyFatPercent)
            put("Date", LocalDate.now().toString())
        }
        return database.insert("BodyFatHistory", null, values)
    }

    fun insertWeightRecord(userId: String, weight: Double): Long {
        val values = ContentValues().apply {
            put("User_ID", userId)
            put("Weight", weight)
            put("Date", LocalDate.now().toString())
        }
        return database.insert("WeightHistory", null, values)
    }

    fun insertNutrientPlan(userId: String, nutrientValue: List<Float>): Long {
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
        }
        return database.insert("NutrientPlan", null, values)
    }

    fun markSetupCompleteForUser(uid: String) {
        requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit()
            .putBoolean("setupComplete_$uid", true)
            .apply()
    }
}

data class WorkoutPlanDays(
    val day: String,
    val workoutName: String,
    val exercises: List<Exercises>
)

data class Exercises(
    val name: String,
    val sets: Int,
    val reps: Int
)