package com.example.fyp_fitledger

import android.app.AlertDialog
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import org.eazegraph.lib.charts.PieChart
import java.time.LocalDate

class DietActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var database: SQLiteDatabase

    private lateinit var userId: String
    private val currentDate: String = LocalDate.now().toString()

    private lateinit var ringViews: Map<String, CircularPercentageRingView>
    private lateinit var btnAddDiet: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diet) // Your layout file

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        NavBarControl.setupBottomNavigation(this, bottomNav)

        dbHelper = DatabaseHelper(this)
        database = dbHelper.writableDatabase

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null)
            userId = currentUser.uid

        btnAddDiet = findViewById(R.id.btnAddDiet)

        ringViews = mapOf(
            "Calories" to findViewById(R.id.pctRingCalories),
            "Protein" to findViewById(R.id.pctRingProtein),
            "Carbohydrates" to findViewById(R.id.pctRingCarbs),
            "Fat" to findViewById(R.id.pctRingFat),
            "Iron" to findViewById(R.id.pctRingIron),
            "Calcium" to findViewById(R.id.pctRingCalcium),
            "Potassium" to findViewById(R.id.pctRingPotassium),
            "Magnesium" to findViewById(R.id.pctRingMagnesium),
            "Zinc" to findViewById(R.id.pctRingZinc),
            "Sodium" to findViewById(R.id.pctRingSodium),
            "VitaminD" to findViewById(R.id.pctRingVitaminD),
            "VitaminA" to findViewById(R.id.pctRingVitaminA),
            "VitaminB12" to findViewById(R.id.pctRingVitaminB12),
            "VitaminC" to findViewById(R.id.pctRingVitaminC),
            "VitaminK" to findViewById(R.id.pctRingVitaminK)
        )

        btnAddDiet.setOnClickListener{
            Log.d("--NutrientDebug", "Button clicked")
            val dialogView = layoutInflater.inflate(R.layout.dialog_log_diet_option, null)
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create()

            val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
            val pictureButton = dialogView.findViewById<Button>(R.id.pictureButton)
            val manualButton = dialogView.findViewById<Button>(R.id.manualButton)

            cancelButton.setOnClickListener{ dialog.dismiss() }
            pictureButton.setOnClickListener{
                dialog.dismiss()
                val intent = Intent(this, DietLogActivity::class.java)
                intent.putExtra("isPicture", true)
                startActivity(intent)
            }
            manualButton.setOnClickListener{
                dialog.dismiss()
                val intent = Intent(this, DietLogActivity::class.java)
                intent.putExtra("isPicture", false)
                startActivity(intent)
            }
            dialog.show()
        }
        updateNutrientRings()
    }

    private fun updateNutrientRings() {
        val intake = getTodayNutrientIntake()
        val requirement = getUserNutrientRequirements()

        intake.forEach { (nutrient, intakeValue) ->
            val reqValue = requirement[nutrient] ?: return@forEach
            val percentage = if (reqValue == 0f) 0f else (intakeValue / reqValue) * 100f
            val ringView = ringViews[nutrient] ?: return@forEach

            ringView.setPercentage(percentage)

            val unit = getUnitForNutrient(nutrient)

            when (nutrient) {
                "Calories" -> ringView.setCenterTextSize(40f)
                "Protein", "Carbohydrates", "Fat" -> ringView.setCenterTextSize(32f)
                else -> ringView.setCenterTextSize(26f) // Minerals and Vitamins
            }

            if (intakeValue <= reqValue) {
                val remaining = reqValue - intakeValue
                ringView.setCenterText(String.format("%.1f%s\nleft", remaining, unit))
                ringView.setRingColor(R.color.grayish_lime_green)
            } else {
                val over = intakeValue - reqValue
                ringView.setCenterText(String.format("over\n%.1f%s", over, unit))
                ringView.setRingColor(Color.RED)
            }
        }
    }

    private fun getTodayNutrientIntake(): Map<String, Float> {
        val db = dbHelper.readableDatabase
        val nutrients = mutableMapOf<String, Float>()

        val nutrientColumns = listOf(
            "Calories", "Protein", "Carbohydrates", "Fat",
            "Iron", "Calcium", "Potassium", "Magnesium", "Zinc", "Sodium",
            "VitaminD", "VitaminA", "VitaminC", "VitaminK", "VitaminB12"
        )

        // Set default value 0 for all nutrients
        nutrientColumns.forEach { nutrients[it] = 0f }

        val query = """
            SELECT f.*, mlf.Quantity
            FROM MealLog ml
            JOIN MealLogFood mlf ON ml.Log_ID = mlf.Log_ID
            JOIN Food f ON f.Food_Name = mlf.Food
            WHERE ml.User_ID = ? AND ml.Date = ?
        """

        val cursor = db.rawQuery(query, arrayOf(userId, currentDate))

        Log.d("--NutrientDebug", "Fetching intake for userId=$userId on date=$currentDate")

        while (cursor.moveToNext()) {
            val quantity = cursor.getFloat(cursor.getColumnIndexOrThrow("Quantity"))
            Log.d("--NutrientDebug", "Food quantity: $quantity")
            for (column in nutrientColumns) {
                val value = cursor.getFloat(cursor.getColumnIndexOrThrow(column))
                val newTotal = nutrients.getOrDefault(column, 0f) + (value * quantity)
                nutrients[column] = newTotal
                Log.d("--NutrientDebug", "$column: +${value * quantity} (total=$newTotal)")
            }
        }

        cursor.close()
        Log.d("--NutrientDebug", "Final intake values: $nutrients")
        return nutrients
    }


    private fun getUserNutrientRequirements(): Map<String, Float> {
        val db = dbHelper.readableDatabase
        val nutrients = mutableMapOf<String, Float>()

        val cursor = db.rawQuery(
            "SELECT * FROM NutrientRequirement WHERE User_ID = ?",
            arrayOf(userId)
        )

        Log.d("--NutrientDebug", "Fetching requirements for userId=$userId")

        if (cursor.moveToFirst()) {
            val nutrientColumns = listOf(
                "Calories", "Protein", "Carbohydrates", "Fat",
                "Iron", "Calcium", "Potassium", "Magnesium", "Zinc", "Sodium",
                "VitaminD", "VitaminA", "VitaminC", "VitaminK", "VitaminB12"
            )
            for (column in nutrientColumns) {
                val value = cursor.getFloat(cursor.getColumnIndexOrThrow(column))
                nutrients[column] = value
                Log.d("--NutrientDebug", "Requirement $column: $value")
            }
        } else {
            Log.d("--NutrientDebug", "No requirement data found for userId=$userId")
        }

        cursor.close()
        return nutrients
    }

    private fun getUnitForNutrient(nutrient: String): String {
        return when (nutrient) {
            "Calories" -> " kcal"
            "Protein", "Carbohydrates", "Fat" -> " g"
            "Iron", "Calcium", "Potassium", "Magnesium", "Zinc", "Sodium", "VitaminC" -> " mg"
            "VitaminD", "VitaminA", "VitaminK", "VitaminB12" -> " µg"
            else -> ""
        }
    }

    // You could define an extension function if needed
    private fun CircularPercentageRingView.setPercentage(percent: Float) {
        this@setPercentage.percentage = percent
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
