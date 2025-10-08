package com.example.fyp_fitledger

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.bumptech.glide.Glide

class ExerciseListActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var exerciseContainer: LinearLayout
    private lateinit var searchEditText: EditText
    private lateinit var filterButton: ImageButton
    private lateinit var filterOption: LinearLayout

    private var allExercises: List<ExerciseSummary> = listOf()
    private var filteredExercises: List<ExerciseSummary> = listOf()

    private var selectedCategories = mutableSetOf<String>()
    private var currentSearchQuery: String = ""

    private var selectedMuscles: Set<String> = emptySet()
    private var selectedEquipments: Set<String> = emptySet()
    private var isCardioSelected: Boolean = false

    private var isUsingAdvancedFilter = false // ← New flag to track mode

    private lateinit var detailResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_list)

        dbHelper = DatabaseHelper(this)
        exerciseContainer = findViewById(R.id.exerciseContainer)
        searchEditText = findViewById(R.id.searchEditText)
        filterButton = findViewById(R.id.filterButton)
        filterOption = findViewById(R.id.filterOption)

        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(searchEditText.windowToken, 0)
                searchEditText.clearFocus()
                true
            } else {
                false
            }
        }

        detailResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val addedExercise = data?.getStringExtra("exerciseName")
                if (addedExercise != null) {
                    val returnIntent = Intent().apply {
                        putExtra("exerciseName", addedExercise)
                    }
                    setResult(Activity.RESULT_OK, returnIntent)
                    finish()
                }
            }
        }


        loadExercises()
        setupSearch()
        setupFilterPopup()
        setupMuscleFilter()
    }

    private fun loadExercises() {
        val exerciseRepository = ExerciseRepository(dbHelper)
        allExercises = exerciseRepository.getExerciseSummaries()
        filteredExercises = allExercises
        displayExercises(filteredExercises)
    }

    private fun displayExercises(exercises: List<ExerciseSummary>) {
        exerciseContainer.removeAllViews()
        for (exercise in exercises) {
            val itemView = layoutInflater.inflate(R.layout.exercise_item, null)
            val imageView = itemView.findViewById<ImageView>(R.id.exerciseGif)
            val textView = itemView.findViewById<TextView>(R.id.exerciseName)

            val gifResourceId = resources.getIdentifier(exercise.gifUrl, "drawable", packageName)
            if (gifResourceId != 0) {
                Glide.with(this)
                    .asGif()
                    .load(gifResourceId)
                    .into(imageView)
            } else {
                // Optional fallback if not found
                imageView.setImageResource(R.drawable.default_image_background)  // put any default image in drawable
                Log.e("GIF_LOAD", "Resource not found: ${exercise.gifUrl}")
            }


            textView.text = exercise.name

            itemView.setOnClickListener {
                val intent = Intent(this, ExerciseDetailActivity::class.java)
                intent.putExtra("exercise_name", exercise.name)
                detailResultLauncher.launch(intent)
            }


            exerciseContainer.addView(itemView)
        }
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener {
            currentSearchQuery = it.toString().lowercase()
            if (!isUsingAdvancedFilter) {
                applySimpleFilters()
            }
        }
    }

    private fun setupMuscleFilter() {
        for (i in 0 until filterOption.childCount) {
            val chip = filterOption.getChildAt(i) as TextView
            chip.setOnClickListener {
                val category = chip.text.toString()

                isUsingAdvancedFilter = false // ← Switch to Simple filter mode
                clearAdvancedFilterSelections() // ← Clear advanced selections

                if (selectedCategories.contains(category)) {
                    selectedCategories.remove(category)
                    chip.backgroundTintList = ContextCompat.getColorStateList(this, R.color.light_grey)
                } else {
                    selectedCategories.add(category)
                    chip.backgroundTintList = ContextCompat.getColorStateList(this, R.color.des_cyan)
                }

                applySimpleFilters()
            }
        }
    }

    private fun applySimpleFilters() {
        filteredExercises = allExercises.filter { exercise ->
            val matchesCategory = if (selectedCategories.isEmpty()) {
                true
            } else {
                selectedCategories.all { selectedCategory ->
                    exercise.category.lowercase().contains(selectedCategory.lowercase())
                }
            }

            val matchesSearch = if (currentSearchQuery.isBlank()) {
                true
            } else {
                exercise.name.lowercase().contains(currentSearchQuery)
            }

            matchesCategory && matchesSearch
        }

        displayExercises(filteredExercises)
    }

    private fun setupFilterPopup() {
        filterButton.setOnClickListener {
            val popup = AdvancedFilterDialog(
                context = this,
                allExercises = allExercises,
                onFilterApplied = { filtered, muscles, equipments, cardio ->
                    isUsingAdvancedFilter = true // ← Switch to Advanced mode
                    clearSimpleFilterSelections() // ← Clear chip selections

                    filteredExercises = filtered
                    displayExercises(filteredExercises)

                    // Save advanced selections
                    selectedMuscles = muscles
                    selectedEquipments = equipments
                    isCardioSelected = cardio
                },
                previouslySelectedMuscles = selectedMuscles,
                previouslySelectedEquipments = selectedEquipments,
                previouslySelectedCardio = isCardioSelected
            )
            popup.show()
        }
    }

    private fun clearSimpleFilterSelections() {
        selectedCategories.clear()

        for (i in 0 until filterOption.childCount) {
            val chip = filterOption.getChildAt(i) as TextView
            chip.backgroundTintList = ContextCompat.getColorStateList(this, R.color.light_grey)
        }
    }

    private fun clearAdvancedFilterSelections() {
        selectedMuscles = emptySet()
        selectedEquipments = emptySet()
        isCardioSelected = false
    }
}

