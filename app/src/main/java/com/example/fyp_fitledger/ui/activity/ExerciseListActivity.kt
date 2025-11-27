package com.example.fyp_fitledger.ui.activity

import android.content.Intent
import android.os.Bundle
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
import com.example.fyp_fitledger.R
import com.example.fyp_fitledger.ui.dialog.AdvancedFilterDialog
import com.example.fyp_fitledger.data.local.DatabaseHelper
import com.example.fyp_fitledger.data.local.dao.ExerciseDao
import com.example.fyp_fitledger.data.local.dao.ExerciseDaoImpl
import com.example.fyp_fitledger.data.model.Exercise

class ExerciseListActivity : AppCompatActivity() {

    private lateinit var exerciseContainer: LinearLayout
    private lateinit var searchEditText: EditText
    private lateinit var filterButton: ImageButton
    private lateinit var filterOption: LinearLayout

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var exerciseDao: ExerciseDao

    private var allExercises: List<Exercise> = listOf()
    private var filteredExercises: List<Exercise> = listOf()

    private var selectedCategories = mutableSetOf<String>()
    private var currentSearchQuery: String = ""

    private var selectedMuscles: Set<String> = emptySet()
    private var selectedEquipments: Set<String> = emptySet()
    private var isCardioSelected: Boolean = false

    private var isUsingAdvancedFilter = false

    // always return this as the result list
    private val selectedExercises = ArrayList<String>()

    private lateinit var detailResultLauncher: ActivityResultLauncher<Intent>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_list)

        dbHelper = DatabaseHelper(this)
        exerciseDao = ExerciseDaoImpl(dbHelper)

        exerciseContainer = findViewById(R.id.exerciseContainer)
        searchEditText = findViewById(R.id.searchEditText)
        filterButton = findViewById(R.id.filterButton)
        filterOption = findViewById(R.id.filterOption)


        // hide keyboard when pressing Enter
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(searchEditText.windowToken, 0)
                searchEditText.clearFocus()
                true
            } else false
        }

        // Launcher to get result from ExerciseDetailActivity
        detailResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

                if (result.resultCode == RESULT_OK) {
                    val data = result.data
                    val addedExercise = data?.getStringExtra("exerciseName")

                    if (addedExercise != null) {
                        selectedExercises.clear()
                        selectedExercises.add(addedExercise)

                        val returnIntent = Intent().apply {
                            putStringArrayListExtra("SELECTED_EXERCISES", selectedExercises)
                        }
                        setResult(RESULT_OK, returnIntent)
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
        allExercises = exerciseDao.getAllExercises()
        filteredExercises = allExercises
        displayExercises(filteredExercises)
    }


    private fun displayExercises(exercises: List<Exercise>) {
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
                imageView.setImageResource(R.drawable.default_image_background)
            }

            textView.text = exercise.name


            // When user clicks an exercise → open detail → return exercise name
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

                isUsingAdvancedFilter = false
                clearAdvancedFilterSelections()

                if (selectedCategories.contains(category)) {
                    selectedCategories.remove(category)
                    chip.backgroundTintList =
                        ContextCompat.getColorStateList(this, R.color.light_grey)
                } else {
                    selectedCategories.add(category)
                    chip.backgroundTintList =
                        ContextCompat.getColorStateList(this, R.color.des_cyan)
                }

                applySimpleFilters()
            }
        }
    }


    private fun applySimpleFilters() {
        filteredExercises = allExercises.filter { exercise ->

            val matchesCategory = if (selectedCategories.isEmpty()) true
            else selectedCategories.any { exercise.category.contains(it, ignoreCase = true) }

            val matchesSearch = if (currentSearchQuery.isBlank()) true
            else exercise.name.lowercase().contains(currentSearchQuery)

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

                    isUsingAdvancedFilter = true
                    clearSimpleFilterSelections()

                    filteredExercises = filtered
                    displayExercises(filteredExercises)

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
            chip.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.light_grey)
        }
    }


    private fun clearAdvancedFilterSelections() {
        selectedMuscles = emptySet()
        selectedEquipments = emptySet()
        isCardioSelected = false
    }
}


