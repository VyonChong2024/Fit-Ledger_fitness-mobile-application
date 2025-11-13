package com.example.fyp_fitledger.data.repo

import android.util.Log
import com.example.fyp_fitledger.data.model.WorkoutPlanDays
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

data class WorkoutExercise(
    val name: String = "",
    val sets: Int = 0,
    val reps: Int = 0
)

data class WorkoutPlanDays(
    val day: String = "",
    val workoutName: String = "",
    val exercises: List<WorkoutExercise> = emptyList()
)

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

        val workoutData = workoutPlan.map { day ->
            mapOf(
                "day" to day.day,
                "workoutName" to day.workoutName,
                "exercises" to day.exercises.map { ex ->
                    mapOf(
                        "name" to ex.name,
                        "sets" to ex.sets,
                        "reps" to ex.reps
                    )
                }
            )
        }

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

        // 🔄 Save data as subcollections atomically
        db.runBatch { batch ->
            batch.set(userRef.collection("profile").document("info"), profileData)
            batch.set(userRef.collection("workoutPlan").document("days"), mapOf("plan" to workoutData))
            batch.set(userRef.collection("nutrientPlan").document("values"), nutrientData)
        }.addOnSuccessListener {
            Log.d("FirebaseRepo", "✅ All user data saved successfully!")
            onResult(true, null)
        }.addOnFailureListener { e ->
            Log.e("FirebaseRepo", "❌ Failed to save user data", e)
            onResult(false, e.message)
        }
    }

    // -----------------------------
    // 🔹 FETCH USER DATA
    // -----------------------------
    fun getUserData(
        userId: String,
        onResult: (Boolean, Map<String, Any>?) -> Unit
    ) {
        val userRef = db.collection("users").document(userId)

        val userData = mutableMapOf<String, Any>()

        // Fetch profile, workout plan, nutrient plan in parallel
        userRef.collection("profile").document("info").get()
            .addOnSuccessListener { profileDoc ->
                if (profileDoc.exists()) {
                    userData["profile"] = profileDoc.data ?: emptyMap<String, Any>()
                }

                userRef.collection("workoutPlan").document("days").get()
                    .addOnSuccessListener { workoutDoc ->
                        if (workoutDoc.exists()) {
                            userData["workoutPlan"] = workoutDoc.data ?: emptyMap<String, Any>()
                        }

                        userRef.collection("nutrientPlan").document("values").get()
                            .addOnSuccessListener { nutrientDoc ->
                                if (nutrientDoc.exists()) {
                                    userData["nutrientPlan"] = nutrientDoc.data ?: emptyMap<String, Any>()
                                }

                                Log.d("FirebaseRepo", "✅ User data fetched successfully")
                                onResult(true, userData)
                            }
                            .addOnFailureListener { e ->
                                Log.e("FirebaseRepo", "❌ Failed to fetch nutrientPlan", e)
                                onResult(false, null)
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirebaseRepo", "❌ Failed to fetch workoutPlan", e)
                        onResult(false, null)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseRepo", "❌ Failed to fetch profile", e)
                onResult(false, null)
            }
    }
}
