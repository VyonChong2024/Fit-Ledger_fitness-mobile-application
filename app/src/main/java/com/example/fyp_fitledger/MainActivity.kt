package com.example.fyp_fitledger

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.Toast

// Gemini Import

//Firebase import
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

// Chatgpt extra import

//Google Credential import
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModelProvider
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.Dispatchers
import java.io.IOException

class MainActivity : ComponentActivity() {

    private lateinit var videoView: VideoView

    private lateinit var userViewModel: UserViewModel

    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

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
        //userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)    //problem on storing the user id

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
                startActivity(Intent(this, HomeActivity::class.java))       //setup had completed, goto homepage
                finish()
                return
            }
        }


        val signInButton = findViewById<Button>(R.id.signInButton)

        lifecycleScope.launch(Dispatchers.IO) {
            dbHelper.importFoodDataFromJson(applicationContext, "foundationDownload.json")
            Log.d("MainActivity", "Food data imported successfully")
        }

        // Handle button click (Delegate sign-in logic)
        signInButton.setOnClickListener {
            showLoadingDialog(this)
            AuthManager.signInWithGoogle(this) { success, uid ->
                dismissLoadingDialog()
                if (success && uid != null) {
                    // Redirect to home or dashboard
                    val user = dbHelper.getUserByUid(uid)
                    if (user == null) {
                        userViewModel.updateUserID(uid)
                        val intent = Intent(this, DemographicActivity::class.java)
                        intent.putExtra("USER_ID", uid)
                        startActivity(intent)
                        finish()
                    } else {
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    }
                } else {
                    // Show error message
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
        progressDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // Removes default margins
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