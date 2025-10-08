package com.example.fyp_fitledger

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class DietLogActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var database: SQLiteDatabase

    private lateinit var userId: String
    private val currentDate: String = LocalDate.now().toString()

    private lateinit var ivFood: ImageView
    private lateinit var btnAddFood: Button
    private lateinit var btnFinish: Button

    private lateinit var photoUri: Uri

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()


    private var progressDialog: AlertDialog? = null // Loading Dialog

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
        private val REQUEST_CODE_CAMERA_OR_GALLERY = 101

        var foodPortion: String? = null
    }

    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var foodPickerLauncher: ActivityResultLauncher<Intent>

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            ivFood.visibility = View.VISIBLE
            ivFood.setImageURI(photoUri)
        } else {
            ivFood.visibility = View.GONE
            Toast.makeText(this, "Image capture cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diet_log)

        dbHelper = DatabaseHelper(this)
        database = dbHelper.writableDatabase

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null)
            userId = currentUser.uid

        ivFood = findViewById(R.id.ivFoodImage)
        btnAddFood = findViewById(R.id.btnAddFood)
        btnFinish = findViewById(R.id.btnFinish)

        btnAddFood.setOnClickListener {
            val intent = Intent(this, FoodListActivity::class.java)
            foodPickerLauncher.launch(intent)
        }

        btnFinish.setOnClickListener{
            saveDietLog()
        }

        val takePicture = intent.getBooleanExtra("isPicture", false)

        if (takePicture == true) {
            Log.d("--DietLogActivity", "takePicture is $takePicture")
            //checkCameraPermissionAndOpenCamera()      //only camera snap image
            launchCameraOrGalleryChooser()      //cameera or chooose from gallery
        } else {
            Log.d("--DietLogActivity", "takePicture is $takePicture")
            ivFood.visibility = View.GONE
        }

        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val selectedUri: Uri? = data?.data ?: photoUri
                if (selectedUri != null) {
                    ivFood.visibility = View.VISIBLE
                    ivFood.setImageURI(selectedUri)
                    getFoodFromImage()
                } else {
                    ivFood.visibility = View.GONE
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            } else {
                ivFood.visibility = View.GONE
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
            }
        }

        foodPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val selectedFood = result.data?.getStringExtra("selectedFoodName")
                selectedFood?.let {
                    Log.d("DietLog", "Selected food: $it")
                    addFoodToDietContainer(selectedFood)
                }
            }
        }
    }

    private fun checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }

    private fun openCamera() {
        val imageFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "food_${System.currentTimeMillis()}.jpg")
        photoUri = FileProvider.getUriForFile(this, "$packageName.provider", imageFile)
        cameraLauncher.launch(photoUri)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                ivFood.visibility = View.GONE
                Toast.makeText(this, "Camera permission is required to take photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun launchCameraOrGalleryChooser() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "food_${System.currentTimeMillis()}.jpg")
        photoUri = FileProvider.getUriForFile(this, "$packageName.provider", photoFile)
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

        val pickImageIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageIntent.type = "image/*"

        val chooser = Intent.createChooser(pickImageIntent, "Select or take a photo")
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(takePictureIntent))

        startActivityForResult(chooser, REQUEST_CODE_CAMERA_OR_GALLERY)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_CAMERA_OR_GALLERY -> {
                if (resultCode == Activity.RESULT_OK) {
                    val selectedUri: Uri? = data?.data ?: photoUri
                    if (selectedUri != null) {
                        ivFood.visibility = View.VISIBLE
                        ivFood.setImageURI(selectedUri)
                        getFoodFromImage()
                    } else {
                        ivFood.visibility = View.GONE
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    ivFood.visibility = View.GONE
                    Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
                }
            }

            1001 -> {
                if (resultCode == RESULT_OK) {
                    val selectedFood = data?.getStringExtra("selectedFoodName")
                    selectedFood?.let {
                        // Add to log or update UI
                        Log.d("DietLog", "Selected food: $it")
                        // Example: addSelectedFoodToLog(it)
                    }
                }
            }
        }
    }



    private fun getFoodFromImage() {
        showLoadingDialog(this)
        val ques = generateQuesitonString()
        //CoroutineScope(Dispatchers.IO).launch {}

        getResponse(ques, ivFood) { response ->
            this.runOnUiThread {
                foodPortion = response

                if (response.equals("No Food Were Found", ignoreCase = true)) {
                    Toast.makeText(this, "No food detected in the image.", Toast.LENGTH_SHORT).show()
                    dismissLoadingDialog()
                    return@runOnUiThread
                }

                val res = parseFoodPortion(response)
                addFoodToDietContainer(res)
                dismissLoadingDialog()
                Log.d("DietLogActivity-API", response)
            }
        }
    }

    fun generateQuesitonString(): String {
        val prompt = """            
            Based on USDA Foundation Foods, analyze the image and identify any visible foods. Output each food with its estimated portion in grams.
            
            Guidelines:
            1. Only include food names that exist in the USDA Foundation Foods list.
            2. Estimate portion size in grams.
            3. If no recognizable foods are found, respond exactly with: No Food Were Found
            
            Format:
            [foodName]:[portion] — multiple items separated by '|'
            
            Example:
            Egg:100|Rice:350
            """.trimIndent()

        Log.d("++DietLogActivity", prompt)
        return prompt
    }

    fun getResponse(question: String, image: ImageView, callback: (String) -> Unit) {
        val apiKey = getString(R.string.api_key)
        val url = "https://api.openai.com/v1/chat/completions"

        val jsonBody = JSONObject().apply {
            put("model", "gpt-4o")
            put("max_tokens", 500)
            put("temperature", 0)

            // Compose image and text parts
            val contentArray = JSONArray()

            val base64Image = imageViewToBase64(image)

            // If base64 image is provided, add it as image_url
            base64Image?.let {
                contentArray.put(JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().apply {
                        put("url", "data:image/jpeg;base64,$base64Image")
                    })
                })
            }

            // Add the text prompt
            contentArray.put(JSONObject().apply {
                put("type", "text")
                put("text", question)
            })

            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", contentArray)
                })
            }

            put("messages", messagesArray)
        }

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
                    if (jsonObject.has("error")) {
                        val errorMessage = jsonObject.getJSONObject("error").getString("message")
                        runOnUiThread {
                            Toast.makeText(this@DietLogActivity, "API Error, Please try again", Toast.LENGTH_LONG).show()
                            Log.e("API Error", "API Error: $errorMessage")
                        }
                        return
                    }
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

    fun parseFoodPortion(response: String): List<FoodPortionValue> {
        val foods = response.trim().split("|")
        val plan = mutableListOf<FoodPortionValue>()
        for (food in foods) {
            val split = food.split(":")
            if (split.size == 2) {
                val nameRaw: String = split[0].trim()
                val correctedName = matchFoodName(nameRaw, dbHelper.getColumnData("Food", "Food_Name").filterNotNull())
                val name = correctedName ?: nameRaw

                val amount = split[1].trim().toDoubleOrNull()
                if (amount != null) {
                    plan.add(FoodPortionValue(name, amount))
                }
                else
                    Log.e("DietLogActivity", "Invalid amount: ${split[1].trim()}")
            }
        }
        return plan
    }

    fun matchFoodName(inputName: String, validNames: List<String>): String? {
        val inputLower = inputName.lowercase()
        val inputTokens = inputLower.split(" ").toSet()

        val candidateNames = validNames.filter { it.lowercase().contains(inputLower) }

        var bestMatch: String? = null
        var bestScore = 0.0

        val targetList = if (candidateNames.isNotEmpty()) candidateNames else validNames

        for (name in targetList) {
            val nameTokens = name.lowercase().split(" ").toSet()
            val intersection = inputTokens.intersect(nameTokens).size.toDouble()
            val union = inputTokens.union(nameTokens).size.toDouble()

            val score = if (union == 0.0) 0.0 else intersection / union

            if (score == 1.0) return name // exact match

            if (score > bestScore) {
                bestScore = score
                bestMatch = name
            }
        }

        return if (candidateNames.isNotEmpty()) {
            // Always return best match from candidates that contain the inputName
            bestMatch
        } else {
            // Only return match if score meets strict threshold
            if (bestScore >= 0.75) bestMatch else null
        }
    }

    fun imageViewToBase64(imageView: ImageView): String? {
        val drawable = imageView.drawable ?: return null
        val bitmap = (drawable as BitmapDrawable).bitmap
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val bytes = stream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }


    private fun addFoodToDietContainer(foodName: String) {
        val dietContainer = findViewById<LinearLayout>(R.id.dietContainer)
        val itemView = LayoutInflater.from(this).inflate(R.layout.item_food, dietContainer, false)

        val tvFoodName = itemView.findViewById<TextView>(R.id.foodName)
        val spinnerPortion = itemView.findViewById<Spinner>(R.id.portionOption)
        val tvCalorie = itemView.findViewById<TextView>(R.id.calorie)
        val etQuantity = itemView.findViewById<EditText>(R.id.quantityPortion)

        val dbHelper = DatabaseHelper(this)

        lifecycleScope.launch {
            val food = withContext(Dispatchers.IO) {
                dbHelper.getFoodByName(foodName) // This should query the SQLite database for the food
            }

            if (food == null) {
                Log.e("AddFood", "Food not found: $foodName")
                return@launch
            }

            tvFoodName.text = food.Food_Name

            val portions = withContext(Dispatchers.IO) {
                dbHelper.getPortionsByFoodId(food.Food_ID) // This should query the SQLite database for portions
            }

            val spinnerOptions = mutableListOf("100 g")
            val gramsList = mutableListOf(100.0)

            for (portion in portions) {
                val label = "1 ${portion.Unit} (${portion.UnitValue}g)"
                spinnerOptions.add(label)
                gramsList.add(portion.UnitValue.toDouble())
            }

            val adapter = ArrayAdapter(this@DietLogActivity, android.R.layout.simple_spinner_item, spinnerOptions)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerPortion.adapter = adapter

            fun updateCalories() {
                val selectedGrams = gramsList[spinnerPortion.selectedItemPosition]
                val quantity = etQuantity.text.toString().toDoubleOrNull() ?: 1.0
                val totalGrams = selectedGrams * quantity
                val calories = (food.Calories?.div(100.0))?.times(totalGrams)
                tvCalorie.text = String.format("%.1f kcal", calories)
            }

            spinnerPortion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    updateCalories()
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

            etQuantity.setText("1")
            etQuantity.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    updateCalories()
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            updateCalories()
            dietContainer.addView(itemView)

            btnFinish.isEnabled = true
        }
    }

    private fun addFoodToDietContainer(foodItems: List<FoodPortionValue>) {
        val dbHelper = DatabaseHelper(this)

        lifecycleScope.launch {
            val dietContainer = findViewById<LinearLayout>(R.id.dietContainer)
            dietContainer.removeAllViews()

            for (portionValue in foodItems) {
                val food = withContext(Dispatchers.IO) {
                    dbHelper.getFoodByName(portionValue.name)
                }

                if (food != null) {
                    val itemView = LayoutInflater.from(this@DietLogActivity).inflate(R.layout.item_food, dietContainer, false)

                    val tvFoodName = itemView.findViewById<TextView>(R.id.foodName)
                    val spinnerPortion = itemView.findViewById<Spinner>(R.id.portionOption)
                    val tvCalorie = itemView.findViewById<TextView>(R.id.calorie)
                    val etQuantity = itemView.findViewById<EditText>(R.id.quantityPortion)

                    tvFoodName.text = food.Food_Name

                    val portions = withContext(Dispatchers.IO) {
                        dbHelper.getPortionsByFoodId(food.Food_ID)
                    }

                    val spinnerOptions = mutableListOf("100 g")
                    val gramsList = mutableListOf(100.0)

                    for (portion in portions) {
                        val label = "1 ${portion.Unit} (${portion.UnitValue}g)"
                        spinnerOptions.add(label)
                        gramsList.add(portion.UnitValue.toDouble())
                    }

                    val adapter = ArrayAdapter(this@DietLogActivity, android.R.layout.simple_spinner_item, spinnerOptions)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerPortion.adapter = adapter

                    etQuantity.setText((portionValue.amount / 100.0).toString()) // Pre-fill based on passed amount

                    fun updateCalories() {
                        val selectedGrams = gramsList[spinnerPortion.selectedItemPosition]
                        val quantity = etQuantity.text.toString().toDoubleOrNull() ?: 1.0
                        val totalGrams = selectedGrams * quantity
                        val calories = (food.Calories?.div(100.0))?.times(totalGrams)
                        tvCalorie.text = String.format("%.1f kcal", calories)
                    }

                    spinnerPortion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                            updateCalories()
                        }

                        override fun onNothingSelected(parent: AdapterView<*>) {}
                    }

                    etQuantity.addTextChangedListener(object : TextWatcher {
                        override fun afterTextChanged(s: Editable?) {
                            updateCalories()
                        }

                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    })

                    updateCalories()
                    dietContainer.addView(itemView)
                } else {
                    Log.e("AddFood", "Food not found: ${portionValue.name}")
                }
                btnFinish.isEnabled = true
            }
        }
    }


    private fun saveDietLog() {
        val dietContainer = findViewById<LinearLayout>(R.id.dietContainer)

        val foodEntries = mutableListOf<Triple<String, Double, Double>>() // (foodName, unitGram, quantity)

        for (i in 0 until dietContainer.childCount) {
            val itemView = dietContainer.getChildAt(i)
            val foodName = itemView.findViewById<TextView>(R.id.foodName).text.toString()
            val quantityStr = itemView.findViewById<EditText>(R.id.quantityPortion).text.toString()
            val spinner = itemView.findViewById<Spinner>(R.id.portionOption)
            val selectedPortion = spinner.selectedItem.toString()

            val quantity = quantityStr.toDoubleOrNull() ?: 0.0

            // Get gram value from spinner selection
            val unitGram = if (selectedPortion.contains("100 g")) {
                100.0
            } else {
                // Extract the (xxg) value from spinner label
                Regex("\\((\\d+(\\.\\d+)?)g\\)").find(selectedPortion)?.groupValues?.get(1)?.toDoubleOrNull() ?: 100.0
            }

            foodEntries.add(Triple(foodName, unitGram, quantity))
        }

        if (foodEntries.isEmpty()) {
            Toast.makeText(this, "No food entries to save!", Toast.LENGTH_SHORT).show()
            return
        }

        // Insert into MealLog
        val contentMealLog = ContentValues().apply {
            put("User_ID", userId)
            put("Date", currentDate)
            put("Notes", "") // Optional
        }
        val logId = database.insert("MealLog", null, contentMealLog)

        // Insert each food into MealLogFood
        for ((foodName, unitGram, quantity) in foodEntries) {
            val quantityPer100g = (quantity * unitGram) / 100.0  // Normalize to per 100g

            val contentFood = ContentValues().apply {
                put("Log_ID", logId)
                put("Food", foodName)
                put("Quantity", quantityPer100g)
            }
            database.insert("MealLogFood", null, contentFood)
            Log.d("DIET_LOG", "Inserted to log $logId for user $userId food:$foodName, quantity:$quantityPer100g")
        }

        Toast.makeText(this, "Diet log saved!", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, DietActivity::class.java)
        startActivity(intent)
        finish()
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

data class FoodPortionValue(
    val name: String,
    val amount: Double
)