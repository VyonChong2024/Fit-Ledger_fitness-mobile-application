package com.example.fyp_fitledger.ui.activity

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.fyp_fitledger.R
import com.example.fyp_fitledger.data.model.WorkoutExercise
import com.example.fyp_fitledger.data.model.WorkoutLog
import com.example.fyp_fitledger.data.model.WorkoutSet
import com.example.fyp_fitledger.data.repo.FirebaseRepository
import com.example.fyp_fitledger.data.viewmodel.UserViewModel
import com.example.fyp_fitledger.data.local.DatabaseHelper
import com.example.fyp_fitledger.data.local.dao.ExerciseDao
import com.example.fyp_fitledger.data.local.dao.ExerciseDaoImpl
import com.example.fyp_fitledger.data.local.dao.WorkoutDao
import com.example.fyp_fitledger.data.local.dao.WorkoutDaoImpl
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WorkoutLogActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var workoutDao: WorkoutDao
    private lateinit var userViewModel: UserViewModel

    private lateinit var userID: String
    private lateinit var btnFinish: Button
    private val allCheckBoxes = mutableListOf<CheckBox>()
    private lateinit var tvAddExercise: TextView
    private lateinit var tvCancelWorkout: TextView

    private lateinit var tvTimer: TextView
    private var startTimeMillis: Long = 0L
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable

    private lateinit var firebaseRepo: FirebaseRepository

    // save the primary key for the workout log & workout exercise & exercise set
    private var workoutLogId: Long? = null
    private var workoutExerciseIds = mutableListOf<Long>()
    private var workoutExerciseSetIds = mutableListOf<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_log)

        // Initialize DB and ViewModel
        dbHelper = DatabaseHelper(this)
        exerciseDao = ExerciseDaoImpl(dbHelper)
        workoutDao = WorkoutDaoImpl(dbHelper)

        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]

        firebaseRepo = FirebaseRepository()

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null)
            userID = currentUser.uid

        val exercises: ArrayList<String>? = intent.getStringArrayListExtra("TODAY_EXERCISES")
        val container = findViewById<LinearLayout>(R.id.exerciseLogContainer)
        val inflater = LayoutInflater.from(this)
        btnFinish = findViewById(R.id.btnFinish)
        btnFinish.isEnabled = false // initially disabled

        // Get today's workout plan info
        val today = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
        val planId = workoutDao.getLatestPlanIdForUser(userID)
        val planDayId = workoutDao.getPlanDayId(planId, today)

        tvAddExercise = findViewById(R.id.tvAddExercise)
        tvAddExercise.setOnClickListener{
            val intent = Intent(this, ExerciseListActivity::class.java)
            startActivity(intent)
        }

        tvCancelWorkout = findViewById(R.id.tvCancelWorkout)
        tvCancelWorkout.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_cancel_workout, null)
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create()

            val noButton = dialogView.findViewById<Button>(R.id.noButton)
            val yesButton = dialogView.findViewById<Button>(R.id.yesButton)

            noButton.setOnClickListener {
                dialog.dismiss()
            }

            yesButton.setOnClickListener {
                // Stop timer
                handler.removeCallbacks(timerRunnable)

                // Clear stored timer state
                val prefs = getSharedPreferences("WorkoutPrefs", MODE_PRIVATE)
                prefs.edit().remove("start_time_millis").apply()

                dialog.dismiss()
                Toast.makeText(this, "Cancel workout", Toast.LENGTH_SHORT).show()

                // Go back to WorkoutActivity
                val intent = Intent(this, WorkoutActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)
                finish()
            }

            dialog.show()
        }

        //Timer function
        tvTimer = findViewById(R.id.tvTimer)

        // Retrieve persisted start time or initialize if first launch
        val prefs = getSharedPreferences("WorkoutPrefs", MODE_PRIVATE)
        startTimeMillis = prefs.getLong("start_time_millis", 0L)

        if (startTimeMillis == 0L) {
            startTimeMillis = System.currentTimeMillis()
            prefs.edit().putLong("start_time_millis", startTimeMillis).apply()
        }

        timerRunnable = object : Runnable {
            override fun run() {
                val elapsedMillis = System.currentTimeMillis() - startTimeMillis
                val seconds = (elapsedMillis / 1000).toInt()
                val formatted = if (seconds >= 3600)
                    String.format("%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60)
                else
                    String.format("%d:%02d", seconds / 60, seconds % 60)

                tvTimer.text = formatted
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(timerRunnable)


        exercises?.forEach { exerciseName ->
            val exerciseView = inflater.inflate(R.layout.item_exercise, container, false)
            val exerciseSetContainer = exerciseView.findViewById<LinearLayout>(R.id.setContainer)

            // Set exercise name
            val nameTextView = exerciseView.findViewById<TextView>(R.id.tvExerciseName)
            nameTextView.text = exerciseName

            // Get number of sets & default reps from WorkoutPlanExercise
            val (sets, reps) = workoutDao.getSetsAndRepsForExercise(planDayId, exerciseName)
            val numberOfSets = sets ?: 1

            repeat(numberOfSets) { setIndex ->
                val setView = inflater.inflate(R.layout.item_exercise_set, exerciseSetContainer, false)

                val setNumberText = setView.findViewById<TextView>(R.id.setNumber)
                val previousRecord = setView.findViewById<TextView>(R.id.tvPreviousRecord)
                val repsInput = setView.findViewById<EditText>(R.id.etReps)
                val weightInput = setView.findViewById<EditText>(R.id.etWeight)
                val checkBox = setView.findViewById<CheckBox>(R.id.cbCompleted)

                setNumberText.text = "${setIndex + 1}"
                if (reps != null) repsInput.hint = reps.toString()

                val previous = workoutDao.getPreviousRecord(userID, exerciseName, setIndex + 1)
                previousRecord.text = previous ?: "-"

                // Track all checkboxes for finish button logic
                allCheckBoxes.add(checkBox)

                val textWatcher = object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        val weightText = weightInput.text.toString()
                        val repsText = repsInput.text.toString()

                        val isValid = weightText.isNotBlank() && weightText != "0" &&
                                repsText.isNotBlank() && repsText != "0"

                        checkBox.isEnabled = isValid
                    }

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                }

                weightInput.addTextChangedListener(textWatcher)
                repsInput.addTextChangedListener(textWatcher)

                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    weightInput.isEnabled = !isChecked
                    repsInput.isEnabled = !isChecked
                    btnFinish.isEnabled = allCheckBoxes.any { it.isChecked }
                }

                exerciseSetContainer.addView(setView)
            }

            val tvAddSet = exerciseView.findViewById<TextView>(R.id.tvAddSet)
            tvAddSet.setOnClickListener {
                val setIndex = exerciseSetContainer.childCount + 1 // New set number

                val newSetView = inflater.inflate(R.layout.item_exercise_set, exerciseSetContainer, false)

                val setNumberText = newSetView.findViewById<TextView>(R.id.setNumber)
                val previousRecord = newSetView.findViewById<TextView>(R.id.tvPreviousRecord)
                val repsInput = newSetView.findViewById<EditText>(R.id.etReps)
                val weightInput = newSetView.findViewById<EditText>(R.id.etWeight)
                val checkBox = newSetView.findViewById<CheckBox>(R.id.cbCompleted)

                setNumberText.text = setIndex.toString()

                val previous = workoutDao.getPreviousRecord(userID, exerciseName, setIndex)
                previousRecord.text = previous ?: "-"

                // Enable checkbox only if inputs are valid
                val textWatcher = object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        val weightText = weightInput.text.toString()
                        val repsText = repsInput.text.toString()
                        checkBox.isEnabled = weightText.isNotBlank() && weightText != "0" &&
                                repsText.isNotBlank() && repsText != "0"
                    }

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                }

                weightInput.addTextChangedListener(textWatcher)
                repsInput.addTextChangedListener(textWatcher)

                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    repsInput.isEnabled = !isChecked
                    weightInput.isEnabled = !isChecked
                    btnFinish.isEnabled = allCheckBoxes.any { it.isChecked }
                }

                allCheckBoxes.add(checkBox)
                exerciseSetContainer.addView(newSetView) // Add to container (above tvAddSet)
            }

            container.addView(exerciseView)
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val name = intent?.getStringExtra("NAME") ?: return
                if (!::userID.isInitialized) return

                val exerciseNames = arrayListOf(name)
                if (planDayId != null) {
                    renderExercises(exerciseNames, container, inflater, planDayId)
                } else {
                    renderExercises(exerciseNames, container, inflater, -1)
                    Log.e("WorkoutLogActivity", "PlanDayId is null")
                }
            }
        }
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(receiver, IntentFilter("com.example.fyp_fitledger.ADD_EXERCISE_TO_LOG"))


        btnFinish.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_finish_workout, null)
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create()

            val noButton = dialogView.findViewById<Button>(R.id.noButton)
            val yesButton = dialogView.findViewById<Button>(R.id.yesButton)

            noButton.setOnClickListener {
                dialog.dismiss()
            }

            yesButton.setOnClickListener {
                dialog.dismiss()

                val workoutLog = saveWorkoutLog(container)

                // Stop timer
                handler.removeCallbacks(timerRunnable)
                val prefs = getSharedPreferences("WorkoutPrefs", MODE_PRIVATE)
                prefs.edit().remove("start_time_millis").apply()

                Toast.makeText(this, "Workout is saved", Toast.LENGTH_SHORT).show()

                saveToFirebase(workoutLog)

                // Navigate back to WorkoutActivity and re-highlight muscles
                val intent = Intent(this, WorkoutActivity::class.java)
                intent.putExtra("HIGHLIGHT_UPDATE", true)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)
                finish()
            }

            dialog.show()
        }

    }

    private fun renderExercises(exerciseNames: List<String>, container: LinearLayout, inflater: LayoutInflater, planDayId: Int) {
        for (exerciseName in exerciseNames) {
            val exerciseView = inflater.inflate(R.layout.item_exercise, container, false)
            val exerciseSetContainer = exerciseView.findViewById<LinearLayout>(R.id.setContainer)

            val nameTextView = exerciseView.findViewById<TextView>(R.id.tvExerciseName)
            nameTextView.text = exerciseName

            val (sets, reps) = workoutDao.getSetsAndRepsForExercise(planDayId, exerciseName)
            val numberOfSets = sets ?: 1

            repeat(numberOfSets) { setIndex ->
                val setView = inflater.inflate(R.layout.item_exercise_set, exerciseSetContainer, false)

                val setNumberText = setView.findViewById<TextView>(R.id.setNumber)
                val previousRecord = setView.findViewById<TextView>(R.id.tvPreviousRecord)
                val repsInput = setView.findViewById<EditText>(R.id.etReps)
                val weightInput = setView.findViewById<EditText>(R.id.etWeight)
                val checkBox = setView.findViewById<CheckBox>(R.id.cbCompleted)

                setNumberText.text = "${setIndex + 1}"
                if (reps != null) repsInput.hint = reps.toString()

                val previous = workoutDao.getPreviousRecord(userID, exerciseName, setIndex + 1)
                previousRecord.text = previous ?: "-"

                allCheckBoxes.add(checkBox)

                val textWatcher = object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        val weightText = weightInput.text.toString()
                        val repsText = repsInput.text.toString()
                        checkBox.isEnabled = weightText.isNotBlank() && weightText != "0" &&
                                repsText.isNotBlank() && repsText != "0"
                    }

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                }

                weightInput.addTextChangedListener(textWatcher)
                repsInput.addTextChangedListener(textWatcher)

                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    weightInput.isEnabled = !isChecked
                    repsInput.isEnabled = !isChecked
                    btnFinish.isEnabled = allCheckBoxes.any { it.isChecked }
                }

                exerciseSetContainer.addView(setView)
            }

            container.addView(exerciseView)
        }
    }

    private fun saveWorkoutLog(container: LinearLayout): WorkoutLog {
        val durationMillis = System.currentTimeMillis() - startTimeMillis
        val durationMinutes = (durationMillis / 60000).toInt()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        val logId = workoutDao.insertWorkoutLog(userID, currentDate, startTimeMillis, durationMinutes)
        workoutLogId = logId

        val exercisesList = mutableListOf<WorkoutExercise>()    //stored exercise log into workout model

        for (i in 0 until container.childCount) {
            val exerciseView = container.getChildAt(i)
            val exerciseName = exerciseView.findViewById<TextView>(R.id.tvExerciseName).text.toString()
            val exerciseId = exerciseDao.getExerciseIdByName(exerciseName) ?: continue

            val setContainer = exerciseView.findViewById<LinearLayout>(R.id.setContainer)
            val setsList = mutableListOf<WorkoutSet>()

            Log.d("--WorkoutSave", "Processing exercise: $exerciseName")

            val workoutExerciseId = workoutDao.insertWorkoutExercise(logId, exerciseId)
            workoutExerciseIds.add(workoutExerciseId.toLong())
            Log.d("WorkoutSave", "Inserted WorkoutExercise: workoutExerciseId=$workoutExerciseId for exerciseId=$exerciseId")

            var anySetSaved = false

            for (j in 0 until setContainer.childCount) {
                val setView = setContainer.getChildAt(j)
                val cb = setView.findViewById<CheckBox>(R.id.cbCompleted)
                val repsInput = setView.findViewById<EditText>(R.id.etReps)
                val weightInput = setView.findViewById<EditText>(R.id.etWeight)

                if (cb?.isChecked == true) {
                    val reps = repsInput?.text?.toString()?.toIntOrNull() ?: continue
                    val weight = weightInput?.text?.toString()?.toDoubleOrNull() ?: continue

                    val setNumber = (j + 1).toString()
                    Log.d("WorkoutSave", "Saving Set $setNumber for $exerciseName: reps=$reps, weight=$weight")
                    val workoutExerciseSetId = workoutDao.insertExerciseSet(workoutExerciseId, (j + 1).toString(), reps, weight)
                    workoutExerciseSetIds.add(workoutExerciseSetId.toLong())

                    setsList.add(WorkoutSet((j + 1).toString(), reps, weight))
                    anySetSaved = true
                }
            }

            if (!anySetSaved) {
                Log.d("--WorkoutSave", "No sets saved for $exerciseName. Deleted workoutExerciseId=$workoutExerciseId")
                workoutDao.deleteWorkoutExercise(workoutExerciseId)
            }

            if (setsList.isNotEmpty()) {
                exercisesList.add(WorkoutExercise(exerciseId, setsList))
            }
        }

        // parameter need to store to firebase
        val workoutLog = WorkoutLog(
            date = currentDate,
            startTime = startTimeMillis,
            duration = durationMinutes,
            notes = "",     //TODO::add a write note UI
            exercises = exercisesList
        )

        return workoutLog
    }

    private fun saveToFirebase(workoutList: WorkoutLog) {

        firebaseRepo.saveWorkoutLog(workoutList) { success, error ->
            if (success) {
                workoutLogId?.let { workoutDao.markWorkoutLogSynced(it) }
                workoutExerciseIds.forEach { workoutDao.markWorkoutExerciseSynced(it) }
                workoutExerciseSetIds.forEach { workoutDao.markExerciseSetSynced(it) }

                Log.d("WorkoutLogActivity", "Workout log saved to firebase!")
            } else {
                Log.e("WorkoutLogActivity", "Failed to save workout log to firebase: $error")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)

        // Remove the persisted start time so the timer resets next time
        val prefs = getSharedPreferences("WorkoutPrefs", MODE_PRIVATE)
        prefs.edit().remove("start_time_millis").apply()
    }
}
