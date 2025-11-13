package com.example.fyp_fitledger.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.fyp_fitledger.ui.activity.DemographicActivity
import com.example.fyp_fitledger.R
import com.example.fyp_fitledger.data.viewmodel.UserViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    private var animationJob: Job? = null

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

            CoroutineScope(Dispatchers.Main).launch {
                delay(300)  //short delay execute function before page fully loaded
                animateMetricsDisplay(bmi, bodyFatPercentage)
            }
            Log.d("BodyMetricsFragment", "BMI: $bmi, BodyFat: $bodyFatPercentage")
        } else {
            tvBMIValue.text = "Invalid Data"
            tvBodyFatValue.text = "Invalid Data"
        }
    }

    private fun animateMetricsDisplay(bmi: Double, bodyFatPercentage: Double) {
        animationJob?.cancel()
        btnNext.isEnabled = false

        animationJob = CoroutineScope(Dispatchers.Main).launch {
            // Animate both metrics simultaneously
            val bmiJob = launch { animateDigits(tvBMIValue, bmi) }
            val bodyFatJob = launch { animateDigits(tvBodyFatValue, bodyFatPercentage) }

            // Wait until both done
            bmiJob.join()
            bodyFatJob.join()

            btnNext.isEnabled = true
        }
    }

    private suspend fun animateDigits(textView: TextView, targetValue: Double) {
        val formatted = String.format(Locale.getDefault(), "%.1f", targetValue)
        val digits = formatted.toCharArray()

        val current = CharArray(digits.size) { if (digits[it] == '.') '.' else '0' }

        for (i in digits.indices) {
            if (digits[i] == '.') continue

            val targetDigit = digits[i].digitToInt()
            for (d in 0..targetDigit) {
                current[i] = ('0'.code + d).toChar()
                textView.text = current.concatToString()
                delay(100) // small delay per digit
            }
        }
    }
}
