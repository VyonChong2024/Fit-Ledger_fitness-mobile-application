package com.example.fyp_fitledger

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class BodyMetricsFragment : Fragment() {

    private lateinit var userViewModel: UserViewModel
    private lateinit var tvBMIValue: TextView
    private lateinit var tvBodyFatValue: TextView
    private lateinit var btnNext: Button

    private var bmi: Double = 0.0
    private var bodyFatPercentage: Double = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_body_metrics, container, false)

        // Initialize ViewModel
        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)

        // Load saved user data
        userViewModel.loadFromPreferences(requireContext())

        // Initialize UI elements
        tvBMIValue = view.findViewById(R.id.tvBMIValue)
        tvBodyFatValue = view.findViewById(R.id.tvBodyFatValue)
        btnNext = view.findViewById(R.id.btnNext)

        // Calculate and display BMI & Body Fat Percentage
        calculateMetrics()

        // Next button click listener
        btnNext.setOnClickListener {
            userViewModel.updateBodyMassIndex(bmi)
            userViewModel.updateBodyFatPercent(bodyFatPercentage)
            userViewModel.saveToPreferences(requireContext())
            (activity as? DemographicActivity)?.addFragment("AccurateMeasurementFragment")
            (activity as? DemographicActivity)?.nextPage()
            //findNavController().navigate(R.id.action_to_accurateMeasurementFragment)
        }

        return view
    }

    private fun calculateMetrics() {
        val height = userViewModel.height ?: 0.0
        val weight = userViewModel.weight ?: 0.0
        val age = userViewModel.age ?: 0
        val gender = userViewModel.gender ?: "Male" // Default to Male

        Log.d("BodyMetricsFragment", "Height: $height, Weight: $weight, Age: $age, Gender: $gender")

        if (height > 0 && weight > 0) {
            bmi = weight / (height * height / 10000)
            bodyFatPercentage = if (gender.equals("Male", true)) {
                (1.20 * bmi) + (0.23 * age) - 16.2
            } else {
                (1.20 * bmi) + (0.23 * age) - 5.4
            }

            displayDataSlowly(bmi, bodyFatPercentage)
            Log.d("BodyMetricsFragment", "BMI: $bmi, BodyFat: $bodyFatPercentage")
        } else {
            tvBMIValue.text = "Invalid Data"
            tvBodyFatValue.text = "Invalid Data"
        }
    }

    private fun displayDataSlowly(bmi: Double, bodyFatPercentage: Double) = CoroutineScope(Dispatchers.Main).launch {
        CoroutineScope(Dispatchers.Main).launch { // Launch a coroutine here
            animateValue(tvBMIValue, bmi)
            animateValue(tvBodyFatValue, bodyFatPercentage)
            btnNext.isEnabled = true
        }
    }

    private suspend fun animateValue(textView: TextView, targetValue: Double) {
        val steps = (targetValue * 10).toInt()
        for (i in 0..steps) {
            textView.text = String.format(Locale.getDefault(),"%.1f", i / 10.0)
            delay(20) // Non-blocking delay
        }
    }
}
