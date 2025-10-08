package com.example.fyp_fitledger

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class BodyFatResultFragment : Fragment() {

    private lateinit var userViewModel: UserViewModel
    private lateinit var tvBodyFatValue: TextView
    private lateinit var tvBodyDensityValue: TextView
    private lateinit var btnNext: Button
    private lateinit var tvBodyDensity: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_body_fat_result, container, false)

        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)
        userViewModel.loadFromPreferences(requireContext())

        tvBodyFatValue = view.findViewById(R.id.tvBodyFatValue)
        tvBodyDensityValue = view.findViewById(R.id.tvBodyDensityValue)
        btnNext = view.findViewById(R.id.btnNext)
        tvBodyDensity = view.findViewById(R.id.tvBodyDensity)

        val fatPercent = userViewModel.bodyFatPercent ?: 0.0
        val bodyDensity = userViewModel.bodyDensity ?: 0.0

        if (bodyDensity == 0.0) {
            tvBodyDensity.visibility = View.GONE
            tvBodyDensityValue.visibility = View.GONE
        }

        //tvBodyFatValue.text = String.format("%.1f", fatPercent)
        //tvBodyDensityValue.text = String.format("%.3f", bodyDensity)

        //Call displayDataSlowly directly within the onCreateView
        displayDataSlowly(fatPercent, bodyDensity)

        btnNext.setOnClickListener {
            userViewModel.updateAccuFat(true)
            (activity as? DemographicActivity)?.addFragment("CustomizeWorkoutFragment")
            (activity as? DemographicActivity)?.nextPage()
        }

        return view
    }
    private fun displayDataSlowly(fatPercent: Double, bodyDensity: Double) {
        Log.d("BodyFatResult", "Tried to execute displayDataSlowly")
        CoroutineScope(Dispatchers.Main).launch {
            launch {
                animateDigitByDigit(tvBodyFatValue, String.format(Locale.getDefault(), "%.1f", fatPercent), "00.0")
                if (bodyDensity != 0.0)
                    animateDigitByDigit(tvBodyDensityValue, String.format(Locale.getDefault(),"%.3f", bodyDensity), "0.000")
            }
        }
    }

    private suspend fun animateDigitByDigit(textView: TextView, targetValue: String, initialValue: String) {
        delay(500)  //delay for animation for entering the fragment page
        if (initialValue.length != targetValue.length) {
            textView.text = targetValue
            Log.d("BodyFatResult", "initialValue and targetValue have different lengths")
            return
        }
        val currentValue = initialValue.toMutableList()
        textView.text = currentValue.joinToString("")

        Log.d("BodyFatResult", "Tried executed for-loop")
        for (i in 0 until targetValue.length) {
            if (targetValue[i].isDigit() && currentValue[i].isDigit()) {
                while (currentValue[i].digitToInt() < targetValue[i].digitToInt()) {
                    currentValue[i] = (currentValue[i].digitToInt() + 1).digitToChar()
                    textView.text = currentValue.joinToString("")
                    Log.d("BodyFatResult", "For-loop: loop $i")
                    delay(400)
                }
            } else {
                currentValue[i] = targetValue[i]
                textView.text = currentValue.joinToString("")
            }
        }
    }
}