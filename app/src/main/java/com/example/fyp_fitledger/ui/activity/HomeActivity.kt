package com.example.fyp_fitledger.ui.activity

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.fyp_fitledger.R
import com.example.fyp_fitledger.ui.component.CircularPercentageRingView
import com.example.fyp_fitledger.ui.component.NavBarControl
import com.example.fyp_fitledger.data.local.DatabaseHelper
import com.github.mikephil.charting.animation.Easing
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import androidx.core.graphics.toColorInt
import com.example.fyp_fitledger.data.local.dao.MealDao
import com.example.fyp_fitledger.data.local.dao.MealDaoImpl
import com.example.fyp_fitledger.data.local.dao.WorkoutDao
import com.example.fyp_fitledger.data.local.dao.WorkoutDaoImpl

class HomeActivity : AppCompatActivity() {

    private var doubleBackToExitPressedOnce = false

    private lateinit var dateDay: TextView
    private lateinit var arrowLeft: ImageView
    private lateinit var arrowRight: ImageView

    private lateinit var ringViewCalorie: CircularPercentageRingView
    private lateinit var pieChartDiet: PieChart

    private lateinit var currentDay: LocalDate
    private lateinit var selectedDay: LocalDate

    private lateinit var tvCalorie: TextView


    private lateinit var userID: String

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var workoutDao: WorkoutDao
    private lateinit var mealDao: MealDao


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        tvCalorie = findViewById(R.id.tvCalIntake)

        arrowRight = findViewById(R.id.arrowRight)
        arrowLeft = findViewById(R.id.arrowLeft)

        currentDay = LocalDate.now()
        selectedDay = currentDay //currentDay.minusDays(n)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null)
            userID = currentUser.uid

        //userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        //userID = userViewModel.userID ?: ""

        dbHelper = DatabaseHelper(this)
        workoutDao = WorkoutDaoImpl(dbHelper)
        mealDao = MealDaoImpl(dbHelper)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        NavBarControl.setupBottomNavigation(this, bottomNav)

        dateDay = findViewById(R.id.dateDay)
        ringViewCalorie = findViewById(R.id.pctRingCalories)
        pieChartDiet = findViewById(R.id.pieChartDiet)


        arrowLeft.setOnClickListener {
            selectedDay = selectedDay.minusDays(1)
            updateDateDisplay()
            refreshAllData() // Your function to refresh pie chart, workout list, etc
        }

        arrowRight.setOnClickListener {
            selectedDay = selectedDay.plusDays(1)
            updateDateDisplay()
            refreshAllData()
        }

        dateDay.setOnClickListener {
            showCalendarPopup()
        }


