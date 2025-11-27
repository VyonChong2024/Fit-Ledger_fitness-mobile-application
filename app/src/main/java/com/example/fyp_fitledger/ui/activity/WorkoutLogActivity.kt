package com.example.fyp_fitledger.ui.activity

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.fyp_fitledger.R
import com.example.fyp_fitledger.data.model.WorkoutExercise
import com.example.fyp_fitledger.data.model.WorkoutLog
import com.example.fyp_fitledger.data.model.WorkoutSet
import com.example.fyp_fitledger.data.repo.FirebaseRepository
import com.example.fyp_fitledger.data.local.DatabaseHelper
import com.example.fyp_fitledger.data.local.dao.ExerciseDao
import com.example.fyp_fitledger.data.local.dao.ExerciseDaoImpl
import com.example.fyp_fitledger.data.local.dao.WorkoutDao
import com.example.fyp_fitledger.data.local.dao.WorkoutDaoImpl
import com.example.fyp_fitledger.data.model.SetEntry
import com.example.fyp_fitledger.data.viewmodel.WorkoutLogViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import androidx.core.content.edit
import android.view.inputmethod.InputMethodManager

class WorkoutLogActivity : AppCompatActivity() {

    // DAOs and helpers
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var workoutDao: WorkoutDao

    private lateinit var userID: String
    private lateinit var btnFinish: Button
    private lateinit var container: LinearLayout
    private lateinit var inflater: LayoutInflater
    private lateinit var tvAddExercise: TextView
    private lateinit var tvCancelWorkout: TextView
    private lateinit var tvTimer: TextView

    // ViewModel holds transient UI state
    private lateinit var viewModel: WorkoutLogViewModel

    // UI state helpers
    private val allCheckBoxes = mutableListOf<CheckBox>()

    // Timer
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable

    // State to avoid triggering listeners while we bind the UI
    private var isBinding = false

    // Storage of DB generated ids for marking synced later
    private var workoutLogId: Long? = null
    private val workoutExerciseIds = mutableListOf<Long>()
    private val workoutExerciseSetIds = mutableListOf<Long>()

    // Activity result launcher to open ExerciseListActivity and get selected exercises
    private val addExerciseLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val names = data?.getStringArrayListExtra("SELECTED_EXERCISES")
                names?.let {
                    // snapshot existing UI so entries persist
                    snapshotCurrentUIState()
                    // add selected exercises (ViewModel avoids duplicates)
                    it.forEach { name -> viewModel.addExercise(name) }
                    viewModel.saveToPrefs(this)
                    renderUI()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_log)

        // init
        dbHelper = DatabaseHelper(this)
        exerciseDao = ExerciseDaoImpl(dbHelper)
        workoutDao = WorkoutDaoImpl(dbHelper)

        viewModel = ViewModelProvider(this)[WorkoutLogViewModel::class.java]

        inflater = LayoutInflater.from(this)
        container = findViewById(R.id.exerciseLogContainer)
        btnFinish = findViewById(R.id.btnFinish)
        tvAddExercise = findViewById(R.id.tvAddExercise)
        tvCancelWorkout = findViewById(R.id.tvCancelWorkout)
        tvTimer = findViewById(R.id.tvTimer)

