package com.example.fyp_fitledger.data.repo

import android.util.Log
import com.example.fyp_fitledger.data.model.BodyFatEntry
import com.example.fyp_fitledger.data.model.FoodPortionValue
import com.example.fyp_fitledger.data.model.MealLog
import com.example.fyp_fitledger.data.model.UserProfile
import com.example.fyp_fitledger.data.model.WeightEntry
import com.example.fyp_fitledger.data.model.WorkoutLog
import com.example.fyp_fitledger.data.model.WorkoutPlanDays
import com.example.fyp_fitledger.data.model.WorkoutExercise
import com.example.fyp_fitledger.data.model.WorkoutSet
import com.example.fyp_fitledger.data.model.Exercises
import com.example.fyp_fitledger.data.model.Nutrients
import com.example.fyp_fitledger.data.model.WorkoutPlan
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = Firebase.firestore

    fun saveUserSetupData(
        gender: String,
        age: Int,
        height: Double,
        weight: Double,
        bodyFatPercent: Double,
        targetBodyFat: Double?,
        targetWeight: Double?,
        dietPlan: String?,
        workoutPlan: List<WorkoutPlanDays>,
        nutrientPlan: List<Float>,
        onResult: (Boolean, String?) -> Unit
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onResult(false, "User not signed in")
            return
        }
        val userId = currentUser.uid
        val userRef = db.collection("users").document(userId)

        val profileData = mapOf(
            "gender" to gender,
            "age" to age,
            "height" to height,
            "weight" to weight,
            "bodyFatPercent" to bodyFatPercent,
            "targetBodyFat" to (targetBodyFat ?: bodyFatPercent),
            "targetWeight" to (targetWeight ?: weight),
            "dietPlan" to (dietPlan ?: "")
        )

        val nutrientData = mapOf(
            "Calories" to nutrientPlan[0],
            "Protein" to nutrientPlan[1],
            "Carbohydrates" to nutrientPlan[2],
            "Fat" to nutrientPlan[3],
            "Iron" to nutrientPlan[4],
            "Calcium" to nutrientPlan[5],
            "Potassium" to nutrientPlan[6],
            "Magnesium" to nutrientPlan[7],
            "Zinc" to nutrientPlan[8],
            "Sodium" to nutrientPlan[9],
            "VitaminD" to nutrientPlan[10],
            "VitaminA" to nutrientPlan[11],
            "VitaminC" to nutrientPlan[12],
            "VitaminK" to nutrientPlan[13],
            "VitaminB12" to nutrientPlan[14]
        )

        val planId = userRef.collection("workoutPlans").document().id
        val planRef = userRef.collection("workoutPlans").document(planId)
        // 🔄 Save data as subcollections atomically
        db.runBatch { batch ->
            batch.set(userRef.collection("profile").document("info"), profileData)
            batch.set(userRef.collection("nutrientRequirement").document("current"), nutrientData)
            batch.set(planRef, mapOf(
                "planName" to "Default Plan",
                "createdDate" to System.currentTimeMillis(),
                "lastUpdated" to System.currentTimeMillis()
            ))
            // Save workout plan days
            workoutPlan.forEach { day ->
                val dayRef = planRef.collection("days").document(day.day)
                val exercisesData = day.exercises.map { ex ->
                    mapOf(
                        "name" to ex.name,
                        "sets" to ex.sets,
                        "reps" to ex.reps
                    )
                }
                val dayData = mapOf(
                    "dayName" to day.day,
                    "workoutName" to day.workoutName,
                    "exercises" to exercisesData
                )
                batch.set(dayRef, dayData)
            }
        }.addOnSuccessListener {
            Log.d("FirebaseRepo", "✅ All user data saved successfully!")
            onResult(true, null)
        }.addOnFailureListener { e ->
            Log.e("FirebaseRepo", "❌ Failed to save user data", e)
            onResult(false, e.message)
        }
    }

    fun saveBodyFatHistory(bodyFatPercent: Double, date: String, onResult: (Boolean, String?) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onResult(false, "User not signed in")
        val historyRef = db.collection("users").document(userId)
            .collection("bodyFatHistory").document()

        val data = mapOf(
            "bodyFatPercent" to bodyFatPercent,
            "date" to date
        )

        historyRef.set(data)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.message) }
    }

    fun saveWeightHistory(weight: Double, date: String, onResult: (Boolean, String?) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onResult(false, "User not signed in")
        val weightRef = db.collection("users").document(userId)
            .collection("weightHistory").document()

        val data = mapOf(
            "weight" to weight,
            "date" to date
        )

        weightRef.set(data)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.message) }
    }

    fun saveWorkoutLog(
        workoutLog: WorkoutLog,
        onResult: (Boolean, String?) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return onResult(false, "User not signed in")
        val logRef = db.collection("users").document(userId)
            .collection("workoutLogs").document()

        val logData = mapOf(
            "date" to workoutLog.date,
            "startTime" to workoutLog.startTime,
            "duration" to workoutLog.duration,
            "notes" to workoutLog.notes,
            "isSynced" to true
        )

        db.runBatch { batch ->
            batch.set(logRef, logData)

            workoutLog.exercises.forEach { ex ->
                val exRef = logRef.collection("exercises").document()
                batch.set(exRef, mapOf(
                    "exerciseId" to ex.exerciseId,
                    "isSynced" to true
                ))

                ex.sets.forEach { set ->
                    val setRef = exRef.collection("sets").document()
                    batch.set(setRef, mapOf(
                        "setNo" to set.setNo,
                        "reps" to set.reps,
                        "weightUsed" to set.weightUsed,
                        "isSynced" to true
                    ))
                }
            }
        }.addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.message) }
    }

    fun saveMealLog(
        date: String,
        time: String,
        notes: String,
        foods: List<FoodPortionValue>,
        onResult: (Boolean, String?) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return onResult(false, "User not signed in")
        val mealRef = db.collection("users").document(userId)
            .collection("mealLogs").document()

        val mealData = mapOf(
            "date" to date,
            "time" to time,
            "notes" to notes
        )

        db.runBatch { batch ->
            batch.set(mealRef, mealData)

            foods.forEach { food ->
                val foodRef = mealRef.collection("foods").document()
                val foodData = mapOf(
                    "food" to food.name,
                    "quantity" to food.amount
                )
                batch.set(foodRef, foodData)
            }
        }.addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.message) }
    }

    // FETCH USER DATA
    fun getUserProfile(onResult: (Boolean, UserProfile?, String?) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onResult(false, null, "User not signed in")
        val profileRef = db.collection("users").document(userId)
            .collection("profile").document("info")

        profileRef.get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val profile = UserProfile(
                        gender = doc.getString("gender") ?: "",
                        age = (doc.getLong("age") ?: 0L).toInt(),
                        height = doc.getDouble("height") ?: 0.0,
                        weight = doc.getDouble("weight") ?: 0.0,
                        bodyFatPercent = doc.getDouble("bodyFatPercent") ?: 0.0,
                        targetBodyFat = doc.getDouble("targetBodyFat") ?: 0.0,
                        targetWeight = doc.getDouble("targetWeight") ?: 0.0,
                        dietPlan = doc.getString("dietPlan") ?: ""
                    )
                    onResult(true, profile, null)
                } else {
                    onResult(false, null, "Profile not found")
                }
            }
            .addOnFailureListener { e -> onResult(false, null, e.message) }
    }

    fun getWorkoutPlan(
        onResult: (Boolean, List<WorkoutPlan>?, String?) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return onResult(false, null, "User not signed in")

        val plansRef = db.collection("users").document(userId).collection("workoutPlans")

        plansRef.get()
            .addOnSuccessListener { plansSnapshot ->
                if (plansSnapshot.isEmpty) {
                    onResult(true, emptyList(), null)
                    return@addOnSuccessListener
                }

                val allDays = mutableListOf<WorkoutPlan>()
                val dayTasks = mutableListOf<Task<QuerySnapshot>>()

                // For each workout plan document
                for (planDoc in plansSnapshot.documents) {
                    val daysRef = planDoc.reference.collection("days")
                    val planName = planDoc.getString("planName") ?: "Default Plan"
                    val createdDate = planDoc.getLong("createdDate") ?: 0L
                    val dayTask = daysRef.get().addOnSuccessListener { daysSnapshot ->
                        for (dayDoc in daysSnapshot.documents) {

                            // Extract exercises list from 'exercises' array in Firestore
                            val exercisesList = (dayDoc["exercises"] as? List<Map<String, Any>>)?.map { ex ->
                                Exercises(
                                    name = ex["name"] as? String ?: "",
                                    sets = (ex["sets"] as? Number)?.toInt() ?: 0,
                                    reps = (ex["reps"] as? Number)?.toInt() ?: 0
                                )
                            } ?: emptyList()

                            // Add to our result list
                            allDays.add(
                                WorkoutPlan(
                                    day = dayDoc.getString("dayName") ?: dayDoc.id,
                                    workoutName = dayDoc.getString("workoutName") ?: "",
                                    exercises = exercisesList,
                                    createdDate = createdDate.toString(),
                                    planName = planName
                                )
                            )
                        }
                    }

                    dayTasks.add(dayTask)
                }

                // Wait until all plan-day reads are complete
                Tasks.whenAllSuccess<QuerySnapshot>(dayTasks)
                    .addOnSuccessListener {
                        onResult(true, allDays, null)
                    }
                    .addOnFailureListener { e ->
                        onResult(false, null, e.message)
                    }
            }
            .addOnFailureListener { e ->
                onResult(false, null, e.message)
            }
    }

    fun getMealLogs(onResult: (Boolean, List<MealLog>?, String?) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onResult(false, null, "User not signed in")
        val mealLogsRef = db.collection("users").document(userId).collection("mealLogs")

        mealLogsRef.get()
            .addOnSuccessListener { mealSnap ->
                if (mealSnap.isEmpty) {
                    return@addOnSuccessListener onResult(true, emptyList(), null)
                }

                val mealLogs = mutableListOf<MealLog>()
                val tasks = mutableListOf<Task<QuerySnapshot>>()

                for (mealDoc in mealSnap.documents) {
                    val foodsRef = mealDoc.reference.collection("foods")
                    tasks.add(foodsRef.get().addOnSuccessListener { foodsSnap ->
                        val foods = foodsSnap.map {
                            FoodPortionValue(
                                name = it.getString("food") ?: "",
                                amount = it.getDouble("quantity") ?: 0.0
                            )
                        }

                        mealLogs.add(
                            MealLog(
                                date = mealDoc.getString("date") ?: "",
                                time = mealDoc.getString("time") ?: "",
                                notes = mealDoc.getString("notes") ?: "",
                                foods = foods
                            )
                        )
                    })
                }

                Tasks.whenAllSuccess<QuerySnapshot>(tasks)
                    .addOnSuccessListener { onResult(true, mealLogs, null) }
                    .addOnFailureListener { e -> onResult(false, null, e.message) }
            }
            .addOnFailureListener { e -> onResult(false, null, e.message) }
    }

    fun getWorkoutLogs(onResult: (Boolean, List<WorkoutLog>?, String?) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onResult(false, null, "User not signed in")
        val logsRef = db.collection("users").document(userId).collection("workoutLogs")

        logsRef.get()
            .addOnSuccessListener { logsSnapshot ->
                if (logsSnapshot.isEmpty) {
                    onResult(true, emptyList(), null)
                    return@addOnSuccessListener
                }

                val workoutLogs = mutableListOf<WorkoutLog>()
                val allTasks = mutableListOf<Task<QuerySnapshot>>()

                for (logDoc in logsSnapshot.documents) {
                    val log = WorkoutLog(
                        date = logDoc.getString("date") ?: "",
                        startTime = logDoc.getLong("startTime") ?: 0L,
                        duration = (logDoc.getLong("duration"))?.toInt() ?: 0,
                        notes = logDoc.getString("notes") ?: "",
                        exercises = mutableListOf()
                    )

                    val exRef = logDoc.reference.collection("exercises")
                    val exTask = exRef.get().addOnSuccessListener { exSnapshot ->
                        val exercises = mutableListOf<WorkoutExercise>()
                        val setTasks = mutableListOf<Task<QuerySnapshot>>()

                        for (exDoc in exSnapshot.documents) {
                            val exerciseId = (exDoc.getLong("exerciseId") ?: 0L)
                            val setsRef = exDoc.reference.collection("sets")

                            val setTask = setsRef.get().addOnSuccessListener { setsSnapshot ->
                                val sets = setsSnapshot.map { setDoc ->
                                    WorkoutSet(
                                        setNo = setDoc.getString("setNo") ?: "",
                                        reps = (setDoc.getLong("reps") ?: 0L).toInt(),
                                        weightUsed = (setDoc.getDouble("weightUsed") ?: 0.0)
                                    )
                                }

                                exercises.add(
                                    WorkoutExercise(
                                        exerciseId = exerciseId,
                                        sets = sets
                                    )
                                )
                            }

                            setTasks.add(setTask)
                        }

                        Tasks.whenAllSuccess<QuerySnapshot>(setTasks)
                            .addOnSuccessListener {
                                (log.exercises as MutableList).addAll(exercises)
                            }
                    }

                    allTasks.add(exTask)
                    workoutLogs.add(log)
                }

                Tasks.whenAllSuccess<QuerySnapshot>(allTasks)
                    .addOnSuccessListener { onResult(true, workoutLogs, null) }
                    .addOnFailureListener { e -> onResult(false, null, e.message) }
            }
            .addOnFailureListener { e -> onResult(false, null, e.message) }
    }

    fun getNutrientRequirement(onResult: (Boolean, Nutrients?, String?) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onResult(false, null, "User not signed in")
        val nutrientRef = db.collection("users").document(userId)
            .collection("nutrientRequirement").document("current")

        nutrientRef.get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    onResult(false, null, "No nutrient data found")
                    return@addOnSuccessListener
                }

                try {
                    val nutrients = Nutrients(
                        calories = doc.getDouble("Calories")?.toFloat() ?: 0f,
                        protein = doc.getDouble("Protein")?.toFloat() ?: 0f,
                        carbohydrates = doc.getDouble("Carbohydrates")?.toFloat() ?: 0f,
                        fat = doc.getDouble("Fat")?.toFloat() ?: 0f,
                        iron = doc.getDouble("Iron")?.toFloat() ?: 0f,
                        calcium = doc.getDouble("Calcium")?.toFloat() ?: 0f,
                        potassium = doc.getDouble("Potassium")?.toFloat() ?: 0f,
                        magnesium = doc.getDouble("Magnesium")?.toFloat() ?: 0f,
                        zinc = doc.getDouble("Zinc")?.toFloat() ?: 0f,
                        sodium = doc.getDouble("Sodium")?.toFloat() ?: 0f,
                        vitaminD = doc.getDouble("VitaminD")?.toFloat() ?: 0f,
                        vitaminA = doc.getDouble("VitaminA")?.toFloat() ?: 0f,
                        vitaminC = doc.getDouble("VitaminC")?.toFloat() ?: 0f,
                        vitaminK = doc.getDouble("VitaminK")?.toFloat() ?: 0f,
                        vitaminB12 = doc.getDouble("VitaminB12")?.toFloat() ?: 0f
                    )

                    onResult(true, nutrients, null)

                } catch (e: Exception) {
                    onResult(false, null, "Error parsing nutrient data")
                }
            }
            .addOnFailureListener { e ->
                onResult(false, null, e.message)
            }
    }

    fun getBodyFatHistory(onResult: (Boolean, List<BodyFatEntry>?, String?) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onResult(false, null, "User not signed in")
        val historyRef = db.collection("users").document(userId)
            .collection("bodyFatHistory")

        historyRef.get()
            .addOnSuccessListener { snapshot ->
                val entries = snapshot.map {
                    BodyFatEntry(
                        bodyFatPercent = it.getDouble("bodyFatPercent") ?: 0.0,
                        date = it.getString("date") ?: ""
                    )
                }.sortedBy { it.date } // optional sorting
                onResult(true, entries, null)
            }
            .addOnFailureListener { e -> onResult(false, null, e.message) }
    }

    fun getWeightHistory(onResult: (Boolean, List<WeightEntry>?, String?) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onResult(false, null, "User not signed in")
        val weightRef = db.collection("users").document(userId)
            .collection("weightHistory")

        weightRef.get()
            .addOnSuccessListener { snapshot ->
                val entries = snapshot.map {
                    WeightEntry(
                        weight = it.getDouble("weight") ?: 0.0,
                        date = it.getString("date") ?: ""
                    )
                }.sortedBy { it.date }
                onResult(true, entries, null)
            }
            .addOnFailureListener { e -> onResult(false, null, e.message) }
    }
}
