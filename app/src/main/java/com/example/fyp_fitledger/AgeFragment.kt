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
        userViewModel.loadFromPreferences(requireContext())

        ageIcon = view.findViewById(R.id.ageIcon)
        seekBarAge = view.findViewById(R.id.seekBarAge)
        tvAge = view.findViewById(R.id.tvAge)
        btnNext = view.findViewById(R.id.btnNext)

        // Get gender from previous fragment
        selectedGender = userViewModel.gender ?: "Male"
        if (selectedGender == "Female")
            ageIcon.setImageResource(R.drawable.icon_age_f4)

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
            selectedAge in 1..3 -> if (selectedGender == "Female") "icon_age_f1" else "icon_age_m1"
            selectedAge in 4..6 -> if (selectedGender == "Female") "icon_age_f2" else "icon_age_m2"
            selectedAge in 7..12 -> if (selectedGender == "Female") "icon_age_f3" else "icon_age_m3"
            selectedAge in 13..18 -> if (selectedGender == "Female") "icon_age_f4" else "icon_age_m4"
            selectedAge in 19..30 -> if (selectedGender == "Female") "icon_age_f5" else "icon_age_m5"
            selectedAge in 31..45 -> if (selectedGender == "Female") "icon_age_f6" else "icon_age_m6"
            selectedAge in 46..65 -> if (selectedGender == "Female") "icon_age_f7" else "icon_age_m7"
            else -> if (selectedGender == "Female") "icon_age_f8" else "icon_age_m8"
        }

        val resId = resources.getIdentifier(iconName, "drawable", requireContext().packageName)
        ageIcon.setImageResource(resId)
    }

    private fun updateNextButton() {
        if (selectedAge in 1..10) {
            btnNext.isEnabled = false
        } else {
            btnNext.isEnabled = true
        }
    }
}
