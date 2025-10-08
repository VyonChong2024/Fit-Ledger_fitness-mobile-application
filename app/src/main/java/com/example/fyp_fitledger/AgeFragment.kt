package com.example.fyp_fitledger

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.card.MaterialCardView

class AgeFragment : Fragment() {

    private lateinit var ageIcon: ImageView
    private lateinit var seekBarAge: SeekBar
    private lateinit var tvAge: TextView
    private lateinit var btnNext: Button

    private lateinit var userViewModel: UserViewModel

    private var selectedAge: Int = 18
    private var selectedGender: String = "Male"  // Default gender

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_age, container, false)
        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)

        ageIcon = view.findViewById(R.id.ageIcon)
        seekBarAge = view.findViewById(R.id.seekBarAge)
        tvAge = view.findViewById(R.id.tvAge)
        btnNext = view.findViewById(R.id.btnNext)

        // Get gender from previous fragment
        arguments?.getString("gender")?.let {
            selectedGender = it
        }

        // Update UI based on seek bar movement
        seekBarAge.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedAge = progress
                tvAge.text = "Age: $selectedAge"
                updateAgeIcon()
                updateNextButton()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnNext.setOnClickListener {
            userViewModel.updateAge(selectedAge)
            userViewModel.saveToPreferences(requireContext())
            (activity as? DemographicActivity)?.addFragment("HeightWeightFragment")
            (activity as? DemographicActivity)?.nextPage()
        }

        return view
    }

    private fun updateAgeIcon() {
        val iconName = when {
            selectedAge in 1..3 -> "icon_age_1"
            selectedAge in 4..10 -> "icon_age_2"
            selectedAge in 11..17 -> if (selectedGender == "Female") "icon_age_f3" else "icon_age_m3"
            selectedAge in 18..34 -> if (selectedGender == "Female") "icon_age_f4" else "icon_age_m4"
            selectedAge in 35..59 -> if (selectedGender == "Female") "icon_age_f5" else "icon_age_m5"
            else -> if (selectedGender == "Female") "icon_age_f6" else "icon_age_m6"
        }

        val resId = resources.getIdentifier(iconName, "drawable", requireContext().packageName)
        ageIcon.setImageResource(resId)
    }

    private fun updateNextButton() {
        if (selectedAge in 1..10) {
            btnNext.isEnabled = false
            btnNext.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        } else {
            btnNext.isEnabled = true
            btnNext.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.purple_500))
        }
    }
}
