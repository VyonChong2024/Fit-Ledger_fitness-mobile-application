package com.example.fyp_fitledger

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide

class ExerciseDetailActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var userViewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_detail)

        // Initialize DB and ViewModel
        dbHelper = DatabaseHelper(this)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]

        val exerciseName = intent.getStringExtra("exercise_name")
        if (exerciseName == null) {
            Toast.makeText(this, "Exercise not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Fetch from DB
        val exerciseRepository = ExerciseRepository(dbHelper)
        val exercise: ExerciseDetailData = exerciseRepository.getExerciseByName(exerciseName)

        // Populate UI
        findViewById<TextView>(R.id.tvWorkoutName).text = exercise.name
        findViewById<TextView>(R.id.tvCategory).text = exercise.category
        findViewById<TextView>(R.id.tvMuscleTrained).text = exercise.muscles
        findViewById<TextView>(R.id.tvEquipment).text = exercise.equipment


        val formattedInstruction = exercise.instruction.replace("\\n", "\n")
        findViewById<TextView>(R.id.tvInstruction).text = formattedInstruction


        val gifResourceId = resources.getIdentifier(exercise.gifUrl, "drawable", packageName)
        val imageView = findViewById<ImageView>(R.id.ivExerciseGIF)
        if (gifResourceId != 0) {
            Glide.with(this)
                .asGif()
                .load(gifResourceId)
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.default_image_background)
        }

        val btnAddExercise = findViewById<Button>(R.id.btnAddExercise)
        btnAddExercise.setOnClickListener {
            val resultIntent = Intent().apply {
                putExtra("exerciseName", exercise.name)
            }
            val logIntent = Intent("com.example.fyp_fitledger.ADD_EXERCISE_TO_LOG")
            logIntent.putExtra("EXERCISE_NAME", exerciseName)
            LocalBroadcastManager.getInstance(this).sendBroadcast(logIntent)

            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        // Handle back button
        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }
}