        loadMealLogCalories(selectedDay.toString())
        loadMacronutrientDistribution(selectedDay.toString())
        loadWorkoutExercises(selectedDay.toString())
    }

    private fun updateDateDisplay() {
        val today = currentDay
        val selected = selectedDay

        val formatter = DateTimeFormatter.ofPattern("d MMM")
        val formattedDate = selected.format(formatter)

        val dayText = if (selected == today) {
            "Today\n$formattedDate"
        } else {
            val dayOfWeek = selected.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            "$dayOfWeek\n$formattedDate"
        }

        dateDay.text = dayText
    }

    private fun showCalendarPopup() {
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDay = LocalDate.of(year, month + 1, dayOfMonth)
                updateDateDisplay()
                refreshAllData()
            },
            selectedDay.year,
            selectedDay.monthValue - 1,
            selectedDay.dayOfMonth
        )
        datePicker.show()
    }

    private fun refreshAllData() {
        loadWorkoutExercises(selectedDay.toString())
        loadMealLogCalories(selectedDay.toString())
        loadMacronutrientDistribution(selectedDay.toString())
    }


    private fun loadWorkoutExercises(date: String) {
        val workoutContainer = findViewById<LinearLayout>(R.id.workoutContainer)
        workoutContainer.removeAllViews() // Clear previous views

        val exerciseNames = workoutDao.getExerciseNamesByDate(userID, date)

        if (exerciseNames.isNotEmpty()) {
            exerciseNames.forEach { name ->
                val exerciseTextView = TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 15, 0, 15) }
                    text = name
                    textSize = 14f
                    setTextColor(ContextCompat.getColor(context, R.color.black))
                    setPadding(20, 10, 20, 10)
                }
                workoutContainer.addView(exerciseTextView)
            }
        } else {
            val noRecordTextView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 50, 0, 50) }
                text = "No record found"
                textSize = 16f
                setTextColor(ContextCompat.getColor(context, R.color.white))
                setBackgroundColor(ContextCompat.getColor(context, R.color.des_cyan))
                setPadding(20, 10, 20, 10)
                gravity = Gravity.CENTER
            }
            workoutContainer.addView(noRecordTextView)
        }
    }

    private fun loadMacronutrientDistribution(date: String) {
        val macronutrients = mealDao.getMacronutrientsByDate(userID, date)

        var totalProtein = 0.0
        var totalCarbs = 0.0
        var totalFat = 0.0

        macronutrients.forEach { (protein, carbs, fat) ->
            totalProtein += protein
            totalCarbs += carbs
            totalFat += fat
        }
        // Calculate total for pie chart
        val total = totalProtein + totalCarbs + totalFat

        val proteinWeight = if (total != 0.0) (totalProtein / total * 100).toFloat() else 0f
        val carbWeight = if (total != 0.0) (totalCarbs / total * 100).toFloat() else 0f
        val fatWeight = if (total != 0.0) (totalFat / total * 100).toFloat() else 0f

        // Set text values
        findViewById<TextView>(R.id.tvDietCarbo).text = String.format("%.1fg", totalCarbs)
        findViewById<TextView>(R.id.tvDietFat).text = String.format("%.1fg", totalFat)
        findViewById<TextView>(R.id.tvDietProtein).text = String.format("%.1fg", totalProtein)

        // Create pie chart slices
        val entries = ArrayList<PieEntry>()
        val dataSet: PieDataSet

        if (total > 0) {
            // Normal case: show carbs, fat, protein
            if (carbWeight > 0) entries.add(PieEntry(carbWeight, "Carbs"))
            if (fatWeight > 0) entries.add(PieEntry(fatWeight, "Fat"))
            if (proteinWeight > 0) entries.add(PieEntry(proteinWeight, "Protein"))

            dataSet = PieDataSet(entries, "Macronutrients")
            dataSet.colors = listOf(
                "#4DB6AC".toColorInt(), // Carbs
                "#FFB74D".toColorInt(), // Fat
                "#9575CD".toColorInt()  // Protein
            )
        } else {
            // No data: show gray placeholder
            entries.add(PieEntry(100f, ""))

            dataSet = PieDataSet(entries, "")
            dataSet.colors = listOf(Color.LTGRAY)
            dataSet.setDrawValues(false)
        }

        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 12f

        val data = PieData(dataSet)

        // Apply it to the PieChart
        pieChartDiet.data = data

        pieChartDiet.description.isEnabled = false
        pieChartDiet.isDrawHoleEnabled = true
        pieChartDiet.setHoleColor(Color.TRANSPARENT)
        pieChartDiet.setEntryLabelColor(Color.BLACK)
        pieChartDiet.centerText = ""
        pieChartDiet.setCenterTextSize(10f)
        pieChartDiet.legend.isEnabled = false
        dataSet.setDrawValues(false)
        pieChartDiet.setNoDataText("")
        pieChartDiet.animateY(1000, Easing.EaseInOutQuad)

        pieChartDiet.invalidate() // Refresh chart
    }

    private fun loadMealLogCalories(date: String) {
        val totalCalories = mealDao.getTotalCaloriesByDate(userID, date)
        val calorieRequirement = mealDao.getNutrientPlanByUserId(userID)?.calories

        val progressPercent = (totalCalories / calorieRequirement!! * 100).toFloat().coerceIn(0f, 100f)

        ringViewCalorie.percentage = progressPercent
        tvCalorie.text = "${totalCalories.roundToInt()} kcal"
    }

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