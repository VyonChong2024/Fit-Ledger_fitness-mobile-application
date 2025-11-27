package com.example.fyp_fitledger.ui.activity

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Toast

//Firebase import
import com.google.firebase.auth.FirebaseAuth

//Google Credential import
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.launch
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.graphics.drawable.toDrawable
import com.example.fyp_fitledger.R
import com.example.fyp_fitledger.data.viewmodel.UserViewModel
import com.example.fyp_fitledger.utils.helper.AuthManager
import com.example.fyp_fitledger.data.local.DatabaseHelper
import com.example.fyp_fitledger.data.local.dao.FoodDao
import com.example.fyp_fitledger.data.local.dao.FoodDaoImpl
import com.example.fyp_fitledger.data.local.dao.MealDao
import com.example.fyp_fitledger.data.local.dao.MealDaoImpl
import com.example.fyp_fitledger.data.local.dao.UserDao
import com.example.fyp_fitledger.data.local.dao.UserDaoImpl
import com.example.fyp_fitledger.data.local.dao.WorkoutDao
import com.example.fyp_fitledger.data.local.dao.WorkoutDaoImpl
import com.example.fyp_fitledger.data.model.MealLogFoods
import com.example.fyp_fitledger.data.model.MealLogs
import com.example.fyp_fitledger.data.model.User
import com.example.fyp_fitledger.data.repo.FirebaseRepository
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {

    private lateinit var videoView: VideoView
    private lateinit var userViewModel: UserViewModel

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var userDao: UserDao
    private lateinit var foodDao: FoodDao
    private lateinit var workoutDao: WorkoutDao
    private lateinit var mealDao: MealDao
    private lateinit var fireRepo: FirebaseRepository


    private var progressDialog: AlertDialog? = null // Loading Dialog
    private var doubleBackToExitPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main) // Load the XML layout

        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)

        dbHelper = DatabaseHelper(this)
        userDao = UserDaoImpl(dbHelper)
        foodDao = FoodDaoImpl(dbHelper)
        workoutDao = WorkoutDaoImpl(dbHelper)
        mealDao = MealDaoImpl(dbHelper)

        fireRepo = FirebaseRepository()

        // Instantiate VideoView
        videoView = findViewById(R.id.VideoView)
        val uri = ("android.resource://" + packageName + "/" + R.raw.login_video).toUri()
        videoView.setVideoURI(uri)
        videoView.setOnPreparedListener { mp ->
            mp.isLooping = true
            mp.setVolume(0f, 0f)
            videoView.start()
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            // Check if the setup is complete for the user
            if (userDao.isUserExist(uid)) {
                Log.d("MainActivity", "User setup had completed")
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
                return
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            foodDao.importFoodDataFromJson(applicationContext, "foundationDownload.json")
        }

        val signInButton = findViewById<Button>(R.id.signInButton)
        signInButton.setOnClickListener {
            showLoadingDialog(this)
            AuthManager.signInWithGoogle(this) { success, uid ->
                dismissLoadingDialog()
                if (success && uid != null) {
                    handleUserAfterSignIn(uid)
                } else {
                    Toast.makeText(this, "Sign-in failed. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }

        doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Click again to exit", Toast.LENGTH_SHORT).show()

        Handler(Looper.getMainLooper()).postDelayed({
            doubleBackToExitPressedOnce = false
        }, 2000) // 2 seconds delay
    }

    private fun showLoadingDialog(context: Context) {
        val builder = AlertDialog.Builder(context)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_loading, null)

        builder.setView(dialogView)
        builder.setCancelable(false) // Prevent manual dismissal

        progressDialog = builder.create()

        // Show the dialog first to get the window reference
        progressDialog?.show()

        // Force the dialog size to match content
        progressDialog?.window?.setLayout(200, 200) // Adjust size as needed
        progressDialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable()) // Removes default margins
    }

    private fun handleUserAfterSignIn(uid: String) {
        val localUser = userDao.getUserById(uid)
        Log.d("=====MainActivity", "localuser: $localUser")
        Log.d("=====MainActivity", "uid: $uid")
        if (localUser != null) {
            // User exists locally, go to home
            Log.d("=====MainActivity", "user exists locally")
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        } else {
            // Local user not found → fetch from Firestore
            Log.d("=====MainActivity", "user not exists locally")
            showLoadingDialog(this)
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(uid)
            Log.d("=====MainActivity", "firestore userRef: $userRef")

            Log.d("FIREBASE_DEBUG", "FirebaseApp name: ${FirebaseApp.getInstance().name}")
            Log.d("FIREBASE_DEBUG", "FirebaseApp projectId: ${FirebaseApp.getInstance().options.projectId}")


            userRef.get(Source.SERVER)
                .addOnSuccessListener { doc ->
                    Log.d("FIREBASE_DEBUG", "Document from server: $doc")
                }
                .addOnFailureListener { e ->
                    Log.e("FIREBASE_DEBUG", "Error reading document", e)
                }

            db.collection("users").document(uid).set(mapOf("test" to true), SetOptions.merge())
            db.collection("users")
                .get(Source.SERVER)
                .addOnSuccessListener { snapshot ->
                    Log.d("FIREBASE_DEBUG", "Found ${snapshot.size()} users")
                    for (doc in snapshot.documents) {
                        Log.d("FIREBASE_DEBUG", "Doc: ${doc.id} => ${doc.data}")
                    }
                }

            db.collection("users")
                .get()
                .addOnSuccessListener { snapshot ->
                    Log.d("DEBUG_USERS", "Found ${snapshot.size()} users")
                    for (doc in snapshot.documents) {
                        Log.d("DEBUG_USERS", "Doc: ${doc.id} => ${doc.data}")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("DEBUG_USERS", "Error fetching users", e)
                }

            userRef.get()
                .addOnSuccessListener { document ->
                    Log.d("=====MainActivity", "document: $document")
                    if (document.exists()) {
                        Log.d("=====MainActivity", "document exist")
                        // User exists in Firestore → import data locally
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                importUserFromFirestore(uid)
                                withContext(Dispatchers.Main) {
                                    dismissLoadingDialog()
                                    startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                                    finish()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    dismissLoadingDialog()
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Failed to import user data: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    redirectToDemographic(uid)
                                }
                            }
                        }
                    } else {
                        Log.d("=====MainActivity", "document not exist")
                        dismissLoadingDialog()
                        // User doesn't exist in Firestore → redirect to demographic
                        redirectToDemographic(uid)
                    }
                }
                .addOnFailureListener { e ->
                    Log.d("=====MainActivity", "error occurs on check user in firebase ${e.message}")
                    dismissLoadingDialog()
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to check user in Firebase: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    redirectToDemographic(uid)
                }
        }
    }

    private fun redirectToDemographic(uid: String) {
        userViewModel.updateUserID(uid)
        val intent = Intent(this, DemographicActivity::class.java)
        intent.putExtra("USER_ID", uid)
        startActivity(intent)
        finish()
    }

    /** Imports user data from Firestore into local SQLite */
    private suspend fun importUserFromFirestore(uid: String) {
        var userImportSuccess = -1L

        // 1️⃣ IMPORT USER PROFILE
        suspendCoroutine { cont ->
            fireRepo.getUserProfile { success, profile, error ->
                if (success && profile != null) {
                    val user = User(
                        userId = uid,
                        gender = profile.gender,
                        age = profile.age,
                        height = profile.height,
                        weight = profile.weight,
                        bodyFatPercent = profile.bodyFatPercent,
                        targetBodyFat = profile.targetBodyFat,
                        targetWeight = profile.targetWeight,
                        dietPlan = profile.dietPlan,
                        isSynced = 1
                    )
                    userImportSuccess = userDao.insertUser(user)
                    cont.resume(Unit)
                } else cont.resumeWithException(Exception(error))
            }
        }

        if (userImportSuccess == -1L) {
            Toast.makeText(this@MainActivity,"Failed to import from firebase",Toast.LENGTH_LONG).show()
            throw Exception("Failed to import user profile")
        }

        // 2️⃣ IMPORT WORKOUT PLAN
        suspendCoroutine { cont ->
            fireRepo.getWorkoutPlan { success, daysList, error ->
                if (success && daysList != null) {
                    for (day in daysList) {
                        val planId = workoutDao.insertWorkoutPlan(uid, day.planName, day.createdDate)
                        val planDayId = workoutDao.insertWorkoutPlanDay(planId, day.day, day.workoutName)

                        day.exercises.forEach { ex ->
                            workoutDao.insertWorkoutPlanExercise(
                                planDayId,
                                ex.name,
                                ex.sets,
                                ex.reps
                            )
                        }
                    }
                    cont.resume(Unit)
                } else cont.resumeWithException(Exception(error))
            }
        }

        // 3️⃣ IMPORT MEAL LOGS
        suspendCoroutine { cont ->
            fireRepo.getMealLogs { success, mealLogs, error ->
                if (success && mealLogs != null) {
                    mealLogs.forEach { ml ->
                        val logId = mealDao.insertMealLog(
                            MealLogs(
                                0,
                                uid,
                                ml.date,
                                ml.time,
                                ml.notes,
                                1
                            )
                        )
                        ml.foods.forEach { fp ->
                            mealDao.insertMealLogFood(
                                MealLogFoods(
                                    0,
                                    logId,
                                    fp.name,
                                    fp.amount
                                )
                            )
                        }
                    }
                    cont.resume(Unit)
                } else cont.resumeWithException(Exception(error))
            }
        }

        // 4️⃣ IMPORT WORKOUT LOGS
        suspendCoroutine { cont ->
            fireRepo.getWorkoutLogs { success, workoutLogs, error ->
                if (success && workoutLogs != null) {
                    for (log in workoutLogs) {
                        val logId = workoutDao.insertWorkoutLog(
                            uid, log.date, log.startTime, log.duration, log.notes
                        )

                        log.exercises.forEach { ex ->
                            val exId = workoutDao.insertWorkoutExercise(logId, ex.exerciseId)
                            ex.sets.forEach { s ->
                                workoutDao.insertExerciseSet(
                                    exId,
                                    s.setNo,
                                    s.reps,
                                    s.weightUsed
                                )
                            }
                        }
                    }
                    cont.resume(Unit)
                } else cont.resumeWithException(Exception(error))
            }
        }

        // 5️⃣ IMPORT NUTRIENT REQUIREMENT
        suspendCoroutine { cont ->
            fireRepo.getNutrientRequirement { success, req, error ->
                if (success && req != null) {
                    mealDao.insertNutrientPlan(uid, req)
                    cont.resume(Unit)
                } else cont.resumeWithException(Exception(error))
            }
        }

        // 6️⃣ IMPORT BODY FAT HISTORY
        suspendCoroutine { cont ->
            fireRepo.getBodyFatHistory { success, history, error ->
                if (success && history != null) {
                    history.forEach {
                        userDao.updateBodyFatPercent(uid, it.bodyFatPercent, it.date)
                    }
                    cont.resume(Unit)
                } else cont.resumeWithException(Exception(error))
            }
        }

        // 7️⃣ IMPORT WEIGHT HISTORY
        suspendCoroutine { cont ->
            fireRepo.getWeightHistory { success, history, error ->
                if (success && history != null) {
                    history.forEach {
                        userDao.updateWeight(uid, it.weight, it.date)
                    }
                    cont.resume(Unit)
                } else cont.resumeWithException(Exception(error))
            }
        }
    }

    private fun dismissLoadingDialog() {
        progressDialog?.dismiss()
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }
}