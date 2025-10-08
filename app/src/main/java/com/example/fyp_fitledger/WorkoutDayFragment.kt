package com.example.fyp_fitledger

import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

class WorkoutDayFragment: Fragment(){

    private lateinit var viewModel: WorkoutPlanViewModel
    private lateinit var btnNext: Button
    private lateinit var dayButton: List<ImageView>

    private val selectedDays = mutableSetOf<Int>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_workout_day, container, false)
        viewModel = ViewModelProvider(requireActivity())[WorkoutPlanViewModel::class.java]

        btnNext = view.findViewById(R.id.btn_next)

        dayButton= listOf(
            view.findViewById(R.id.icon_day_1),
            view.findViewById(R.id.icon_day_2),
            view.findViewById(R.id.icon_day_3),
            view.findViewById(R.id.icon_day_4),
            view.findViewById(R.id.icon_day_5),
            view.findViewById(R.id.icon_day_6),
            view.findViewById(R.id.icon_day_7)
        )

        for (i in dayButton.indices) {
            dayButton[i].setOnClickListener {
                // Handle click event for dayButton[i]
                val dayNumber = i + 1 // Assuming the icons correspond to day 1 to 7
                if (selectedDays.contains(dayNumber)) {
                    selectedDays.remove(dayNumber)
                    dayButton[i].setColorFilter(ContextCompat.getColor(requireContext(), R.color.light_grey), PorterDuff.Mode.SRC_ATOP) // Remove color filter
                    if (selectedDays.isEmpty()) {
                        btnNext.isEnabled = false
                    }
                    (it as ImageView).setColorFilter(ContextCompat.getColor(requireContext(), R.color.light_grey), PorterDuff.Mode.SRC_ATOP) // Remove color filter
                } else {
                    selectedDays.add(dayNumber)
                    (it as ImageView).setColorFilter(ContextCompat.getColor(requireContext(), R.color.grayish_lime_green), PorterDuff.Mode.SRC_ATOP)
                }
                checkButtonEnable()
            }
        }

        btnNext.setOnClickListener {
            val sortedNumbers: List<Int> = selectedDays.sorted()
            viewModel.updateDaySelected(convertDayNumbersToStrings(sortedNumbers))      //selectedDays
            (activity as? DemographicActivity)?.addFragment("WorkoutTimeFragment")
            (activity as? DemographicActivity)?.nextPage()
        }

        return view
    }

    fun checkButtonEnable() {
        btnNext.isEnabled = selectedDays.isNotEmpty()
    }

    fun convertDayNumbersToStrings(dayNumbers: List<Int>): List<String> {  //MutableSet<Int>
        val dayStrings = mutableListOf<String>()
        for (dayNumber in dayNumbers) {
            val dayString = when (dayNumber) {
                1 -> "Monday"
                2 -> "Tuesday"
                3 -> "Wednesday"
                4 -> "Thursday"
                5 -> "Friday"
                6 -> "Saturday"
                7 -> "Sunday"
                else -> "Invalid Day" // Handle invalid day numbers
            }
            dayStrings.add(dayString)
        }
        return dayStrings
    }
}