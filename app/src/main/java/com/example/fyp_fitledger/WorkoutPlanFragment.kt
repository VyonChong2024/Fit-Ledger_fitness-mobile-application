package com.example.fyp_fitledger

import android.app.AlertDialog
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class WorkoutPlanFragment: Fragment() {

    private lateinit var userViewModel: UserViewModel
    private lateinit var workoutPlanViewModel: WorkoutPlanViewModel

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var database: SQLiteDatabase

    private lateinit var workoutPlanContainer: LinearLayout
    private lateinit var btContinue: Button
    private val client = OkHttpClient()

    companion object {
        var workoutPlan: String? = null
    }


    private var progressDialog: AlertDialog? = null // Loading Dialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_workout_plan, container, false) // Store the view

        dbHelper = DatabaseHelper(requireContext())
        database = dbHelper.writableDatabase

        // Initialize ViewModel
        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)
        workoutPlanViewModel = ViewModelProvider(requireActivity()).get(WorkoutPlanViewModel::class.java)

        // Load saved user data
        userViewModel.loadFromPreferences(requireContext())

        workoutPlanContainer = view.findViewById(R.id.workoutPlanContainer)
        btContinue = view.findViewById(R.id.btContinue)

        val chatGPTHelper = ChatGPTHelper()

        showLoadingDialog(requireContext())


        val ques = generateQuesitonString()

        //passing question to APi
        getResponse(ques) { response ->
            requireActivity().runOnUiThread {
                workoutPlan = response
                val res = parseWorkoutPlan(response)
                displayWorkoutPlan(res)
                dismissLoadingDialog()
                Log.d("WorkoutPlanFragment-API", response)
            }
        }

        //Debugging purpose
        /*
        val sampleOutput = "Monday|Chest & Triceps|Barbell Bench Press:4:12|Incline Dumbbell Press:4:12|Cable Crossover:3:15|Tricep Pushdown:3:15\n" +
                "Tuesday|Back & Biceps|Bent Over Barbell Row:4:12|Lat Pulldown:4:12|Seated Cable Row:3:15|Barbell Curl:3:15\n" +
                "Thursday|Shoulders & Abs|Arnold Press:4:12|Lateral Raise:4:12|Reverse Flys:3:15|Russian Twist:3:20\n" +
                "Friday|Full Body|Deadlift:4:12|Squat:4:12|Pull-Up:3:12|Push-Up:3:15"

        val res = parseWorkoutPlan(sampleOutput)
        displayWorkoutPlan(res)

        dismissLoadingDialog()
         */

        btContinue.isEnabled = true

        btContinue.setOnClickListener {
            (activity as? DemographicActivity)?.addFragment("DietPlanFragment")
            (activity as? DemographicActivity)?.nextPage()
        }

        return view
    }

    fun generateQuesitonString(): String {
        val gender = userViewModel.gender
        val age = userViewModel.age
        val height = (userViewModel.height)?.format(2)
        val weight = (userViewModel.weight)?.format(2)
        val bodyFatPercent = (userViewModel.bodyFatPercent)?.format(1)
        val bodyDensity = (userViewModel.bodyDensity)?.format(3)
        val accFat = userViewModel.accFat

        val goal = workoutPlanViewModel.goal
        val targetBodyFat = workoutPlanViewModel.targetBodyFat
        val targetWeight = workoutPlanViewModel.targetWeight
        val activityLevel = workoutPlanViewModel.activityLevel
        val activityFrequency = workoutPlanViewModel.activityFrequency
        val targetMuscles = workoutPlanViewModel.targetMuscles
        val daySelected = workoutPlanViewModel.daySelected
        val duration = workoutPlanViewModel.duration

        val workoutNames = retrieveAllWorkoutName()

        val prompt = """
            These are all available workouts:
            $workoutNames
            
            User Info:
            Gender: $gender
            Age: $age
            Height: $height cm
            Weight: $weight kg
            Body Fat %: $bodyFatPercent
            
            Goal: $goal
            Activity Level: $activityLevel
            Activity Frequency: $activityFrequency
            Target Muscles: $targetMuscles
            Workout Days: $daySelected
            Workout Duration: $duration minutes per day
            
            Guidelines:
            1. All muscle groups should have at least 2 recovery days before being trained again.
            2. Suggest the reps with exact numbers (not a range).
            3. Output format: 
               [day]|[workout name]|[exercise1]:[sets]:[reps]|[exercise2]:[sets]:[reps]...
            4. The number of exercises should be based on the total workout duration, using a ratio of 1 exercise per 15 minutes. However, this may vary depending on the intensity level of the activity.
            5. Only include exercises that target the following muscle groups: $targetMuscles. Do not include exercises for other muscle groups not listed here.
            6. It's acceptable for the same muscle group to be trained more than once per week, as long as it follows rule #1.
            7. The exercises name must exactly match the workout name. 
            
            Example:
            Monday|Push Day (chest & shoulder)|Barbell Bench Press:4:12|Incline Dumbbell Bench Press:4:12...
            """.trimIndent()


        Log.d("WorkoutPlanFragment", prompt)
        return prompt
    }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this).toDouble()

    fun getResponse(question: String, callback: (String) -> Unit) {
        val apiKey = getString(R.string.api_key)
        val url = "https://api.openai.com/v1/chat/completions"

        val jsonBody = JSONObject()
        jsonBody.put("model", "gpt-3.5-turbo")

        val messagesArray = JSONArray()
        val userMessage = JSONObject()
        userMessage.put("role", "user")
        userMessage.put("content", question)
        messagesArray.put(userMessage)

        jsonBody.put("messages", messagesArray)
        jsonBody.put("max_tokens", 500)
        jsonBody.put("temperature", 0)

        val body = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("API Error", "Request failed", e)
                callback("Error: ${e.message}")
                dismissLoadingDialog()
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (body == null) {
                    callback("No response from server.")
                    dismissLoadingDialog()
                    return
                }

                try {
                    val jsonObject = JSONObject(body)
                    val choicesArray = jsonObject.getJSONArray("choices")
                    val message = choicesArray.getJSONObject(0).getJSONObject("message")
                    val content = message.getString("content")
                    callback(content.trim())
                } catch (e: Exception) {
                    Log.e("Parsing Error", "Invalid JSON structure: $body", e)
                    callback("Failed to parse response.")
                }
                dismissLoadingDialog()
            }
        })
    }

    fun retrieveAllWorkoutName(): String {
        val workoutNames = dbHelper.getColumnData("Exercise", "Name").joinToString(", ")
        return workoutNames
    }

    fun parseWorkoutPlan(response: String): List<WorkoutPlanDay> {
        val lines = response.trim().split("\n")
        val plan = mutableListOf<WorkoutPlanDay>()

        for (line in lines) {
            val parts = line.split("|")
            if (parts.size >= 3) {
                val day = parts[0].trim()
                val workoutName = parts[1].trim()
                val exercises = parts.drop(2).mapNotNull {
                    val split = it.split(":")
                    if (split.size == 3) {
                        val nameRaw = split[0].trim()
                        //Get the exact name from the Exercise table and find the best match
                        val correctedName = matchExerciseName(nameRaw, dbHelper.getColumnData("Exercise", "Name").filterNotNull())
                        val name = correctedName ?: nameRaw

                        val sets = split[1].trim().toIntOrNull()
                        val reps = split[2].trim().toIntOrNull()
                        if (sets != null && reps != null) {
                            Exercise(name, sets, reps)
                        } else null
                    } else null
                }
                plan.add(WorkoutPlanDay(day, workoutName, exercises))
            }
        }

        return plan
    }

    fun matchExerciseName(inputName: String, validNames: List<String>): String? {
        val inputTokens = inputName.lowercase().split(" ").toSet()
        var bestMatch: String? = null
        var bestScore = 0.0

        for (name in validNames) {
            val nameTokens = name.lowercase().split(" ").toSet()
            val intersection = inputTokens.intersect(nameTokens).size.toDouble()
            val union = inputTokens.union(nameTokens).size.toDouble()

            val score = if (union == 0.0) 0.0 else intersection / union

            if (score == 1.0) {
                return name //exact match of name found
            }

            if (score > bestScore) {
                bestScore = score
                bestMatch = name
            }
        }

        return if (bestScore >= 0.4) bestMatch else null
    }

    fun displayWorkoutPlan(workoutPlan: List<WorkoutPlanDay>) {
        workoutPlanContainer.removeAllViews()

        for (day in workoutPlan) {
            val dayLayoutContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 24, 0, 24)
                }
                setPadding(16, 16, 16, 16)
                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.des_cyan))
            }

            val dayTitle = TextView(context).apply {
                text = "${day.day} - ${day.workoutName}"
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.light_grey))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            }

            //Add exercise list container
            val exerciseListLayoutContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            //Add exercise list container header
            val headerRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            val nameHeader = TextView(context).apply {
                text = "Exercise"
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.light_grayish_blue))  //Color.DKGRAY
                setPadding(8, 8, 8, 8)
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 2f)
            }
            val setsHeader = TextView(context).apply {
                text = "Sets"
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.light_grayish_blue))
                setPadding(8, 8, 8, 8)
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            }
            val repsHeader = TextView(context).apply {
                text = "Reps"
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.light_grayish_blue))
                setPadding(8, 8, 8, 8)
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            }

            headerRow.addView(nameHeader)
            headerRow.addView(setsHeader)
            headerRow.addView(repsHeader)
            exerciseListLayoutContainer.addView(headerRow)

            //Loop for each exercise to display
            for (exercise in day.exercises) {
                //Add exercise container
                val exerciseLayoutContainer = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val nameColumn = TextView(context).apply {
                    text = exercise.name
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setPadding(8, 8, 8, 8)
                    layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 2f)
                }

                val setColumn = TextView(context).apply {
                    text = "${exercise.sets}"
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setPadding(8, 8, 8, 8)
                    gravity = Gravity.END
                    layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
                }

                val repsColumn = TextView(context).apply {
                    text = "${exercise.reps}"
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setPadding(8, 8, 8, 8)
                    gravity = Gravity.END
                    layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
                }

                exerciseLayoutContainer.addView(nameColumn)
                exerciseLayoutContainer.addView(setColumn)
                exerciseLayoutContainer.addView(repsColumn)

                exerciseListLayoutContainer.addView(exerciseLayoutContainer)
            }

            dayLayoutContainer.addView(dayTitle)
            dayLayoutContainer.addView(exerciseListLayoutContainer)
            workoutPlanContainer.addView(dayLayoutContainer)

            val divider = View(context)
            divider.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
            )
            divider.setBackgroundColor(Color.GRAY)
            workoutPlanContainer.addView(divider)
        }
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
}

data class WorkoutPlanDay(
    val day: String,
    val workoutName: String,
    val exercises: List<Exercise>
)

data class Exercise(
    val name: String,
    val sets: Int,
    val reps: Int
)


