package com.example.fyp_fitledger

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

class WorkoutActivity : AppCompatActivity() {

    private lateinit var ivSearch: ImageView
    private lateinit var cvStartWorkout: CardView
    private lateinit var cvStartWorkoutText: TextView

    private lateinit var userId: String

    private lateinit var muscleImageView: ImageView
    private lateinit var originalBitmap: Bitmap
    private lateinit var maskBitmap: Bitmap
    private lateinit var overlayBitmap: Bitmap
    private lateinit var canvas: Canvas
    private lateinit var paint: Paint
    private lateinit var userViewModel: UserViewModel

    private val handler = Handler(Looper.getMainLooper())
    private var imageViewIndex = 0
    private val views = listOf("front", "side", "back")

    private val addedExerciseNames = mutableListOf<String>()

    private val muscleColorMap = mapOf(
        Color.parseColor("#00008d") to "Upper Chest",
        Color.parseColor("#0000ff") to "Middle Chest",
        Color.parseColor("#add8e6") to "Lower Chest",
        Color.parseColor("#ff0000") to "Bicep",
        Color.parseColor("#8b0000") to "Tricep",
        Color.parseColor("#F08080") to "Forearm",
        Color.parseColor("#00ffff") to "Side Delts",
        Color.parseColor("#008b8b") to "Front Delts",
        Color.parseColor("#e0ffff") to "Rear Delts",
        Color.parseColor("#008d00") to "Rectus Abdominis",  //abs
        Color.parseColor("#00ff00") to "Oblique",
        Color.parseColor("#ffff00") to "Quadriceps",
        Color.parseColor("#808000") to "Hamstrings",
        Color.parseColor("#fffacd") to "Calves",
        Color.parseColor("#ff00ff") to "Trapezius",  //upper back
        Color.parseColor("#8b008b") to "Latissimus Dorsi",  //lats
        Color.parseColor("#FF69B4") to "Erector Spinae",  //lower back
        Color.parseColor("#000000") to "Glutes"
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

    private lateinit var exerciseResultLauncher: ActivityResultLauncher<Intent>
    private var hasOpenedWorkoutLog = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        NavBarControl.setupBottomNavigation(this, bottomNav)

        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        muscleImageView = findViewById(R.id.muscleImageView)

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

            hasOpenedWorkoutLog = true
            updateStartWorkoutButton()  // update UI after opening
        }

        cycleImage()
        retrieveWorkoutData(userId)
        Log.d("WorkoutActivity", "trainedMusclesByDay: $trainedMusclesByDay")
        Log.d("WorkoutActivity", "retrieve workout data finish")
        loadTodayWorkoutExercises(userId)
        Log.d("WorkoutActivity", "load today workout data finish")

        exerciseResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val exerciseName = data?.getStringExtra("exerciseName")
                if (exerciseName != null) {
                    addExerciseToWorkout(exerciseName)
                }
            }
        }

    }

    private fun retrieveWorkoutData(userId: String) {
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val today = sdf.format(calendar.time)
        calendar.add(Calendar.DATE, -1)
        val yesterday = sdf.format(calendar.time)
        calendar.add(Calendar.DATE, -1)
        val twoDaysAgo = sdf.format(calendar.time)

        val dateMap = mapOf(today to 0, yesterday to 1, twoDaysAgo to 2)

        for ((date, daysAgo) in dateMap) {
            val cursor = db.rawQuery("""
                SELECT e.MuscleGroup 
                FROM WorkoutLog wl
                JOIN WorkoutExercise we ON we.Log_ID = wl.Log_ID
                JOIN Exercise e ON e.Exercise_ID = we.Exercise_ID
                WHERE wl.Date = ? AND wl.User_ID = ?
            """.trimIndent(), arrayOf(date, userId))

            while (cursor.moveToNext()) {
                val rawMuscle = cursor.getString(0)?.lowercase(Locale.getDefault()) ?: continue
                Log.d("WorkoutRetrieval", "Raw muscle from DB: $rawMuscle")

                // Split by comma, trim, and process each muscle individually
                val muscleParts = rawMuscle.split(",").map { it.trim() }

                for (musclePart in muscleParts) {
                    Log.d("WorkoutRetrieval", "Processing muscle part: $musclePart")

                    val mappedMuscles = broadMuscleMappings[musclePart]
                        ?: listOf(musclePart.replaceFirstChar { it.uppercaseChar() })

                    Log.d("WorkoutRetrieval", "Mapped to: $mappedMuscles for date offset: $daysAgo")

                    for (mappedMuscle in mappedMuscles) {
                        val currentVal = trainedMusclesByDay[mappedMuscle]
                        trainedMusclesByDay[mappedMuscle] = minOf(currentVal ?: 3, daysAgo)
                    }
                }
            }

            cursor.close()
        }

        db.close()
    }

    private fun highlightMuscles() {
        if (originalBitmap.width != maskBitmap.width || originalBitmap.height != maskBitmap.height) {
            Log.e("WorkoutActivity", "originalBitmap and maskBitmap size mismatch")
            return
        }

        val combinedBitmap = Bitmap.createBitmap(
            originalBitmap.width, originalBitmap.height, Bitmap.Config.ARGB_8888
        )

        val alphaByDay = mapOf(
            0 to 255,
            1 to 150,
            2 to 80
        )

        // 1. Copy the original bitmap into the combinedBitmap first
        val pixels = IntArray(originalBitmap.width * originalBitmap.height)
        originalBitmap.getPixels(pixels, 0, originalBitmap.width, 0, 0, originalBitmap.width, originalBitmap.height)

        // 2. Read maskBitmap pixels
        val maskPixels = IntArray(maskBitmap.width * maskBitmap.height)
        maskBitmap.getPixels(maskPixels, 0, maskBitmap.width, 0, 0, maskBitmap.width, maskBitmap.height)

        // 3. Loop through the pixels (1D array, faster than 2D)
        for (i in pixels.indices) {
            val pixelColor = maskPixels[i]

            val matchedEntry = muscleColorMap.entries.find { isColorSimilar(it.key, pixelColor) }
            val muscleName = matchedEntry?.value

            if (muscleName != null && trainedMusclesByDay.containsKey(muscleName)) {
                val daysAgo = trainedMusclesByDay[muscleName] ?: continue
                val alpha = alphaByDay[daysAgo] ?: 0

                // 4. Blend red color on top of the original pixel
                val originalColor = pixels[i]

                val red = Color.red(originalColor)
                val green = Color.green(originalColor)
                val blue = Color.blue(originalColor)

                // Blend slightly toward red
                val newRed = (red + 255) / 2
                val newGreen = (green) / 2
                val newBlue = (blue) / 2

                // Create the new color with alpha
                val blendedColor = Color.argb(alpha, newRed, newGreen, newBlue)

                pixels[i] = blendedColor
            }
        }

        // 5. Set all pixels back
        combinedBitmap.setPixels(pixels, 0, combinedBitmap.width, 0, 0, combinedBitmap.width, combinedBitmap.height)

        // 6. Finally draw combined bitmap
        canvas.drawBitmap(combinedBitmap, 0f, 0f, null)
    }

    private fun cycleImage() {
        val gender = userViewModel.gender ?: "male"
        val viewType = views[imageViewIndex % views.size]
        val imageName = "muscle_${gender}_${viewType}"
        val maskName = "muscle_${gender}_${viewType}_color"

        val imageResId = resources.getIdentifier(imageName, "drawable", packageName)
        val maskResId = resources.getIdentifier(maskName, "drawable", packageName)

        if (imageResId == 0 || maskResId == 0) {
            Log.e("WorkoutActivity", "Image resource not found for $imageName or $maskName")
            return
        }

        val tempOriginalBitmap = BitmapFactory.decodeResource(resources, imageResId)
        val tempMaskBitmap = BitmapFactory.decodeResource(resources, maskResId)

        if (tempOriginalBitmap == null || tempMaskBitmap == null) {
            Log.e("WorkoutActivity", "Bitmap decoding failed for $imageName or $maskName")
            return
        }

        originalBitmap = tempOriginalBitmap
        maskBitmap = tempMaskBitmap

        overlayBitmap = Bitmap.createBitmap(originalBitmap.width, originalBitmap.height, Bitmap.Config.ARGB_8888)
        canvas = Canvas(overlayBitmap)

        // Now start background work
        GlobalScope.launch(Dispatchers.Default) {
            highlightMuscles()

            // After highlight done, switch to UI thread
            withContext(Dispatchers.Main) {
                val imageViewHeight = muscleImageView.height
                val aspectRatio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
                val scaledHeight = imageViewHeight
                val scaledWidth = (scaledHeight * aspectRatio).toInt()

                val scaledBitmap = Bitmap.createScaledBitmap(overlayBitmap, scaledWidth, scaledHeight, false)
                muscleImageView.setImageBitmap(scaledBitmap)

                // After everything done, THEN start next cycle
                imageViewIndex++
                Log.d("WorkoutActivity", "Paint finish, start timer 2 seconds")
                handler.postDelayed({ cycleImage() }, 2000)
            }
        }
    }

    private fun loadTodayWorkoutExercises(userId: String) {
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase

        val container = findViewById<LinearLayout>(R.id.workoutExerciseContainer)
        addedExerciseNames.clear()
        container.removeAllViews()

        val sdf = SimpleDateFormat("EEEE", Locale.getDefault()) // returns "Monday", "Tuesday", etc.
        val currentDay = sdf.format(Calendar.getInstance().time)

        val cursor = db.rawQuery("""
            SELECT wpe.ExerciseName 
            FROM WorkoutPlan wp
            JOIN WorkoutPlanDay wpd ON wp.Plan_ID = wpd.Plan_ID
            JOIN WorkoutPlanExercise wpe ON wpd.PlanDay_ID = wpe.PlanDay_ID
            WHERE wpd.DayName = ? AND wp.User_ID = ?
        """.trimIndent(), arrayOf(currentDay, userId))

        if (cursor.moveToFirst()) {
            do {
                val exerciseName = cursor.getString(0)
                addedExerciseNames.add(exerciseName)

                val textView = TextView(this).apply {
                    text = exerciseName
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.white))
                    setPadding(20, 12, 40, 12)
                    background = ContextCompat.getDrawable(context, R.drawable.round_corner_background)
                    maxWidth = 520

                    // Create LayoutParams for the TextView
                    val layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, //width
                        LinearLayout.LayoutParams.WRAP_CONTENT  //height
                    )

                    layoutParams.setMargins(0, 0, 0, 40)

                    // Apply the LayoutParams to the TextView
                    this.layoutParams = layoutParams
                }

                container.addView(textView)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

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
        if (hasOpenedWorkoutLog) {
            cvStartWorkout.findViewById<CardView>(R.id.cvStartWorkout)?.apply {
                setBackgroundColor(ContextCompat.getColor(context, R.color.grayish_lime_green))
            }
            cvStartWorkoutText.apply {
                text = "Continue"
            }
        }
    }


    private fun isColorSimilar(color1: Int, color2: Int, threshold: Int = 35): Boolean {
        val r1 = Color.red(color1)
        val g1 = Color.green(color1)
        val b1 = Color.blue(color1)

        val r2 = Color.red(color2)
        val g2 = Color.green(color2)
        val b2 = Color.blue(color2)

        val distance = sqrt(((r1 - r2).toDouble().pow(2.0)) + ((g1 - g2).toDouble().pow(2.0)) + ((b1 - b2).toDouble().pow(2.0)))
        return distance < threshold
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
        }, 2000) // 2 seconds delay
    }
}