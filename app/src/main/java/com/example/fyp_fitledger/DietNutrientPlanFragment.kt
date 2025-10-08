package com.example.fyp_fitledger

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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

class DietNutrientPlanFragment : Fragment() {

    private lateinit var userViewModel: UserViewModel
    private lateinit var workoutPlanViewModel: WorkoutPlanViewModel

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var database: SQLiteDatabase

    private lateinit var workoutPlanContainer: LinearLayout
    private lateinit var btContinue: Button
    private val client = OkHttpClient()

    companion object {
        lateinit var responseString: String
    }

    private var progressDialog: AlertDialog? = null // Loading Dialog
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_diet_nutrient_plan, container, false)

        // Initialize ViewModel
        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)
        workoutPlanViewModel = ViewModelProvider(requireActivity()).get(WorkoutPlanViewModel::class.java)

        // Load saved user data
        userViewModel.loadFromPreferences(requireContext())

        btContinue = view.findViewById(R.id.btContinue)

        val chatGPTHelper = ChatGPTHelper()

        showLoadingDialog(requireContext())

        val ques = generateQuesitonString()
        //passing question to APi
        getResponse(ques) { response ->
            requireActivity().runOnUiThread {
                responseString = response
                dismissLoadingDialog()
                Log.d("WorkoutPlanFragment-API", response)

                val parsedData = parseNutrientResponse(response)
                if (parsedData.isNotEmpty()) {
                    saveNutrientRequirementsToDB(parsedData)
                }
            }
        }

        btContinue.setOnClickListener {
            (activity as? DemographicActivity)?.addFragment("FinishSetupFragment")
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

        val goal = workoutPlanViewModel.goal
        val activityLevel = workoutPlanViewModel.activityLevel
        val activityFrequency = workoutPlanViewModel.activityFrequency
        val dietPlan = workoutPlanViewModel.dietPlan

        val prompt = """
            You are a nutrition expert.
            
            Here is a list of nutrients to calculate:
            Calories, Protein, Carbohydrates, Fat, Iron, Calcium, Potassium, Magnesium, Zinc, Sodium, VitaminD, VitaminA, VitaminC, VitaminK, VitaminB12.
            
            User Information:
            - Gender: $gender
            - Age: $age
            - Height: $height cm
            - Weight: $weight kg
            - Body Fat Percentage: $bodyFatPercent
            
            - Goal: $goal
            - Activity Level: $activityLevel
            - Activity Frequency: $activityFrequency
            - Diet Plan Preference: $dietPlan
            
            Instructions:
            1. If any value is 'null', treat it as missing or unspecified.
            2. Use these units internally: 
               Calories = kcal, Protein = g, Carbohydrates = g, Fat = g, Iron = mg, Calcium = mg, Potassium = mg, Magnesium = mg, 
               Zinc = mg, Sodium = mg, VitaminD = mcg, VitaminA = mcg, VitaminC = mg, VitaminK = mcg, VitaminB12 = mcg.
            3. Do not mention units in the response.
            4. Only return the values in this format (no extra text or explanation):
               [Nutrient1]:[value]|[Nutrient2]:[value]|...
            
            Example Output:
            Calories:2500|Protein:165|Carbohydrates:312|Fat:70|Iron:18|...
            
            Based on the above information, generate a full list of nutrient requirements.
        """.trimIndent()

        Log.d("DietNutrientPlanFragment", prompt)
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

    private fun parseNutrientResponse(response: String): Map<String, Float> {
        val nutrientMap = mutableMapOf<String, Float>()
        val pairs = response.split("|")
        for (pair in pairs) {
            val parts = pair.split(":")
            if (parts.size == 2) {
                val key = parts[0].trim()
                val value = parts[1].trim().toFloatOrNull()
                if (value != null) {
                    nutrientMap[key] = value
                }
            }
        }
        return nutrientMap
    }

    private fun saveNutrientRequirementsToDB(nutrientMap: Map<String, Float>) {
        dbHelper = DatabaseHelper(requireContext())
        database = dbHelper.writableDatabase

        val userId = userViewModel.userID
        val columns = listOf(
            "Calories", "Protein", "Carbohydrates", "Fat",
            "Iron", "Calcium", "Potassium", "Magnesium", "Zinc", "Sodium",
            "VitaminD", "VitaminA", "VitaminC", "VitaminK", "VitaminB12"
        )

        val values = ContentValues().apply {
            put("User_ID", userId)
            for (column in columns) {
                put(column, nutrientMap[column] ?: 0f)
            }
        }

        // Delete existing row if it exists
        database.delete("NutrientRequirement", "User_ID = ?", arrayOf(userId))

        // Insert new values
        database.insert("NutrientRequirement", null, values)
        Log.d("DietPlanDB", "Nutrient requirements saved for user $userId")
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