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
import androidx.navigation.fragment.findNavController
import com.google.android.material.slider.Slider

class ActivityLevelFrequencyFragment : Fragment() {

    private lateinit var sliderActivityLevel: Slider
    private lateinit var sliderActivityFrequency: Slider
    private lateinit var tvActivityLevel: TextView
    private lateinit var tvActivityFrequency: TextView
    private lateinit var btnNext: Button

    private lateinit var alIcon: ImageView
    private lateinit var afIcon: ImageView

    private val viewModel: WorkoutPlanViewModel by activityViewModels()

    private val activityLevels =
        arrayOf("Sedentary", "Lightly Active", "Moderately Active", "Very Active", "Super Active")
    private val activityFrequencies =
        arrayOf("Rarely", "Occasionally", "Regularly", "Frequently", "Daily")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_activity_frequency_level, container, false)

        sliderActivityLevel = view.findViewById(R.id.slider_activity_level)
        sliderActivityFrequency = view.findViewById(R.id.slider_activity_frequency)
        tvActivityLevel = view.findViewById(R.id.tv_activity_level)
        tvActivityFrequency = view.findViewById(R.id.tv_activity_frequency)
        btnNext = view.findViewById(R.id.btn_next)

        alIcon = view.findViewById(R.id.alIcon)
        afIcon = view.findViewById(R.id.afIcon)

        // Handle Activity Level selection
        sliderActivityLevel.addOnChangeListener { _, value, _ ->
            val index = value.toInt() - 1
            val selectedLevel = activityLevels[index]
            tvActivityLevel.text = selectedLevel
            updateALIcon(index)

            checkIfNextEnabled()
        }

        sliderActivityFrequency.addOnChangeListener { _, value, _ ->
            val index = value.toInt() - 1
            val selectedFrequency = activityFrequencies[index]
            tvActivityFrequency.text = selectedFrequency
            updateAFIcon(index)

            checkIfNextEnabled()
        }

        // Handle Next Button
        btnNext.setOnClickListener {
            viewModel.updateActivityLevel(tvActivityLevel.text.toString())
            viewModel.updateActivityFrequency(tvActivityFrequency.text.toString())

            (activity as? DemographicActivity)?.addFragment("TargetMuscleFragment")
            (activity as? DemographicActivity)?.nextPage()
        }

        return view
    }

    private fun checkIfNextEnabled() {
        btnNext.isEnabled = sliderActivityLevel.value > 0 && sliderActivityFrequency.value > 0
    }

    private fun updateALIcon(i: Int) {
        val alIconName =
            arrayOf("icon_al_1", "icon_al_2", "icon_al_3", "icon_al_4", "icon_al_5")

        val resId = resources.getIdentifier(alIconName[i], "drawable", requireContext().packageName)
        alIcon.setImageResource(resId)
    }

    private fun updateAFIcon(i: Int) {
        val afIconName =
            arrayOf("icon_af_1", "icon_af_2", "icon_af_3", "icon_af_4", "icon_af_5")

        val resId = resources.getIdentifier(afIconName[i], "drawable", requireContext().packageName)
        afIcon.setImageResource(resId)
    }
}
