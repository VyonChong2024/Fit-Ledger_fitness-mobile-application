package com.example.fyp_fitledger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.slider.Slider

class WorkoutTimeFragment : Fragment() {

    private lateinit var sliderWorkoutTime: Slider
    private lateinit var tvWorkoutTime: TextView
    private lateinit var btnNext: Button

    private lateinit var timeIcon: ImageView

    private val viewModel: WorkoutPlanViewModel by activityViewModels()

    private val timeLabel =
        arrayOf("30 Minutes", "45 Minutes", "1 Hour", "1 Hour 15 Minutes", "1 Hour 30 Minutes",
            "1 Hour 45 Minutes", "2 Hours", "2 Hours+")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_workout_time, container, false)

        sliderWorkoutTime = view.findViewById(R.id.slider_workout_time)
        tvWorkoutTime = view.findViewById(R.id.tv_workout_time)
        btnNext = view.findViewById(R.id.btn_next)

        timeIcon = view.findViewById(R.id.timeIcon)

        // Handle Activity Level selection
        sliderWorkoutTime.addOnChangeListener { _, value, _ ->
            val index = (value.toInt() - 30)/15
            val selectedTime = timeLabel[index]
            tvWorkoutTime.text = selectedTime
            updateTimeIcon(index)
        }

        // Handle Next Button
        btnNext.setOnClickListener {
            viewModel.updateDuration(sliderWorkoutTime.value.toInt())

            (activity as? DemographicActivity)?.addFragment("WorkoutPlanFragment")
            (activity as? DemographicActivity)?.nextPage()
        }

        return view
    }


    private fun updateTimeIcon(i: Int) {
        val timeIconName =
            arrayOf("icon_time_1", "icon_time_2", "icon_time_3", "icon_time_4")

        val resId = resources.getIdentifier(timeIconName[i/2], "drawable", requireContext().packageName)
        timeIcon.setImageResource(resId)
    }
}