        btnFinish.isEnabled = false

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) userID = currentUser.uid

        // Load persisted UI state (if any) BEFORE adding plan defaults
        viewModel.loadFromPrefs(this)

        // If no saved exercises, load today's plan (intent extra)
        if (viewModel.addedExercises.isEmpty()) {
            val initialExercises = intent.getStringArrayListExtra("TODAY_EXERCISES")
            initialExercises?.forEach { viewModel.addExercise(it) }
            viewModel.saveToPrefs(this)
        }

        val prefs = getSharedPreferences("WorkoutPrefs", MODE_PRIVATE)
        var savedTime = prefs.getLong("start_time_millis", 0L)

        if (savedTime != 0L) {
            viewModel.startTimeMillis = savedTime
        } else {
            savedTime = System.currentTimeMillis()
            prefs.edit().putLong("start_time_millis", savedTime).apply()
        }
        viewModel.startTimeMillis = savedTime

        setupTimer()
        setupListeners()
        // Render UI from ViewModel
        renderUI()
        markWorkoutRunning()
    }

    private fun setupTimer() {
        handler.removeCallbacksAndMessages(null)

        timerRunnable = object : Runnable {
            override fun run() {
                val now = System.currentTimeMillis()
                val elapsedMillis = now - viewModel.startTimeMillis
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
    }


    private fun setupListeners() {
        tvAddExercise.setOnClickListener {
            // persist current UI state before launching
            snapshotCurrentUIState()
            val intent = Intent(this, ExerciseListActivity::class.java)
            addExerciseLauncher.launch(intent)
        }

        tvCancelWorkout.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_cancel_workout, null)
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create()
            val noButton = dialogView.findViewById<Button>(R.id.noButton)
            val yesButton = dialogView.findViewById<Button>(R.id.yesButton)
            noButton.setOnClickListener { dialog.dismiss() }
            yesButton.setOnClickListener {
                // Fully stop timer and clear persisted start time and temporary UI state
                handler.removeCallbacks(timerRunnable)
                val prefs = getSharedPreferences("WorkoutPrefs", MODE_PRIVATE)
                prefs.edit().remove("start_time_millis").apply()

                viewModel.clearExercises()
                viewModel.clearPrefs(this)
                viewModel.startTimeMillis = 0L

                clearWorkoutRunning()
                dialog.dismiss()
                Toast.makeText(this, "Cancel workout", Toast.LENGTH_SHORT).show()
                WorkoutActivity.hasOpenedWorkoutLog = false
                finish()
            }
            dialog.show()
        }

        btnFinish.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_finish_workout, null)
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create()

            val noButton = dialogView.findViewById<Button>(R.id.noButton)
            val yesButton = dialogView.findViewById<Button>(R.id.yesButton)
            noButton.setOnClickListener { dialog.dismiss() }
            yesButton.setOnClickListener {
                dialog.dismiss()
                lifecycleScope.launch {
                    val workoutLog = saveWorkoutLog()
                    // stop timer and clear
                    handler.removeCallbacks(timerRunnable)
                    val prefs = getSharedPreferences("WorkoutPrefs", MODE_PRIVATE)
                    prefs.edit().remove("start_time_millis").apply()

                    Toast.makeText(this@WorkoutLogActivity, "Workout is saved", Toast.LENGTH_SHORT).show()
                    saveToFirebase(workoutLog)

                    // clear temp prefs because saved successfully locally
                    viewModel.clearPrefs(this@WorkoutLogActivity)
                    viewModel.clearExercises()

                    clearWorkoutRunning()
                    // go back
                    val intent = Intent(this@WorkoutLogActivity, WorkoutActivity::class.java)
                    intent.putExtra("HIGHLIGHT_UPDATE", true)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    WorkoutActivity.hasOpenedWorkoutLog = false
                    startActivity(intent)
                    finish()
                }
            }
            dialog.show()
        }
    }

    /**
     * Renders entire UI from viewModel.addedExercises
     * Clears container and rebuilds views. Keeps allCheckBoxes updated.
     */
    private fun renderUI() {
        // prevent listeners from saving during bind
        isBinding = true

        container.removeAllViews()
        allCheckBoxes.clear()

        // Get today's plan info (can be used to get default sets/reps)
        val today = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
        val planId = workoutDao.getLatestPlanIdForUser(userID)
        val planDayId = workoutDao.getPlanDayId(planId, today)

        for (exerciseName in viewModel.addedExercises) {
            val exerciseView = inflater.inflate(R.layout.item_exercise, container, false)
            val exerciseSetContainer = exerciseView.findViewById<LinearLayout>(R.id.setContainer)
            val nameTextView = exerciseView.findViewById<TextView>(R.id.tvExerciseName)
            nameTextView.text = exerciseName

            // find default sets/reps
            val (sets, reps) = workoutDao.getSetsAndRepsForExercise(planDayId, exerciseName)
            val savedSets = viewModel.exerciseEntries[exerciseName]?.size ?: 0
            val planSets = sets ?: 1
            // show whichever is larger so saved rows are not lost
            val numberOfSets = max(savedSets, planSets)

            // ensure ViewModel structure fits current plan
            viewModel.ensureSetsForExercise(exerciseName, numberOfSets)
            val existingEntries = viewModel.exerciseEntries[exerciseName] ?: mutableListOf()

            // Build set views
            for (setIndex in 1..numberOfSets) {
                val setView = inflater.inflate(R.layout.item_exercise_set, exerciseSetContainer, false)
                val entry = existingEntries.getOrNull(setIndex - 1)
                bindSetView(setView, exerciseName, setIndex, reps, entry, exerciseSetContainer)

                val cb = setView.findViewById<CheckBox>(R.id.cbCompleted)
                val etReps = setView.findViewById<EditText>(R.id.etReps)
                val etWeight = setView.findViewById<EditText>(R.id.etWeight)
                if (cb.isChecked) {
                    etReps.isEnabled = false
                    etWeight.isEnabled = false
                } else {
                    etReps.isEnabled = true
                    etWeight.isEnabled = true
                }

                exerciseSetContainer.addView(setView)
            }

            // Add "Add Set" click inside each exercise container
            val tvAddSet = exerciseView.findViewById<TextView>(R.id.tvAddSet)
            tvAddSet.setOnClickListener {
                val newSetIndex = exerciseSetContainer.childCount + 1
                // ensure viewModel data structure updated first
                viewModel.ensureSetsForExercise(exerciseName, newSetIndex)
                val newSetView = inflater.inflate(R.layout.item_exercise_set, exerciseSetContainer, false)
                // newly added set has no default reps -> null
                bindSetView(newSetView, exerciseName, newSetIndex, null, viewModel.exerciseEntries[exerciseName]?.getOrNull(newSetIndex - 1), exerciseSetContainer)
                exerciseSetContainer.addView(newSetView)
                // After adding set, persist and update finish button
                viewModel.saveToPrefs(this)
                updateFinishButtonState()
            }

            container.addView(exerciseView)
        }

        updateFinishButtonState()
        // done binding
        isBinding = false
    }

    /**
     * Binds a single set item view with logic (text watchers, checkbox handling, next focus)
     */
    private fun bindSetView(
        setView: View,
        exerciseName: String,
        setNumber: Int,
        defaultReps: Int?,
        entry: SetEntry? = null,
        exerciseContainer: LinearLayout
    ) {
        val setNumberText = setView.findViewById<TextView>(R.id.setNumber)
        val previousRecordView = setView.findViewById<TextView>(R.id.tvPreviousRecord)
        val repsInput = setView.findViewById<EditText>(R.id.etReps)
        val weightInput = setView.findViewById<EditText>(R.id.etWeight)
        val checkBox = setView.findViewById<CheckBox>(R.id.cbCompleted)

        setNumberText.text = setNumber.toString()

        // === 1) GET PREVIOUS RECORD ============================================================
        val previousRecord = workoutDao.getPreviousRecord(userID, exerciseName, setNumber)
        previousRecordView.text = previousRecord ?: "-"

        val (prevWeightHint, prevRepsHint) = parsePreviousRecord(previousRecord)

        // === 2) DETERMINE FINAL HINTS (priority) ==============================================
        val finalRepsHint = when {
            !prevRepsHint.isNullOrBlank() -> prevRepsHint
            defaultReps != null -> defaultReps.toString()
            else -> ""
        }

        val finalWeightHint = prevWeightHint ?: ""

        // dynamic hints (updated when un-ticked)
        var dynamicRepsHint = finalRepsHint
        var dynamicWeightHint = finalWeightHint

        // === 3) APPLY ENTRY VALUES IF ANY ======================================================
        isBinding = true
        try {
            if (entry != null) {
                entry.reps?.let { repsInput.setText(it) }
                entry.weight?.let { weightInput.setText(it) }
                checkBox.isChecked = entry.checked
            } else {
                repsInput.setText("")
                weightInput.setText("")
                checkBox.isChecked = false
            }
        } finally {
            isBinding = false
        }

        // apply initial hints only if no typed value exists
        if (repsInput.text.isBlank()) repsInput.hint = dynamicRepsHint
        if (weightInput.text.isBlank()) weightInput.hint = dynamicWeightHint

        // === 4) ENABLE / DISABLE CHECKBOX ======================================================
        fun updateCheckboxEnabledState() {
            val repsAvailable = repsInput.text.isNotBlank() || dynamicRepsHint.isNotBlank()
            val weightAvailable = weightInput.text.isNotBlank() || dynamicWeightHint.isNotBlank()
            checkBox.isEnabled = repsAvailable && weightAvailable
        }

        updateCheckboxEnabledState()

        // === 5) SAVE ENTRY =====================================================================
        val setIndex = setNumber - 1
        fun saveEntry() {
            if (isBinding) return

            val repsVal =
                if (repsInput.text.isNotBlank()) repsInput.text.toString()
                else dynamicRepsHint.takeIf { it.isNotBlank() }

            val weightVal =
                if (weightInput.text.isNotBlank()) weightInput.text.toString()
                else dynamicWeightHint.takeIf { it.isNotBlank() }

            viewModel.updateSetEntry(exerciseName, setIndex, repsVal, weightVal, checkBox.isChecked)
            viewModel.saveToPrefs(this)
            updateFinishButtonState()
        }

        // === 6) TEXT WATCHERS ==================================================================
        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (isBinding) return
                updateCheckboxEnabledState()
                saveEntry()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        repsInput.addTextChangedListener(textWatcher)
        weightInput.addTextChangedListener(textWatcher)

        // === 7) CHECKBOX TOGGLE LOGIC ==========================================================
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isBinding) return@setOnCheckedChangeListener

            if (isChecked) {
                // convert hint → actual
                if (repsInput.text.isBlank() && dynamicRepsHint.isNotBlank())
                    repsInput.setText(dynamicRepsHint)

                if (weightInput.text.isBlank() && dynamicWeightHint.isNotBlank())
                    weightInput.setText(dynamicWeightHint)

                repsInput.isEnabled = false
                weightInput.isEnabled = false

            } else {
                // UNCHECK — text becomes new hint
                val repsPrevVal = repsInput.text.toString()
                val weightPrevVal = weightInput.text.toString()

                // clear fields
                repsInput.setText("")
                weightInput.setText("")

                // update dynamic hints
                dynamicRepsHint = when {
                    prevRepsHint != null -> prevRepsHint          // highest priority
                    repsPrevVal.isNotBlank() -> repsPrevVal
                    defaultReps != null -> defaultReps.toString()
                    else -> ""
                }

                dynamicWeightHint = when {
                    prevWeightHint != null -> prevWeightHint       // highest priority
                    weightPrevVal.isNotBlank() -> weightPrevVal
                    else -> ""
                }

                repsInput.hint = dynamicRepsHint
                weightInput.hint = dynamicWeightHint

                repsInput.isEnabled = true
                weightInput.isEnabled = true
            }

            saveEntry()
            updateCheckboxEnabledState()
        }

        // === 8) INPUT NAVIGATION ===============================================================
        weightInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT ||
                event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                inputCursorControl(weightInput, exerciseContainer)
                true
            } else false
        }

        repsInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT ||
                event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                inputCursorControl(repsInput, exerciseContainer)
                true
            } else false
        }
    }


    private fun parsePreviousRecord(record: String?): Pair<String?, String?> {
        if (record.isNullOrBlank()) return null to null

        // Expected format: "15.0kg * 15"
        val parts = record.split("*")
        if (parts.size != 2) return null to null

        val weightPart = parts[0].trim().removeSuffix("kg").trim()
        val repsPart = parts[1].trim()

        return weightPart to repsPart
    }

    private fun updateFinishButtonState() {
        // rebuild check reliably (some checkboxes may be stale across renders)
        btnFinish.isEnabled = allCheckBoxes.any { it.isChecked }
    }

    /**
     * Save workout log into local DB. Runs in background coroutine and returns model.
     */
    private suspend fun saveWorkoutLog(): WorkoutLog = withContext(Dispatchers.IO) {
        val durationMillis = System.currentTimeMillis() - viewModel.startTimeMillis
        val durationMinutes = (durationMillis / 60000).toInt()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        val logId = workoutDao.insertWorkoutLog(userID, currentDate, viewModel.startTimeMillis, durationMinutes)
        workoutLogId = logId

        val exercisesList = mutableListOf<WorkoutExercise>()

        // container child iteration must match renderUI ordering
        for (i in 0 until container.childCount) {
            val exerciseView = container.getChildAt(i)
            val exerciseName = exerciseView.findViewById<TextView>(R.id.tvExerciseName).text.toString()
            val exerciseId = exerciseDao.getExerciseIdByName(exerciseName) ?: continue

            val setContainer = exerciseView.findViewById<LinearLayout>(R.id.setContainer)
            val setsList = mutableListOf<WorkoutSet>()

            val workoutExerciseId = workoutDao.insertWorkoutExercise(logId, exerciseId)
            workoutExerciseIds.add(workoutExerciseId.toLong())

            var anySetSaved = false

            for (j in 0 until setContainer.childCount) {
                val setView = setContainer.getChildAt(j)
                val cb = setView.findViewById<CheckBox>(R.id.cbCompleted)
                val repsInput = setView.findViewById<EditText>(R.id.etReps)
                val weightInput = setView.findViewById<EditText>(R.id.etWeight)

                if (cb?.isChecked == true) {
                    val reps = repsInput.text.toString().toIntOrNull() ?: continue
                    val weight = weightInput.text.toString().toDoubleOrNull() ?: continue

                    val setNumber = (j + 1).toString()
                    val workoutExerciseSetId = workoutDao.insertExerciseSet(workoutExerciseId, setNumber, reps, weight)
                    workoutExerciseSetIds.add(workoutExerciseSetId.toLong())

                    setsList.add(WorkoutSet(setNumber, reps, weight))
                    anySetSaved = true
                }
            }

            if (!anySetSaved) {
                // delete empty workoutExercise record
                workoutDao.deleteWorkoutExercise(workoutExerciseId)
            } else {
                exercisesList.add(WorkoutExercise(exerciseId, setsList))
            }
        }

        return@withContext WorkoutLog(
            date = currentDate,
            startTime = viewModel.startTimeMillis,
            duration = durationMinutes,
            notes = "",
            exercises = exercisesList
        )
    }

    private fun saveToFirebase(workoutList: WorkoutLog) {
        val firebaseRepo = FirebaseRepository()
        firebaseRepo.saveWorkoutLog(workoutList) { success, error ->
            lifecycleScope.launch(Dispatchers.IO) {
                if (success) {
                    workoutLogId?.let { workoutDao.markWorkoutLogSynced(it) }
                    workoutExerciseIds.forEach { workoutDao.markWorkoutExerciseSynced(it) }
                    workoutExerciseSetIds.forEach { workoutDao.markExerciseSetSynced(it) }
                } else {
                    Log.d("====Firebase====", "failed to store data to firebase: $error")
                }
            }
        }
    }

    /**
     * Capture current UI input values and persist to viewModel + prefs (used before launching child activity)
     */
    private fun snapshotCurrentUIState() {
        viewModel.exerciseEntries.clear()
        for (i in 0 until container.childCount) {
            val exerciseView = container.getChildAt(i)
            val exerciseName = exerciseView.findViewById<TextView>(R.id.tvExerciseName).text.toString()

            val setContainer = exerciseView.findViewById<LinearLayout>(R.id.setContainer)
            val entries = mutableListOf<SetEntry>()

            for (j in 0 until setContainer.childCount) {
                val setView = setContainer.getChildAt(j)

                // 1. Get references to the views
                val etReps = setView.findViewById<EditText>(R.id.etReps)
                val etWeight = setView.findViewById<EditText>(R.id.etWeight)
                val cb = setView.findViewById<CheckBox>(R.id.cbCompleted)

                val isChecked = cb?.isChecked == true

                // 2. FIX: Logic to decide whether to save Text, Hint, or Null
                // IF text is typed -> Save Text
                // ELSE IF checkbox is Checked -> Save Hint (User accepts recommendation)
                // ELSE -> Save Null (So it remains a hint when you return)

                val reps = when {
                    !etReps.text.isNullOrBlank() -> etReps.text.toString()
                    isChecked -> etReps.hint?.toString()?.takeIf { it.isNotBlank() }
                    else -> null
                }

                val weight = when {
                    !etWeight.text.isNullOrBlank() -> etWeight.text.toString()
                    isChecked -> etWeight.hint?.toString()?.takeIf { it.isNotBlank() }
                    else -> null
                }

                entries.add(SetEntry(reps, weight, isChecked))
            }

            viewModel.exerciseEntries[exerciseName] = entries
        }

        // persist
        viewModel.saveToPrefs(this)
    }


    private fun markWorkoutRunning() {
        getSharedPreferences("WorkoutSession", MODE_PRIVATE)
            .edit {
                putBoolean("isRunning", true)
            }
    }

    private fun clearWorkoutRunning() {
        getSharedPreferences("WorkoutSession", MODE_PRIVATE)
            .edit {
                putBoolean("isRunning", false)
            }
    }


    private fun inputCursorControl(current: EditText, exerciseContainer: LinearLayout) {
        // Collect all EditTexts (ordered)
        val fields = mutableListOf<EditText>()

        for (i in 0 until exerciseContainer.childCount) {
            val setView = exerciseContainer.getChildAt(i)

            val weight = setView.findViewById<EditText>(R.id.etWeight)
            val reps = setView.findViewById<EditText>(R.id.etReps)

            fields.add(weight)
            fields.add(reps)
        }

        // Find current index
        val index = fields.indexOf(current)

        if (index == -1) return

        // If last field → close keyboard
        if (index == fields.lastIndex) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(current.windowToken, 0)
            current.clearFocus()
            return
        }

        // Else → go to next field
        val nextField = fields[index + 1]
        nextField.requestFocus()
    }


    override fun onPause() {
        super.onPause()
        // persist current UI state so values survive Activity switching
        snapshotCurrentUIState()
    }

    override fun onResume() {
        super.onResume()
        // reload from prefs in case something changed while backgrounded
        viewModel.loadFromPrefs(this)

        // Safely reload start time only from WorkoutPrefs
        val timerPrefs = getSharedPreferences("WorkoutPrefs", MODE_PRIVATE)
        val savedStart = timerPrefs.getLong("start_time_millis", 0L)

        if (savedStart > 0) {
            viewModel.startTimeMillis = savedStart
        }

        renderUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
        // Only persist start time if it's non-zero (we clear on cancel/finish intentionally)
        if (viewModel.startTimeMillis != 0L) {
            val prefs = getSharedPreferences("WorkoutPrefs", MODE_PRIVATE)
            prefs.edit().putLong("start_time_millis", viewModel.startTimeMillis).apply()
        }
    }
}
