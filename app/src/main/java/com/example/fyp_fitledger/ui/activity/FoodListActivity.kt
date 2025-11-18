package com.example.fyp_fitledger.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.fyp_fitledger.R
import com.example.fyp_fitledger.data.local.DatabaseHelper
import com.example.fyp_fitledger.data.local.dao.FoodDao
import com.example.fyp_fitledger.data.local.dao.FoodDaoImpl

class FoodListActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var foodDao: FoodDao
    private lateinit var foodContainer: LinearLayout
    private lateinit var searchEditText: EditText
    private var allFood: List<String> = listOf() // This will store all food names
    private var filteredFood: List<String> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_food_list)

        dbHelper = DatabaseHelper(this)
        foodDao = FoodDaoImpl(dbHelper)

        foodContainer = findViewById(R.id.foodContainer)
        searchEditText = findViewById(R.id.searchEditText)

        allFood = foodDao.getAllFoodName()
        filteredFood = allFood
        displayFoods(filteredFood)

        searchEditText.addTextChangedListener {
            val query = it.toString().trim().lowercase()
            filteredFood = if (query.isEmpty()) {
                allFood
            } else {
                allFood.filter { food -> food.lowercase().contains(query) } // Filter based on query
            }
            displayFoods(filteredFood) // Update the displayed food items
        }

        // To hide the keyboard after pressing the "Done" button
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(searchEditText.windowToken, 0)
                searchEditText.clearFocus()
                true
            } else {
                false
            }
        }
    }

    // Function to display the foods
    private fun displayFoods(foods: List<String>) {
        foodContainer.removeAllViews()
        for (foodName in foods) {
            val itemLayout = LayoutInflater.from(this).inflate(R.layout.item_food_entry, foodContainer, false)

            val foodText = itemLayout.findViewById<TextView>(R.id.foodNameText)
            val addButton = itemLayout.findViewById<Button>(R.id.btnAddFood)

            foodText.text = foodName
            addButton.setOnClickListener {
                val resultIntent = Intent()
                resultIntent.putExtra("selectedFoodName", foodName)
                setResult(RESULT_OK, resultIntent)
                finish()
            }

            foodContainer.addView(itemLayout)
        }
    }
}
