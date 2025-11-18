package com.example.fyp_fitledger.ui.activity

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fyp_fitledger.R
import com.example.fyp_fitledger.ui.component.CircularPercentageRingView
import com.example.fyp_fitledger.ui.component.NavBarControl
import com.example.fyp_fitledger.data.local.DatabaseHelper
import com.example.fyp_fitledger.data.local.dao.MealDao
import com.example.fyp_fitledger.data.local.dao.MealDaoImpl
import com.example.fyp_fitledger.data.model.Nutrients
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate

class DietActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var mealDao: MealDao

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
        mealDao = MealDaoImpl(dbHelper)

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
        val intake = getTodayNutrientIntake()        // Nutrients
        val requirement = mealDao.getNutrientPlanByUserId(userId)  // Nutrients

        if (requirement == null) {
            Toast.makeText(this, "Nutrient Requirement not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Convert nutrients to a list for iteration
        val nutrientList = listOf(
            Triple("Calories", intake.calories, requirement.calories),
            Triple("Protein", intake.protein, requirement.protein),
            Triple("Carbohydrates", intake.carbohydrates, requirement.carbohydrates),
            Triple("Fat", intake.fat, requirement.fat),
            Triple("Iron", intake.iron, requirement.iron),
            Triple("Calcium", intake.calcium, requirement.calcium),
            Triple("Potassium", intake.potassium, requirement.potassium),
            Triple("Magnesium", intake.magnesium, requirement.magnesium),
            Triple("Zinc", intake.zinc, requirement.zinc),
            Triple("Sodium", intake.sodium, requirement.sodium),
            Triple("VitaminD", intake.vitaminD, requirement.vitaminD),
            Triple("VitaminA", intake.vitaminA, requirement.vitaminA),
            Triple("VitaminC", intake.vitaminC, requirement.vitaminC),
            Triple("VitaminK", intake.vitaminK, requirement.vitaminK),
            Triple("VitaminB12", intake.vitaminB12, requirement.vitaminB12)
        )

        nutrientList.forEach { (name, intakeValue, reqValue) ->
            val ringView = ringViews[name] ?: return@forEach
            val unit = getUnitForNutrient(name)

            // Ring % value
            val percentage = if (reqValue == 0f) 0f else (intakeValue / reqValue) * 100f
            ringView.setPercentage(percentage)

            // Adjust text size by nutrient category
            when (name) {
                "Calories" -> ringView.setCenterTextSize(40f)
                "Protein", "Carbohydrates", "Fat" -> ringView.setCenterTextSize(32f)
                else -> ringView.setCenterTextSize(26f)
            }

            // Under or over requirement
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

    private fun getTodayNutrientIntake(): Nutrients {

        val foods = mealDao.getAllNutrientByDate(userId, currentDate)

        var calories = 0f
        var protein = 0f
        var carbohydrates = 0f
        var fat = 0f
        var iron = 0f
        var calcium = 0f
        var potassium = 0f
        var magnesium = 0f
        var zinc = 0f
        var sodium = 0f
        var vitaminD = 0f
        var vitaminA = 0f
        var vitaminC = 0f
        var vitaminK = 0f
        var vitaminB12 = 0f

        for (f in foods) {
            val qty = f.quantity

            calories += f.calories * qty
            protein += f.protein * qty
            carbohydrates += f.carbohydrates * qty
            fat += f.fat * qty
            iron += f.iron * qty
            calcium += f.calcium * qty
            potassium += f.potassium * qty
            magnesium += f.magnesium * qty
            zinc += f.zinc * qty
            sodium += f.sodium * qty
            vitaminD += f.vitaminD * qty
            vitaminA += f.vitaminA * qty
            vitaminC += f.vitaminC * qty
            vitaminK += f.vitaminK * qty
            vitaminB12 += f.vitaminB12 * qty
        }

        return Nutrients(
            calories, protein, carbohydrates, fat,
            iron, calcium, potassium, magnesium, zinc, sodium,
            vitaminD, vitaminA, vitaminC, vitaminK, vitaminB12
        )
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
