package com.example.fyp_fitledger.ui.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.fyp_fitledger.data.local.DatabaseHelper
import com.example.fyp_fitledger.data.repo.FirebaseRepository
import com.example.fyp_fitledger.ui.activity.HomeActivity
import com.example.fyp_fitledger.R
import com.example.fyp_fitledger.data.local.dao.MealDao
import com.example.fyp_fitledger.data.local.dao.MealDaoImpl
import com.example.fyp_fitledger.data.local.dao.UserDao
import com.example.fyp_fitledger.data.local.dao.UserDaoImpl
import com.example.fyp_fitledger.data.local.dao.WorkoutDao
import com.example.fyp_fitledger.data.local.dao.WorkoutDaoImpl
import com.example.fyp_fitledger.data.viewmodel.UserViewModel
import com.example.fyp_fitledger.data.viewmodel.WorkoutPlanViewModel
import com.example.fyp_fitledger.data.model.Exercises
import com.example.fyp_fitledger.data.model.User
import com.example.fyp_fitledger.data.model.WorkoutPlanDays
import java.time.LocalDate

class FinishSetupFragment : Fragment() {

    private lateinit var btnNext: Button

    private lateinit var userViewModel: UserViewModel
    private lateinit var workoutPlanViewModel: WorkoutPlanViewModel

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var userDao: UserDao
    private lateinit var mealDao: MealDao
    private lateinit var workoutDao: WorkoutDao
    private lateinit var firebaseRepo: FirebaseRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_finish_setup, container, false)

        btnNext = view.findViewById(R.id.btContinue)

        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)
        workoutPlanViewModel = ViewModelProvider(requireActivity()).get(WorkoutPlanViewModel::class.java)

        dbHelper = DatabaseHelper(requireContext())
        firebaseRepo = FirebaseRepository()

        userDao = UserDaoImpl(dbHelper)
        mealDao = MealDaoImpl(dbHelper)
        workoutDao = WorkoutDaoImpl(dbHelper)

        // Retrieve all user information
        val userId = userViewModel.userID
        val gender = userViewModel.gender
        val age = userViewModel.age
        val height = userViewModel.height
        val weight = userViewModel.weight
        val bodyFatPercent = userViewModel.bodyFatPercent
        val targetBodyFat = workoutPlanViewModel.targetBodyFat
        val targetWeight = workoutPlanViewModel.targetWeight
        val dietPlan = workoutPlanViewModel.dietPlan
        val workoutPlan = WorkoutPlanFragment.workoutPlan?.let { parseWorkoutPlans(it) } ?: emptyList()

        val today = LocalDate.now().toString()

        // stored the primary key of each data in database table
        val workoutPlanIds = mutableListOf<Long>()
        val workoutPlanDayIds = mutableListOf<Long>()
        val workoutExerciseIds = mutableListOf<Long>()
        val bodyFatIds = mutableListOf<Long>()
        val weightIds = mutableListOf<Long>()
        val nutrientIds = mutableListOf<Long>()

        if(userId != null) {
            try {
                //insert user table data
                userDao.insertUser(User(userId, gender!!, age!!, height!!, weight!!, bodyFatPercent!!, targetBodyFat, targetWeight, dietPlan))

                //insert workout plan data
                if (!workoutPlan.isEmpty()) {
                    val planId = workoutDao.insertWorkoutPlan(userId, today + " Plan", today)
                    workoutPlanIds.add(planId)
                    for (day in workoutPlan) {
                        val dayId = workoutDao.insertWorkoutPlanDay(planId, day.day, day.workoutName)
                        workoutPlanDayIds.add(dayId)
                        for (exercise in day.exercises) {
                            val exerciseId = workoutDao.insertWorkoutPlanExercise(dayId, exercise.name, exercise.sets, exercise.reps)
                            workoutExerciseIds.add(exerciseId)
                        }
                    }
                }

                //insert body fat and weight history record
                val dbBodyFatId = userDao.updateBodyFatPercent(userId, bodyFatPercent, today)
                val dbWeightId = userDao.updateWeight(userId, weight, today)
                bodyFatIds.add(dbBodyFatId)
                weightIds.add(dbWeightId)

                //insert nutrient plan data
                val nutrientPlan = DietNutrientPlanFragment.responseString
                val nutrientPlanList = parseNutrientPlan(nutrientPlan)
                val nutrientId = mealDao.insertNutrientPlan(userId, nutrientPlanList)
                nutrientIds.add(nutrientId)

                firebaseRepo.saveUserSetupData(
                    gender, age, height, weight, bodyFatPercent,
                    targetBodyFat, targetWeight, dietPlan, workoutPlan, nutrientPlanList) { success, error ->
                    if (success) {
                        //Mark data as synced
                        userDao.markUserSynced(userId)
                        workoutPlanIds.forEach { workoutDao.markWorkoutPlanSynced(it) }
                        workoutPlanDayIds.forEach { workoutDao.markWorkoutPlanDaySynced(it) }
                        workoutExerciseIds.forEach { workoutDao.markWorkoutPlanExerciseSynced(it) }
                        nutrientIds.forEach { mealDao.markNutrientPlanSynced(it) }

                        Log.d("MainActivity", "Data saved to Firebase")

                        firebaseRepo.saveBodyFatHistory(bodyFatPercent, today ) { success, error ->
                            if (success) {
                                bodyFatIds.forEach { userDao.markBodyFatSynced(it) }
                                Log.d("MainActivity", "Body fat history saved to Firebase")
                            }
                            else Log.e("MainActivity", "Save failed: $error")
                        }
                        firebaseRepo.saveWeightHistory(weight!!, today) { success, error ->
                            if (success) {
                                weightIds.forEach { userDao.markWeightSynced(it) }
                                Log.d("MainActivity", "Weight history saved to Firebase")
                            }
                            else Log.e("MainActivity", "Save failed: $error")
                        }
                    }
                    else Log.e("MainActivity", "Save failed: $error")
                }

                btnNext.isEnabled = true
                Toast.makeText(requireContext(), "Saving data successful", Toast.LENGTH_SHORT).show()
                Log.d("FinishSetupFragment", "Saving data successful")
            } finally {
                Log.d("FinishSetupFragment", "Transaction End")
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
}