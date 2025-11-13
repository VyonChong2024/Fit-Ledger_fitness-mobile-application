package com.example.fyp_fitledger.ui.activity

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import android.net.Uri
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
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import androidx.core.graphics.drawable.toDrawable
import com.example.fyp_fitledger.R
import com.example.fyp_fitledger.data.viewmodel.UserViewModel
import com.example.fyp_fitledger.utils.helper.AuthManager
import com.example.fyp_fitledger.utils.helper.DatabaseHelper

class MainActivity : ComponentActivity() {

    private lateinit var videoView: VideoView
    private lateinit var userViewModel: UserViewModel

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var database: SQLiteDatabase

    private var progressDialog: AlertDialog? = null // Loading Dialog
    private var doubleBackToExitPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main) // Load the XML layout

        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)

        dbHelper = DatabaseHelper(this)
        database = dbHelper.writableDatabase

        // Instantiate VideoView
        videoView = findViewById(R.id.VideoView)
        val uri = Uri.parse("android.resource://" + packageName + "/" + R.raw.login_video)
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
            if (isSetupComplete(uid)) {
                Log.d("MainActivity", "User setup had completed")
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
                return
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            dbHelper.importFoodDataFromJson(applicationContext, "foundationDownload.json")
            Log.d("MainActivity", "Food data imported successfully")
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

    private fun isSetupComplete(uid: String): Boolean {
        return getSharedPreferences("AppPrefs", MODE_PRIVATE)
            .getBoolean("setupComplete_$uid", false)
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
        val localUser = dbHelper.getUserByUid(uid)
        if (localUser != null) {
            // User exists locally, go to home
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        } else {
            // Local user not found → fetch from Firestore
            showLoadingDialog(this)
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(uid)

            userRef.get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // User exists in Firestore → import data locally
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                importUserFromFirestore(uid, document)
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
                                    // Optionally, redirect to demographic form
                                    redirectToDemographic(uid)
                                }
                            }
                        }
                    } else {
                        dismissLoadingDialog()
                        // User doesn't exist in Firestore → redirect to demographic
                        redirectToDemographic(uid)
                    }
                }
                .addOnFailureListener { e ->
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
    private suspend fun importUserFromFirestore(uid: String, document: DocumentSnapshot) {
        // Example: import profile
        val profileDoc = document.reference.collection("profile").document("info").get().await()
        val profileData = profileDoc.data ?: emptyMap<String, Any>()

        val gender = profileData["gender"] as? String ?: ""
        val age = (profileData["age"] as? Long)?.toInt() ?: 0
        val height = (profileData["height"] as? Double) ?: 0.0
        val weight = (profileData["weight"] as? Double) ?: 0.0
        val bodyFat = (profileData["bodyFatPercent"] as? Double) ?: 0.0

        // TODO: Similarly, import workoutPlan and nutrientPlan if needed
    }

    private fun dismissLoadingDialog() {
        progressDialog?.dismiss()
    }

    override fun onDestroy() {
        database.close()
        dbHelper.close()
        super.onDestroy()
    }
}