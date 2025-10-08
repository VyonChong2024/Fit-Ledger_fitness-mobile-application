package com.example.fyp_fitledger

import android.app.DatePickerDialog
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.doOnNextLayout
import androidx.core.view.setMargins
import androidx.core.view.setPadding
import androidx.lifecycle.ViewModelProvider
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
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

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
    private lateinit var database: SQLiteDatabase

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
        database = dbHelper.writableDatabase

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
        val db = dbHelper.readableDatabase

        val workoutContainer = findViewById<LinearLayout>(R.id.workoutContainer)
        workoutContainer.removeAllViews() // Clear previous views

        val query = """
            SELECT DISTINCT e.Name
            FROM ExerciseSet es
            JOIN WorkoutExercise we ON es.WorkoutExercise_ID = we.WorkoutExercise_ID
            JOIN Exercise e ON we.Exercise_ID = e.Exercise_ID
            JOIN WorkoutLog wl ON we.Log_ID = wl.Log_ID
            WHERE wl.User_ID = ? AND wl.Date = ?
        """

        val cursor = db.rawQuery(query, arrayOf(userID, date))

        if (cursor.moveToFirst()) {
            do {
                val exerciseName = cursor.getString(cursor.getColumnIndexOrThrow("Name"))

                val exerciseTextView = TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 15, 0, 15) // Space between exercises
                    }
                    text = exerciseName
                    textSize = 14f
                    setTextColor(ContextCompat.getColor(context, R.color.black))
                    setPadding(20, 10, 20, 10)
                }

                workoutContainer.addView(exerciseTextView)
            } while (cursor.moveToNext())
        } else {
            // If no exercises, show 'No record found'
            val noRecordTextView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0,50,0,50)
                }
                text = "No record found"
                textSize = 16f
                setTextColor(ContextCompat.getColor(context, R.color.white))
                setBackgroundColor(ContextCompat.getColor(context, R.color.des_cyan))
                setPadding(20, 10, 20, 10)
                gravity = Gravity.CENTER
            }

            workoutContainer.addView(noRecordTextView)
        }

        cursor.close()
    }

    private fun loadMacronutrientDistribution(date: String) {
        val db = dbHelper.readableDatabase

        val query = """
            SELECT f.Protein, f.Carbohydrates, f.Fat, mlf.Quantity
            FROM MealLog ml
            JOIN MealLogFood mlf ON ml.Log_ID = mlf.Log_ID
            JOIN Food f ON f.Food_Name = mlf.Food
            WHERE ml.Date = ? AND ml.User_ID = ?
        """

        val cursor = db.rawQuery(query, arrayOf(date, userID))
        Log.d("**HomeActivity", "Date: $date, ID: $userID")

        var totalProtein = 0.0
        var totalCarbs = 0.0
        var totalFat = 0.0

        while (cursor.moveToNext()) {
            val protein = cursor.getDouble(cursor.getColumnIndexOrThrow("Protein"))
            val carbs = cursor.getDouble(cursor.getColumnIndexOrThrow("Carbohydrates"))
            val fat = cursor.getDouble(cursor.getColumnIndexOrThrow("Fat"))
            val quantity = cursor.getDouble(cursor.getColumnIndexOrThrow("Quantity"))

            totalProtein += protein * quantity
            totalCarbs += carbs * quantity
            totalFat += fat * quantity
        }

        cursor.close()

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
                Color.parseColor("#4DB6AC"), // Carbs
                Color.parseColor("#FFB74D"), // Fat
                Color.parseColor("#9575CD")  // Protein
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
        val db = dbHelper.readableDatabase
        var totalCalories = 0.0
        var calorieRequirement = 2000.0  // Default, will update based on user ID

        // 1. Get the total calories from MealLog
        val calorieQuery = """
            SELECT f.Calories, mf.Quantity
            FROM MealLog ml
            JOIN MealLogFood mf ON ml.Log_ID = mf.Log_ID
            JOIN Food f ON mf.Food = f.Food_Name
            WHERE ml.User_ID = ? AND ml.Date = ?
        """.trimIndent()

        val cursor = db.rawQuery(calorieQuery, arrayOf(userID, date))
        if (cursor.moveToFirst()) {
            do {
                val caloriesPerUnit = cursor.getDouble(0)
                val quantity = cursor.getDouble(1)
                totalCalories += caloriesPerUnit * quantity
                Log.d("**CursorCheck", "Quantity: $quantity, Calories: $caloriesPerUnit")
            } while (cursor.moveToNext())
        }
        cursor.close()

        // 2. Get the user's calorie requirement
        val reqCursor = db.rawQuery(
            "SELECT Calories FROM NutrientRequirement WHERE User_ID = ?",
            arrayOf(userID)
        )

        if (reqCursor.moveToFirst()) {
            calorieRequirement = reqCursor.getDouble(0)
        }
        reqCursor.close()

        // 3. Apply to RingView & TextView
        val progressPercent = (totalCalories / calorieRequirement * 100).toFloat().coerceIn(0f, 100f)
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