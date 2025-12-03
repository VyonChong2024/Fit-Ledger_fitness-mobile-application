package com.example.fyp_fitledger.ui.activity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.fyp_fitledger.R
import com.example.fyp_fitledger.data.viewmodel.UserViewModel
import com.example.fyp_fitledger.ui.component.NavBarControl
import com.example.fyp_fitledger.data.local.DatabaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.collections.iterator
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import com.example.fyp_fitledger.data.local.dao.WorkoutDao
import com.example.fyp_fitledger.data.local.dao.WorkoutDaoImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class WorkoutActivity : AppCompatActivity() {

    private lateinit var ivSearch: ImageView
    private lateinit var cvStartWorkout: CardView
    private lateinit var cvStartWorkoutText: TextView
    private lateinit var muscleImageView: ImageView
    private lateinit var userId: String
    private lateinit var userViewModel: UserViewModel

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var workoutDao: WorkoutDao

    private val views = listOf("front", "side", "back")
    // Track current index to allow immediate refresh on click
    private var currentViewIndex = 0

    // NEW: Track selection state
    private var selectedView: View? = null
    private var selectedMuscles: Set<String> = emptySet()
    private val addedExerciseNames = mutableListOf<String>()
    private val muscleColorMap = mapOf(
        "#00008d".toColorInt() to "Upper Chest",
        "#0000ff".toColorInt() to "Middle Chest",
        "#add8e6".toColorInt() to "Lower Chest",
        "#ff0000".toColorInt() to "Bicep",
        "#8b0000".toColorInt() to "Tricep",
        "#F08080".toColorInt() to "Forearm",
        "#00ffff".toColorInt() to "Side Delts",
        "#008b8b".toColorInt() to "Front Delts",
        "#e0ffff".toColorInt() to "Rear Delts",
        "#008d00".toColorInt() to "Rectus Abdominis",  //abs
        "#00ff00".toColorInt() to "Oblique",
        "#ffff00".toColorInt() to "Quadriceps",
        "#808000".toColorInt() to "Hamstrings",
        "#fffacd".toColorInt() to "Calves",
        "#ff00ff".toColorInt() to "Trapezius",  //upper back
        "#8b008b".toColorInt() to "Latissimus Dorsi",  //lats
        "#FF69B4".toColorInt() to "Erector Spinae",  //lower back
        "#000000".toColorInt() to "Glutes"
    )
    val broadMuscleMappings = mapOf(
        // Delts
        "deltoids" to listOf("Front Delts", "Side Delts", "Rear Delts"),
        "anterior deltoid" to listOf("Front Delts"),
        "lateral deltoid" to listOf("Side Delts"),
        "posterior deltoid" to listOf("Rear Delts"),
        "rear deltoids" to listOf("Rear Delts"),

        // Arms
        "biceps" to listOf("Bicep"),
        "biceps brachii" to listOf("Bicep"),
        "triceps" to listOf("Tricep"),
        "triceps brachii" to listOf("Tricep"),
        "brachioradialis" to listOf("Forearm"),
        "extensor carpi" to listOf("Forearm"),
        "flexor carpi" to listOf("Forearm"),
        "forearms" to listOf("Forearm"),

        // Chest
        "pectoralis major" to listOf("Upper Chest", "Middle Chest", "Lower Chest"),

        // Back
        "latissimus dorsi" to listOf("Latissimus Dorsi"),
        "trapezius" to listOf("Trapezius"),
        "erector spinae" to listOf("Erector Spinae"),

        // Abs
        "rectus abdominis" to listOf("Rectus Abdominis"),
        "transverse abdominis" to listOf("Rectus Abdominis"),  // Closest match
        "obliques" to listOf("Oblique"),
        "core" to listOf("Rectus Abdominis", "Oblique"),

        // Glutes / Hip
        "glutes" to listOf("Glutes"),
        "gluteus maximus" to listOf("Glutes"),
        "gluteus medius" to listOf("Glutes"),
        "gluteus minimus" to listOf("Glutes"),
        "tensor fasciae latae" to listOf("Glutes"), // Closest mapping
        "hip flexors" to listOf("Glutes"),  // Generalized

        // Adductors
        "adductors" to listOf("Hamstrings"), // No specific label, best fit
        "adductor longus" to listOf("Hamstrings"),
        "adductor brevis" to listOf("Hamstrings"),
        "adductor magnus" to listOf("Hamstrings"),

        // Legs
        "quadriceps" to listOf("Quadriceps"),
        "hamstrings" to listOf("Hamstrings"),
        "legs" to listOf("Quadriceps", "Hamstrings", "Calves"),
        "gastrocnemius" to listOf("Calves"),
        "soleus" to listOf("Calves")
    )
    private val trainedMusclesByDay: HashMap<String, Int> = hashMapOf()

    private val bitmapCache = mutableMapOf<String, Pair<Bitmap, Bitmap>>()
    private val fastColorLookup = HashMap<Int, String>()
    private lateinit var exerciseResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        NavBarControl.setupBottomNavigation(this, bottomNav)

        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        muscleImageView = findViewById(R.id.muscleImageView)

        dbHelper = DatabaseHelper(this)
        workoutDao = WorkoutDaoImpl(dbHelper)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null)
            userId = currentUser.uid

        ivSearch = findViewById(R.id.ivSearch)
        cvStartWorkout = findViewById(R.id.cvStartWorkout)
        cvStartWorkoutText = findViewById(R.id.cvStartWorkoutText)

        ivSearch.setOnClickListener{
            val intent = Intent(this, ExerciseListActivity::class.java)
            exerciseResultLauncher.launch(intent)
        }

        cvStartWorkout.setOnClickListener {
            val intent = Intent(this, WorkoutLogActivity::class.java)
            intent.putExtra("TODAY_EXERCISES", ArrayList(addedExerciseNames))
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)

            updateStartWorkoutButton()  // update UI after opening
        }

        exerciseResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val exerciseName = data?.getStringExtra("exerciseName")
                if (exerciseName != null) {
                    addExerciseToWorkout(exerciseName)
                }
            }
        }

        // 1. Initialize Fast Lookup Map
        muscleColorMap.forEach { (color, name) -> fastColorLookup[color] = name }
        retrieveWorkoutData(userId)
        loadTodayWorkoutExercises(userId)

        // 2. Start the image cycle using Lifecycle Scope
        startMuscleImageCycle()
        updateStartWorkoutButton()
    }

    private fun retrieveWorkoutData(userId: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        val today = sdf.format(calendar.time)
        calendar.add(Calendar.DATE, -1)
        val yesterday = sdf.format(calendar.time)
        calendar.add(Calendar.DATE, -1)
        val twoDaysAgo = sdf.format(calendar.time)

        val dateMap = mapOf(today to 0, yesterday to 1, twoDaysAgo to 2)

        for ((date, dayOffset) in dateMap) {
            // Retrieve raw muscles from DAO
            val data = workoutDao.getMusclesTrainedByDate(userId, date)

            for (rawMuscle in data.muscles) {
                val normalized = rawMuscle.lowercase(Locale.getDefault())
                val muscleParts = normalized.split(",").map { it.trim() }

                for (musclePart in muscleParts) {
                    val mappedList = broadMuscleMappings[musclePart]
                        ?: listOf(musclePart.replaceFirstChar { it.uppercaseChar() })

                    for (mapped in mappedList) {
                        val current = trainedMusclesByDay[mapped]
                        trainedMusclesByDay[mapped] = minOf(current ?: 3, dayOffset)
                    }
                }
            }
        }
    }

    private fun startMuscleImageCycle() {
        // lifecycleScope is bound to the Activity lifecycle.
        // When Activity is destroyed, this loop STOPS automatically.
        lifecycleScope.launch(Dispatchers.Default) {
            var index = 0
            while (isActive) { // Check if activity is still alive
                val viewType = views[index % views.size]

                // 3. Process the image (Background Thread)
                val processedBitmap = processMuscleImage(viewType)

                // 4. Update UI (Main Thread)
                withContext(Dispatchers.Main) {
                    if (processedBitmap != null) {
                        muscleImageView.setImageBitmap(processedBitmap)
                    }
                }

                // 5. Wait 2 seconds
                delay(2000)
                index++
            }
        }
    }

    private fun processMuscleImage(viewType: String): Bitmap? {
        val gender = userViewModel.gender ?: "male"
        val cacheKey = "${gender}_$viewType"

        // A. Load Bitmaps (Use Cache if available to save CPU/Memory)
        var bitmaps = bitmapCache[cacheKey]
        if (bitmaps == null) {
            val imageName = "muscle_${gender}_${viewType}"
            val maskName = "muscle_${gender}_${viewType}_color"

            val imageResId = resources.getIdentifier(imageName, "drawable", packageName)
            val maskResId = resources.getIdentifier(maskName, "drawable", packageName)

            if (imageResId == 0 || maskResId == 0) return null

            val original = BitmapFactory.decodeResource(resources, imageResId)
            val mask = BitmapFactory.decodeResource(resources, maskResId)

            // Check for size mismatch
            if (original.width != mask.width || original.height != mask.height) {
                // Resize mask to match original if needed, or return null
                return null
            }

            bitmaps = Pair(original, mask)
            bitmapCache[cacheKey] = bitmaps
        }

        val originalBitmap = bitmaps.first
        val maskBitmap = bitmaps.second

        // B. Create Mutable Copy
        val width = originalBitmap.width
        val height = originalBitmap.height
        val resultBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)

        val pixels = IntArray(width * height)
        val maskPixels = IntArray(width * height)

        resultBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        maskBitmap.getPixels(maskPixels, 0, width, 0, 0, width, height)

        var matchingPixelCount = 0
        var isTargetSelected = false
        // C. Fast Pixel Processing
        for (i in pixels.indices) {
            val maskColor = maskPixels[i]

            // Skip transparent mask pixels immediately
            if (maskColor == 0 || Color.alpha(maskColor) == 0) continue

            // 1. O(1) Lookup instead of looping through the map
            // Note: If your mask PNG has compression artifacts (colors aren't exact),
            // you might need the 'closestMatch' logic. If it's a clean PNG, this works.
            var muscleName = fastColorLookup[maskColor]

            // Fallback: If exact match fails, try fuzzy match (only if necessary)
            if (muscleName == null) {
                muscleName = findClosestMuscleColor(maskColor)
            }

            if (muscleName != null) {
                isTargetSelected = true
                val daysAgo = trainedMusclesByDay[muscleName]
                if (daysAgo != null) {
                    // Apply Tint
                    val alpha = when (daysAgo) {
                        0 -> 120 // Today (Darker/Stronger)
                        1 -> 60 // Yesterday
                        2 -> 30  // 2 Days ago
                        else -> 0
                    }

                    if (alpha > 0) {
                        pixels[i] = blendColors(pixels[i], Color.RED, alpha)
                    }
                }
            }
            if (selectedMuscles.contains(muscleName)) {
                // Apply GREEN tint (High Priority)
                pixels[i] = blendColors(pixels[i], Color.GREEN, 120)

                // Only increment count if it matches the TARGET
                matchingPixelCount++
                isTargetSelected = true
            }
        }
        Log.d("PAINT_DEBUG", "View: $viewType | Target Muscles: $selectedMuscles | Found Pixels: $matchingPixelCount | Target Selected: $isTargetSelected")
        resultBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return resultBitmap
    }

    // Fast blend function using bitwise math (No Color objects)
    private fun blendColors(baseColor: Int, overlayColor: Int, alpha: Int): Int {
        val r1 = (baseColor shr 16) and 0xFF
        val g1 = (baseColor shr 8) and 0xFF
        val b1 = baseColor and 0xFF

        val r2 = (overlayColor shr 16) and 0xFF
        val g2 = (overlayColor shr 8) and 0xFF
        val b2 = overlayColor and 0xFF

        // Simple alpha blending
        val r = (r1 * (255 - alpha) + r2 * alpha) / 255
        val g = (g1 * (255 - alpha) + g2 * alpha) / 255
        val b = (b1 * (255 - alpha) + b2 * alpha) / 255

        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    // Optimized Fuzzy Match (No Sqrt, No Pow)
    private fun findClosestMuscleColor(targetColor: Int): String? {
        var minDistance = Int.MAX_VALUE
        var closestMuscle: String? = null
        val thresholdSquared = 35 * 35 // Squared threshold

        val r1 = (targetColor shr 16) and 0xFF
        val g1 = (targetColor shr 8) and 0xFF
        val b1 = targetColor and 0xFF

        for ((color, name) in fastColorLookup) {
            val r2 = (color shr 16) and 0xFF
            val g2 = (color shr 8) and 0xFF
            val b2 = color and 0xFF

            // Use Squared Distance (Much faster than Sqrt)
            val dist = (r1 - r2) * (r1 - r2) + (g1 - g2) * (g1 - g2) + (b1 - b2) * (b1 - b2)

            if (dist < minDistance && dist < thresholdSquared) {
                minDistance = dist
                closestMuscle = name
            }
        }
        return closestMuscle
    }

    private fun loadTodayWorkoutExercises(userId: String) {
        val container = findViewById<LinearLayout>(R.id.workoutExerciseContainer)
        addedExerciseNames.clear()
        container.removeAllViews()

        val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
        val currentDay = sdf.format(Calendar.getInstance().time)

        // DAO retrieves list of names
        val exerciseList = workoutDao.getTodayExercisePlanName(userId, currentDay)

        for (exerciseName in exerciseList) {
            addedExerciseNames.add(exerciseName)

            val tv = createExerciseView(exerciseName)
            container.addView(tv)
        }
        updateContainerBias()
    }

    private fun updateContainerBias() {
        val workoutExerciseContainer = findViewById<LinearLayout>(R.id.workoutExerciseContainer)

        // After adding items to the container
        val itemCount = workoutExerciseContainer.childCount

        val baseBias = 0.2f
        val maxBias = 0.5f
        val bias = (baseBias + (itemCount * 0.02f)).coerceAtMost(maxBias)

        // Update constraint bias programmatically
        val layoutParams = workoutExerciseContainer.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.verticalBias = bias
        workoutExerciseContainer.layoutParams = layoutParams
    }

    private fun addExerciseToWorkout(exerciseName: String) {
        if (addedExerciseNames.contains(exerciseName)) {
            Toast.makeText(this, "Exercise already exist!", Toast.LENGTH_SHORT).show()
            return
        }
        addedExerciseNames.add(exerciseName)

        val container = findViewById<LinearLayout>(R.id.workoutExerciseContainer)

        val textView = TextView(this).apply {
            text = exerciseName
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.white))
            setPadding(20, 12, 40, 12)
            background = ContextCompat.getDrawable(context, R.drawable.round_corner_background)
            maxWidth = 520
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 40)
            }
        }

        Toast.makeText(this, "Exercise added successfully", Toast.LENGTH_SHORT).show()

        container.addView(textView)
        updateContainerBias()
    }

    private fun updateStartWorkoutButton() {
        val running = getSharedPreferences("WorkoutSession", MODE_PRIVATE)
            .getBoolean("isRunning", false)

        if (running) {
            cvStartWorkout.findViewById<CardView>(R.id.cvStartWorkout)?.apply {
                setBackgroundColor(ContextCompat.getColor(context, R.color.grayish_lime_green))
            }
            cvStartWorkoutText.apply {
                text = getString(R.string.frag_btn_cont)
            }
        } else {
            cvStartWorkout.findViewById<CardView>(R.id.cvStartWorkout)?.apply {
                setBackgroundColor(ContextCompat.getColor(context, R.color.japanese_laurel))
            }
            cvStartWorkoutText.apply {
                text = "Start Workout"
            }
        }
    }

    // --- NEW LOGIC: Helper to create the Exercise TextView ---
    private fun createExerciseView(exerciseName: String): TextView {
        return TextView(this).apply {
            text = exerciseName
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.white))
            setPadding(20, 12, 40, 12)

            // Default State
            background = ContextCompat.getDrawable(context, R.drawable.round_corner_background)
            typeface = Typeface.DEFAULT

            maxWidth = 520
            tag = exerciseName

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 40) }

            setOnClickListener { view ->
                onExerciseClicked(view, exerciseName)
            }
        }
    }

    // --- NEW LOGIC: Handle Click Event ---
    private fun onExerciseClicked(view: View, exerciseName: String) {
        val textView = view as TextView

        // 1. Reset previous selection UI
        selectedView?.let { prev ->
            val prevTv = prev as TextView
            // Restore normal font
            prevTv.typeface = Typeface.DEFAULT
            // Restore original background drawable
            prevTv.background = ContextCompat.getDrawable(this, R.drawable.round_corner_background)
            // Restore white text if you changed it
            prevTv.setTextColor(ContextCompat.getColor(this, R.color.white))
        }

        // 2. If clicking the same item, deselect it
        if (selectedView == view) {
            selectedView = null
            selectedMuscles = emptySet()
        } else {
            // 3. Select new item UI
            selectedView = view

            // SET BOLD
            textView.typeface = Typeface.DEFAULT_BOLD

            // SET SELECTED BACKGROUND (Programmatically tinting green)
            val drawable = ContextCompat.getDrawable(this, R.drawable.round_corner_background)?.mutate()
            // Using a distinct color for selection (e.g., Green or a Lighter Gray)
            drawable?.setTint("#4CAF50".toColorInt())
            textView.background = drawable

            // 4. Update Muscle Selection Data
            updateSelectedMusclesFromDB(exerciseName)
        }

        // 5. Force refresh
        forceRefreshImage()
    }

    private fun updateSelectedMusclesFromDB(exerciseName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val newSelection = mutableSetOf<String>()

            // Get string from DB
            val muscleString = workoutDao.getMuscleGroupForExercise(exerciseName)

            Log.d("MUSCLE_DEBUG", "Exercise: $exerciseName, DB Returned: $muscleString")

            if (muscleString != null) {
                // Split by comma, TRIM spaces, and force LOWERCASE for mapping matching
                val muscleParts = muscleString.split(",").map { it.trim().lowercase() }

                for (part in muscleParts) {
                    Log.d("MUSCLE_DEBUG", "Processing part: '$part'")

                    // robust mapping lookup
                    val specificMuscles = broadMuscleMappings[part]
                        ?: broadMuscleMappings[part.replace(" ", "")] // Handle "front delts" vs "frontdelts"
                        ?: broadMuscleMappings[part.removeSuffix("s")] // Handle "shoulders" vs "shoulder"
                        // Fallback: If map fails, Capitalize it (e.g. "bicep" -> "Bicep")
                        ?: listOf(part.split(" ").joinToString(" ") {
                            it.replaceFirstChar { char -> char.uppercase() }
                        })

                    Log.d("MUSCLE_DEBUG", "Mapped '$part' to: $specificMuscles")
                    newSelection.addAll(specificMuscles)
                }
            } else {
                Log.e("MUSCLE_DEBUG", "No muscle group found in DB for $exerciseName")
            }

            selectedMuscles = newSelection

            withContext(Dispatchers.Main) {
                forceRefreshImage()
            }
        }
    }

    private fun forceRefreshImage() {
        lifecycleScope.launch(Dispatchers.Default) {
            val viewType = views[currentViewIndex % views.size]
            val processedBitmap = processMuscleImage(viewType)
            withContext(Dispatchers.Main) {
                if (processedBitmap != null) {
                    muscleImageView.setImageBitmap(processedBitmap)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStartWorkoutButton()
    }
    private var doubleBackToExitPressedOnce = false

    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }

        doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Click again to exit", Toast.LENGTH_SHORT).show()

        Handler(Looper.getMainLooper()).postDelayed({
            doubleBackToExitPressedOnce = false
        }, 5000)
    }
}