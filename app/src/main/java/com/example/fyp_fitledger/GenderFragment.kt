package com.example.fyp_fitledger

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.card.MaterialCardView

class GenderFragment : Fragment() {
    private lateinit var cardMale: MaterialCardView
    private lateinit var cardFemale: MaterialCardView
    private lateinit var iconMale: ImageView
    private lateinit var iconFemale: ImageView
    private lateinit var btnNext: Button

    private lateinit var userViewModel: UserViewModel

    private var selectedGender: String? = null  // To store selected gender

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_gender, container, false)
        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)

        cardMale = view.findViewById(R.id.cardMale)
        cardFemale = view.findViewById(R.id.cardFemale)
        iconMale = view.findViewById(R.id.iconMale)
        iconFemale = view.findViewById(R.id.iconFemale)
        btnNext = view.findViewById(R.id.btnNext)

        cardMale.setOnClickListener {
            selectGender("Male")
            cardMale.strokeColor = ContextCompat.getColor(requireContext(), R.color.teal_700)
            view.findViewById<ImageView>(R.id.iconMale).setColorFilter(ContextCompat.getColor(requireContext(), R.color.teal_700))
            view.findViewById<TextView>(R.id.tvMale).setTextColor(ContextCompat.getColor(requireContext(), R.color.teal_700))

            cardFemale.strokeColor = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
            view.findViewById<ImageView>(R.id.iconFemale).setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            view.findViewById<TextView>(R.id.tvFemale).setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        }
        cardFemale.setOnClickListener {
            selectGender("Female")
            cardMale.strokeColor = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
            view.findViewById<ImageView>(R.id.iconMale).setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            view.findViewById<TextView>(R.id.tvMale).setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))

            cardFemale.strokeColor = ContextCompat.getColor(requireContext(), R.color.teal_700)
            view.findViewById<ImageView>(R.id.iconFemale).setColorFilter(ContextCompat.getColor(requireContext(), R.color.teal_700))
            view.findViewById<TextView>(R.id.tvFemale).setTextColor(ContextCompat.getColor(requireContext(), R.color.teal_700))

        }

        btnNext.setOnClickListener {
            selectedGender?.let { it1 -> userViewModel.updateGender(it1) }
            userViewModel.saveToPreferences(requireContext())
            (activity as? DemographicActivity)?.addFragment("AgeFragment")
            (activity as? DemographicActivity)?.nextPage()
        }

        return view
    }

    private fun selectGender(gender: String) {
        selectedGender = gender
        btnNext.isEnabled = true // Show Next button after selection

        if (gender == "Male") {
            cardMale.strokeColor = ContextCompat.getColor(requireContext(), R.color.cyan_cornflower_blue)
            iconMale.setColorFilter(ContextCompat.getColor(requireContext(), R.color.cyan_cornflower_blue))

            cardFemale.strokeColor = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
            iconFemale.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        } else {
            cardFemale.strokeColor = ContextCompat.getColor(requireContext(), R.color.cyan_cornflower_blue)
            iconFemale.setColorFilter(ContextCompat.getColor(requireContext(), R.color.cyan_cornflower_blue))

            cardMale.strokeColor = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
            iconMale.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        }
    }
}
